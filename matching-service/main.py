from __future__ import annotations

import os
from typing import Any, Dict, List, Optional

import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel, Field

try:
    from sentence_transformers import SentenceTransformer
except Exception:
    SentenceTransformer = None  # type: ignore


DEFAULT_WEIGHTS = {
    "skill": 0.40,
    "time": 0.25,
    "goal": 0.15,
    "credit": 0.10,
    "social": 0.10,
}


class MatchRequest(BaseModel):
    project_id: int
    project: Dict[str, Any]
    candidates: List[Dict[str, Any]]


class MatchResult(BaseModel):
    user_id: int
    score: float
    breakdown: Dict[str, float] = Field(default_factory=dict)


def _safe_float(x: Any, default: float = 0.0) -> float:
    try:
        return float(x)
    except Exception:
        return default


def _normalize01(x: float) -> float:
    if np.isnan(x) or np.isinf(x):
        return 0.0
    return float(min(1.0, max(0.0, x)))


def _extract_project_text(project: Dict[str, Any]) -> str:
    parts = [
        str(project.get("title") or ""),
        str(project.get("description") or ""),
        str(project.get("project_type") or ""),
    ]
    return " ".join([p for p in parts if p]).strip()


def _extract_user_text(candidate: Dict[str, Any]) -> str:
    user = candidate.get("user") or {}
    parts = [
        str(user.get("bio") or ""),
        str(user.get("project_experience") or ""),
        str(user.get("department") or ""),
    ]
    interests = user.get("interests")
    if isinstance(interests, list):
        parts.extend([str(i) for i in interests if i])
    return " ".join([p for p in parts if p]).strip()


def _skills_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    reqs = project.get("skill_requirements")
    if not isinstance(reqs, list):
        reqs = []

    cand_skills = candidate.get("skills")
    if not isinstance(cand_skills, list):
        cand_skills = []

    cand_map = {}
    for s in cand_skills:
        if not isinstance(s, dict):
            continue
        name = (s.get("skill_name") or "").strip().lower()
        if not name:
            continue
        cand_map[name] = _safe_float(s.get("proficiency_level"), 0.0)

    # If no requirements provided, treat as neutral rather than 0.
    if len(reqs) == 0:
        # Use a weak signal: having any skills slightly helps.
        return 0.5 if len(cand_map) > 0 else 0.3

    required_names = []
    bonus_names = []
    for r in reqs:
        if not isinstance(r, dict):
            continue
        name = (r.get("skill_name") or r.get("name") or "").strip().lower()
        if not name:
            continue
        if r.get("required") is True:
            required_names.append(name)
        else:
            bonus_names.append(name)

    # required must match as much as possible
    req_hit = 0
    req_level_penalty = 0.0
    for name in required_names:
        if name in cand_map:
            req_hit += 1
        else:
            req_level_penalty += 1.0

    req_score = 1.0
    if len(required_names) > 0:
        req_score = req_hit / float(len(required_names))

    bonus_hit = 0
    for name in bonus_names:
        if name in cand_map:
            bonus_hit += 1
    bonus_score = 0.0
    if len(bonus_names) > 0:
        bonus_score = bonus_hit / float(len(bonus_names))

    # combine: required dominates, bonus improves
    score = 0.8 * req_score + 0.2 * bonus_score
    # penalty for missing required skills
    if len(required_names) > 0:
        score *= (1.0 - 0.15 * (req_level_penalty / float(len(required_names))))

    return _normalize01(score)


def _time_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    # Current backend doesn't send structured availability; use weekly_hours as a proxy.
    proj_hours = _safe_float(project.get("weekly_hours"), 0.0)
    if proj_hours <= 0:
        return 0.5

    # candidate availability may be a list of time slots; if absent, assume medium.
    availability = candidate.get("availability")
    if not isinstance(availability, list) or len(availability) == 0:
        return 0.5

    # If there are explicit time slots, approximate overlap by count.
    overlap = min(len(availability), max(1.0, proj_hours / 5.0))
    denom = max(len(availability), proj_hours / 5.0)
    return _normalize01(_safe_float(overlap / denom, 0.5))


class _Embedder:
    def __init__(self) -> None:
        self.enabled = bool(os.getenv("SEMANTIC_ENABLED", "1").strip() != "0")
        self.model_name = os.getenv(
            "SEMANTIC_MODEL",
            "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
        )
        self.device = os.getenv("SEMANTIC_DEVICE")
        self._model: Optional[Any] = None

    def _load(self) -> None:
        if self._model is not None:
            return
        if not self.enabled:
            return
        if SentenceTransformer is None:
            self.enabled = False
            return
        kwargs: Dict[str, Any] = {}
        if self.device:
            kwargs["device"] = self.device
        self._model = SentenceTransformer(self.model_name, **kwargs)

    def cosine(self, a: str, b: str) -> float:
        if not a or not b:
            return 0.3
        self._load()
        if not self.enabled or self._model is None:
            # fallback: simple token overlap
            sa = set(a.lower().split())
            sb = set(b.lower().split())
            if not sa or not sb:
                return 0.3
            return _normalize01(len(sa & sb) / float(len(sa | sb)))
        va = self._model.encode([a], normalize_embeddings=True)[0]
        vb = self._model.encode([b], normalize_embeddings=True)[0]
        return _normalize01(float(np.dot(va, vb)))


_embedder = _Embedder()


def _goal_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    ptxt = _extract_project_text(project)
    utxt = _extract_user_text(candidate)
    return _normalize01(_embedder.cosine(ptxt, utxt))


def _credit_score(candidate: Dict[str, Any]) -> float:
    credit = candidate.get("credit") or {}
    if not isinstance(credit, dict):
        credit = {}

    level = str(credit.get("credit_level") or "").strip().upper()
    total = _safe_float(credit.get("total_credit"), 0.0)

    level_map = {
        "S": 1.0,
        "A": 0.85,
        "B": 0.7,
        "C": 0.55,
        "D": 0.4,
    }
    level_score = level_map.get(level, 0.6)

    # total credit soft cap at 100
    total_score = _normalize01(total / 100.0)

    return _normalize01(0.7 * level_score + 0.3 * total_score)


def _social_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    # Very limited data currently; use department match / collaboration history count if any.
    p_creator = project.get("creator_id")
    user = candidate.get("user") or {}

    dept = (user.get("department") or "").strip().lower()
    # No project dept in current data; fallback to small boost if any dept present.
    dept_score = 0.5 if dept else 0.3

    history = candidate.get("collaboration_history")
    if not isinstance(history, list):
        history = []
    hist_score = _normalize01(min(1.0, len(history) / 5.0))

    # Avoid recommending creator themselves; backend already filters.
    if user.get("id") == p_creator:
        return 0.0

    return _normalize01(0.7 * dept_score + 0.3 * hist_score)


def _compute_breakdown(project: Dict[str, Any], candidate: Dict[str, Any]) -> Dict[str, float]:
    return {
        "skill": _skills_score(project, candidate),
        "time": _time_score(project, candidate),
        "goal": _goal_score(project, candidate),
        "credit": _credit_score(candidate),
        "social": _social_score(project, candidate),
    }


def _final_score(breakdown: Dict[str, float], weights: Dict[str, float]) -> float:
    score = 0.0
    for k, w in weights.items():
        score += _safe_float(breakdown.get(k), 0.0) * _safe_float(w, 0.0)
    return float(score)


app = FastAPI(title="Matching Service", version="1.0.0")


@app.get("/health")
def health() -> Dict[str, Any]:
    return {
        "status": "ok",
        "semantic_enabled": _embedder.enabled,
        "semantic_model": _embedder.model_name,
    }


@app.post("/api/matching/calculate", response_model=List[MatchResult])
def calculate(req: MatchRequest) -> List[MatchResult]:
    project = req.project or {}

    weights = {
        "skill": _safe_float(os.getenv("WEIGHT_SKILL"), DEFAULT_WEIGHTS["skill"]),
        "time": _safe_float(os.getenv("WEIGHT_TIME"), DEFAULT_WEIGHTS["time"]),
        "goal": _safe_float(os.getenv("WEIGHT_GOAL"), DEFAULT_WEIGHTS["goal"]),
        "credit": _safe_float(os.getenv("WEIGHT_CREDIT"), DEFAULT_WEIGHTS["credit"]),
        "social": _safe_float(os.getenv("WEIGHT_SOCIAL"), DEFAULT_WEIGHTS["social"]),
    }

    results: List[MatchResult] = []
    for c in req.candidates:
        user = c.get("user") or {}
        uid = user.get("id")
        if uid is None:
            continue
        breakdown = _compute_breakdown(project, c)
        score = _final_score(breakdown, weights)
        results.append(
            MatchResult(
                user_id=int(uid),
                score=float(score),
                breakdown={k: float(v) for k, v in breakdown.items()},
            )
        )

    results.sort(key=lambda r: r.score, reverse=True)
    return results
