from __future__ import annotations

import hashlib
import json
import logging
import os
import re
import time
from collections import defaultdict, deque
from pathlib import Path
from threading import Lock
from typing import Any, Dict, List, Optional, Tuple

import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel, Field

try:
    from sentence_transformers import SentenceTransformer
except Exception:
    SentenceTransformer = None  # type: ignore


logger = logging.getLogger("matching-service")
LOG_DEBUG_ENABLED = os.getenv("MATCHING_DEBUG", "0").strip() == "1"
LOG_INFO_ENABLED = os.getenv("MATCHING_INFO", "1").strip() != "0"


def _log_debug(message: str) -> None:
    if LOG_DEBUG_ENABLED:
        logger.debug(message)


def _log_info(message: str) -> None:
    if LOG_INFO_ENABLED:
        logger.info(message)


def _log_error(message: str) -> None:
    logger.error(message)


_METRIC_LOCK = Lock()
_METRIC_WINDOW = max(50, int(os.getenv("MATCHING_METRICS_WINDOW", "500")))
_REQUEST_TIMES_MS: deque = deque(maxlen=_METRIC_WINDOW)
_RECENT_RESULTS: deque = deque(maxlen=_METRIC_WINDOW)
_FEEDBACK_EVENTS: defaultdict = defaultdict(int)
_TOTAL_REQUESTS = 0
_TOTAL_FEEDBACK = 0


def _record_request_metrics(duration_ms: float, recall_count: int) -> None:
    global _TOTAL_REQUESTS
    with _METRIC_LOCK:
        _TOTAL_REQUESTS += 1
        _REQUEST_TIMES_MS.append(float(duration_ms))
        _RECENT_RESULTS.append(int(recall_count))


def _record_feedback_metrics(event: str) -> None:
    global _TOTAL_FEEDBACK
    with _METRIC_LOCK:
        _TOTAL_FEEDBACK += 1
        _FEEDBACK_EVENTS[(event or "UNKNOWN").strip().upper()] += 1


def _p95(values: List[float]) -> float:
    if not values:
        return 0.0
    return float(np.percentile(np.array(values, dtype=float), 95))


DEFAULT_WEIGHTS = {
    "skill": 0.40,           # 技能匹配（含认证加成）- 40% ↑ 核心维度
    "collaboration": 0.15,   # 协作历史（历史评分+成功率）- 15% ↓ 数据不足时降低
    "time": 0.15,            # 时间匹配 - 15% ↑ 硬性约束，提升重要性
    "goal": 0.10,            # 目标契合（含历史项目类型）- 10% ↑ 重要性高
    "experience": 0.08,      # 项目经验 - 8%
    "mentor_rating": 0.05,   # 导师评分 - 5% ⭐ 新增第9维度
    "academic": 0.04,        # 学术背景 - 4%
    "credit": 0.02,          # 信誉评分 - 2%
    "application": 0.01,     # 申请历史 - 1%
}

AB_BUCKETS = max(1, int(os.getenv("MATCHING_AB_BUCKETS", "100")))
AB_EXPERIMENT = os.getenv("MATCHING_AB_EXPERIMENT", "off").strip().lower()  # off/weights_v2
AB_TRAFFIC = max(0.0, min(1.0, float(os.getenv("MATCHING_AB_TRAFFIC", "0") or 0.0)))
STRATEGY_FILE = Path(os.getenv("MATCHING_STRATEGY_FILE", "matching-service/matching_strategy.json"))
STRATEGY_BACKUP_DIR = Path(os.getenv("MATCHING_STRATEGY_BACKUP_DIR", "matching-service/strategy_backups"))
OFFLINE_EVAL_FILE = Path(os.getenv("MATCHING_OFFLINE_EVAL_FILE", "matching-service/offline_eval.jsonl"))
LTR_MODEL_FILE = Path(os.getenv("MATCHING_LTR_MODEL_FILE", "matching-service/ltr_model.json"))
LTR_MODEL_BACKUP_DIR = Path(os.getenv("MATCHING_LTR_MODEL_BACKUP_DIR", "matching-service/ltr_model_backups"))
AB_CONFIG_LOCK = Lock()

SEMANTIC_LOAD_FAIL = 0
SEMANTIC_CALL_COUNT = 0
SEMANTIC_HIT_COUNT = 0
SEMANTIC_TOTAL_MS = 0.0

AB_SEGMENT_ALLOW_ALL = "*"
AB_SEGMENT_NOT_SET = "__not_set__"
AB_ALLOWED_PROJECT_TYPES = {
    p.strip().lower()
    for p in os.getenv("MATCHING_AB_PROJECT_TYPES", "*").split(",")
    if p.strip()
}
AB_ALLOWED_USER_SEGMENTS = {
    p.strip().lower()
    for p in os.getenv("MATCHING_AB_USER_SEGMENTS", "*").split(",")
    if p.strip()
}

AB_RUNTIME: Dict[str, Any] = {
    "experiment": AB_EXPERIMENT,
    "traffic": AB_TRAFFIC,
    "buckets": AB_BUCKETS,
    "project_types": AB_ALLOWED_PROJECT_TYPES if AB_ALLOWED_PROJECT_TYPES else {AB_SEGMENT_ALLOW_ALL},
    "user_segments": AB_ALLOWED_USER_SEGMENTS if AB_ALLOWED_USER_SEGMENTS else {AB_SEGMENT_ALLOW_ALL},
}


def _normalize_text_key(value: Any) -> str:
    text = str(value or "").strip().lower()
    return text if text else AB_SEGMENT_NOT_SET


def _extract_user_segment(user: Optional[Dict[str, Any]]) -> str:
    if not isinstance(user, dict):
        return AB_SEGMENT_NOT_SET

    for key in ("ab_segment", "segment", "user_segment", "grade", "major", "department"):
        if key in user and str(user.get(key) or "").strip():
            return _normalize_text_key(user.get(key))

    return AB_SEGMENT_NOT_SET


class MatchRequest(BaseModel):
    project_id: int
    project: Dict[str, Any]
    candidates: List[Dict[str, Any]]


class MatchFeedbackRequest(BaseModel):
    project_id: int
    user_id: int
    project_type: Optional[str] = None
    event: str  # INVITED / INVITE_ACCEPTED / INVITE_DECLINED / APPLICATION_APPROVED / APPLICATION_REJECTED
    source: Optional[str] = None
    event_time: Optional[int] = None
    breakdown: Dict[str, float] = Field(default_factory=dict)


class MatchWeightsResponse(BaseModel):
    weights: Dict[str, float]
    feedback_count: int


class MatchResult(BaseModel):
    user_id: int
    score: float
    breakdown: Dict[str, float] = Field(default_factory=dict)
    time_explanation: str = ""
    # 置信度与风险（用于解释“为什么这个分数可信/不可信”）
    confidence: float = 0.0           # 0-1
    confidenceLevel: str = "LOW"      # HIGH/MEDIUM/LOW（camelCase 方便 Java DTO 直接反序列化）
    riskLevel: str = "MEDIUM"         # LOW/MEDIUM/HIGH
    # 可解释性/置信度（后端 Java DTO 已有对应字段，前端可按需展示）
    confidence: Optional[float] = None      # 0-1
    confidenceLevel: Optional[str] = None   # HIGH/MEDIUM/LOW
    riskLevel: Optional[str] = None         # LOW/MEDIUM/HIGH


class StrategyUpdateRequest(BaseModel):
    weights_control: Dict[str, float]
    weights_treatment: Optional[Dict[str, float]] = None
    note: Optional[str] = None


class StrategyRollbackRequest(BaseModel):
    version: int
    note: Optional[str] = None


class LtrTrainRequest(BaseModel):
    min_samples: Optional[int] = None
    note: Optional[str] = None


class LtrRollbackRequest(BaseModel):
    version: int
    note: Optional[str] = None


class LtrConfigRequest(BaseModel):
    enabled: Optional[bool] = None
    alpha: Optional[float] = None


def _safe_float(x: Any, default: float = 0.0) -> float:
    try:
        return float(x)
    except Exception:
        return default


DATA_MISSING_POLICY = os.getenv("MATCHING_MISSING_POLICY", "unknown_bias").strip().lower()


def _missing_score(has_data: bool, medium_default: float = 0.5, unknown_default: float = 0.35) -> float:
    if has_data:
        return medium_default
    # 区分“未知”与“中等”：未知默认略低，避免全面0.5中庸化
    if DATA_MISSING_POLICY == "medium":
        return medium_default
    return unknown_default


def _has_project_time_slots(project: Dict[str, Any]) -> bool:
    slots = project.get("availability") or project.get("time_slots") or project.get("timeSlots") or []
    return isinstance(slots, list) and any(isinstance(s, dict) for s in slots)


def _required_skill_missing(project: Dict[str, Any], candidate: Dict[str, Any]) -> bool:
    """
    硬过滤判定：若项目存在 required=true 的技能需求，而候选人缺失任一必需技能 -> True
    """
    reqs = project.get("skill_requirements") or project.get("skillRequirements") or []
    if not isinstance(reqs, list):
        return False

    required_names: List[str] = []
    for r in reqs:
        if not isinstance(r, dict):
            continue
        if bool(r.get("required") is True):
            name = _normalize_skill_name(
                r.get("skill_name")
                or r.get("skillName")
                or r.get("tagName")
                or r.get("name")
            )
            if name:
                required_names.append(name)

    if not required_names:
        return False

    cand_skills = candidate.get("skills") or []
    if not isinstance(cand_skills, list):
        return True

    cand_set = set()
    for s in cand_skills:
        if not isinstance(s, dict):
            continue
        n = _normalize_skill_name(s.get("skill_name"))
        if n:
            cand_set.add(n)

    return any(rn not in cand_set for rn in required_names)


def _compute_confidence_and_risk(project: Dict[str, Any], candidate: Dict[str, Any], breakdown: Dict[str, float]) -> Tuple[float, str, str]:
    """
    confidence：信息完备性（结构化数据越多越高）
    riskLevel：偏"不可用/不可靠"的提示（缺必需技能/时间完全不匹配/信誉差）
    优化：增加评价数据质量判断
    """
    user = candidate.get("user") or {}

    signals = 0
    total = 7  # 增加评价数据信号

    if isinstance(candidate.get("skills"), list) and len(candidate.get("skills") or []) > 0:
        signals += 1
    if isinstance(candidate.get("availability"), list) and len(candidate.get("availability") or []) > 0:
        signals += 1
    if isinstance(candidate.get("collaboration_history"), list) and len(candidate.get("collaboration_history") or []) > 0:
        signals += 1
    # 评价数据质量判断：不仅检查是否存在，还检查数据质量
    evaluations = user.get("evaluations") or {}
    if isinstance(evaluations, dict):
        has_eval_data = evaluations.get("has_evaluation_data", False)
        overall_avg = _safe_float(evaluations.get("overall_avg_score"), 0.0)
        if has_eval_data and overall_avg > 0.1:
            signals += 1
        elif any(_safe_float(evaluations.get(k), 0) > 0 for k in ("avg_tech_contribution","avg_collaboration","avg_task_completion")):
            signals += 0.5  # 数据有限，给半分
    if isinstance(candidate.get("experience_score"), dict) or str(user.get("project_experience") or "").strip():
        signals += 1
    if isinstance(candidate.get("credit"), dict) and (candidate.get("credit") or {}).get("credit_level"):
        signals += 1
    # 新增：检查是否有导师评分
    if _safe_float(user.get("mentor_rating"), 0.0) > 0:
        signals += 1

    confidence = _normalize01(signals / float(total))
    if confidence >= 0.67:
        level = "HIGH"
    elif confidence >= 0.34:
        level = "MEDIUM"
    else:
        level = "LOW"

    risk = "MEDIUM"
    if _required_skill_missing(project, candidate):
        risk = "HIGH"
    if _has_project_time_slots(project) and breakdown.get("time", 0.0) <= 0.05:
        risk = "HIGH"
    if breakdown.get("credit", 1.0) < 0.45:
        risk = "HIGH"
    # 新增：如果评价数据不足且协作历史也少，增加风险提示
    if breakdown.get("collaboration", 0.0) < 0.3:
        # 检查是否是因为数据不足导致的低分
        if not isinstance(evaluations, dict) or not evaluations.get("has_evaluation_data", False):
            if not isinstance(candidate.get("collaboration_history"), list) or len(candidate.get("collaboration_history") or []) == 0:
                risk = "MEDIUM"  # 新用户风险中等，不是高风险

    return confidence, level, risk


def _normalize01(x: float) -> float:
    if np.isnan(x) or np.isinf(x):
        return 0.0
    return float(min(1.0, max(0.0, x)))


def _extract_project_text(project: Dict[str, Any]) -> str:
    parts = [
        str(project.get("title") or ""),
        str(project.get("description") or ""),
        str(project.get("project_type") or project.get("type") or ""),
    ]
    return " ".join([p for p in parts if p]).strip()


def _project_text_signal(text: str) -> float:
    """
    评估项目文本的“有效信息量”，用于动态禁用 goal 维度，避免数字/噪声导致虚假差异。
    返回 0-1，越高表示越可信。
    """
    t = (text or "").strip()
    if not t:
        return 0.0

    total = len(t)
    digits = sum(1 for ch in t if ch.isdigit())
    letters = sum(1 for ch in t if ch.isalpha())
    # 兼容中文：Unicode CJK Unified Ideographs
    cjk = sum(1 for ch in t if "\u4e00" <= ch <= "\u9fff")
    informative = letters + cjk

    if total <= 0:
        return 0.0

    digit_ratio = digits / float(total)
    info_ratio = informative / float(total)

    # 最低门槛：至少有一定数量的字母/汉字，否则基本就是数字/符号
    if informative < 4:
        return _normalize01(0.2 * info_ratio)

    # 越接近“纯数字”，信号越弱
    score = (0.65 * info_ratio) + (0.35 * (1.0 - digit_ratio))
    return _normalize01(score)


def _normalize_weights(weights: Dict[str, float]) -> Dict[str, float]:
    cleaned = {k: max(0.0, _safe_float(v, 0.0)) for k, v in (weights or {}).items() if k in DEFAULT_WEIGHTS}
    total = sum(cleaned.values())
    if total <= 0:
        return DEFAULT_WEIGHTS.copy()
    return {k: (v / total) for k, v in cleaned.items()}


def _effective_weights_for_request(project: Dict[str, Any], base_weights: Dict[str, float]) -> Dict[str, float]:
    """
    依据“本次请求提供的信息”动态调整权重，避免某维度缺少有效输入时仍参与导致虚假差异。
    - 项目文本信号低：禁用 goal
    - 技能需求很少：提升 skill 主导性
    - 有结构化时间段：提升 time 主导性；没有则降低 time
    """
    w = dict(base_weights or {})

    reqs = project.get("skill_requirements") or project.get("skillRequirements") or []
    req_count = len(reqs) if isinstance(reqs, list) else 0

    time_slots = project.get("time_slots") or project.get("timeSlots") or project.get("availability") or []
    has_time_slots = isinstance(time_slots, list) and len(time_slots) > 0

    text = _extract_project_text(project)
    text_signal = _project_text_signal(text)

    # 1) 低信号文本：禁用 goal
    if text_signal < 0.35:
        w["goal"] = 0.0

    # 2) skill 主导性：只有 1 个技能时更应靠 skill 拉开差距
    if req_count == 1:
        w["skill"] = max(_safe_float(w.get("skill"), 0.0), 0.55)
    elif req_count == 0:
        # 没有技能要求时，skill 不应过强（避免“候选人会的多就高分”）
        w["skill"] = min(_safe_float(w.get("skill"), 0.0), 0.25)

    # 3) time 主导性：有结构化时段时提高，否则降低（减少未知默认分影响）
    if has_time_slots:
        w["time"] = max(_safe_float(w.get("time"), 0.0), 0.20)
    else:
        w["time"] = min(_safe_float(w.get("time"), 0.0), 0.08)

    return _normalize_weights(w)


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


SKILL_ALIASES: Dict[str, str] = {
    "js": "javascript",
    "node": "nodejs",
    "node.js": "nodejs",
    "ts": "typescript",
    "vue": "vuejs",
    "react.js": "react",
    "py": "python",
    "golang": "go",
    "c sharp": "c#",
}


def _normalize_skill_name(name: Any) -> str:
    raw = str(name or "").strip().lower()
    if not raw:
        return ""
    return SKILL_ALIASES.get(raw, raw)


def _normalize_proficiency(value: Any) -> float:
    if isinstance(value, (int, float)):
        v = float(value)
        if v > 1.0:
            v = v / 5.0  # 兼容 1-5
        return _normalize01(v)

    text = str(value or "").strip().upper()
    if text in {"EXPERT", "ADVANCED"}:
        return 1.0
    if text in {"INTERMEDIATE", "MID"}:
        return 0.7
    if text in {"BEGINNER", "JUNIOR"}:
        return 0.4
    return _normalize01(_safe_float(value, 0.5))


def _skills_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    # 兼容 snake_case / camelCase（避免后端字段映射问题导致“虚假匹配度”）
    reqs = project.get("skill_requirements") or project.get("skillRequirements")
    if not isinstance(reqs, list):
        reqs = []

    cand_skills = candidate.get("skills")
    if not isinstance(cand_skills, list):
        cand_skills = []

    _log_debug(f"技能匹配 - 项目要求: {len(reqs)}个技能, 候选人技能: {len(cand_skills)}个")

    # 候选人技能映射：skill -> best proficiency
    cand_map: Dict[str, float] = {}
    cert_map: Dict[str, str] = {}
    for s in cand_skills:
        if not isinstance(s, dict):
            continue
        name = _normalize_skill_name(s.get("skill_name"))
        if not name:
            continue

        prof = _normalize_proficiency(s.get("proficiency_level"))
        if name not in cand_map or prof > cand_map[name]:
            cand_map[name] = prof

        cert_type = str(s.get("certification_type") or "SELF_CLAIM").strip().upper()
        cert_map[name] = cert_type

    _log_debug(f"候选人技能映射: {list(cand_map.keys())}")

    if len(reqs) == 0:
        score = 0.5 if len(cand_map) > 0 else 0.3
        _log_debug(f"无技能要求，返回默认分数: {score}")
        return score

    required_items: List[Dict[str, Any]] = []
    optional_items: List[Dict[str, Any]] = []
    for r in reqs:
        if not isinstance(r, dict):
            continue
        name = _normalize_skill_name(
            r.get("skill_name")
            or r.get("skillName")
            or r.get("tagName")
            or r.get("name")
        )
        if not name:
            continue
        item = {
            "name": name,
            "required": bool(r.get("required") is True),
            "required_level": _normalize_proficiency(r.get("proficiency_level") or r.get("proficiencyLevel")),
        }
        if item["required"]:
            required_items.append(item)
        else:
            optional_items.append(item)

    def score_item(item: Dict[str, Any], importance: float) -> float:
        name = item["name"]
        req_level = _safe_float(item.get("required_level"), 0.5)
        cand_level = _safe_float(cand_map.get(name), 0.0)

        if name not in cand_map:
            # 必需技能缺失惩罚更强
            return 0.0

        # 熟练度差值惩罚（上限控制：最多扣 0.35）
        gap = max(0.0, req_level - cand_level)
        gap_penalty = min(0.35, gap * 0.7)

        # 认证加成：按认证类型 + 技能关键度加权（提升加成幅度）
        cert_type = cert_map.get(name, "SELF_CLAIM")
        if importance >= 0.9:  # 必需技能
            cert_base = {
                "OFFICIAL": 0.20,        # 官方认证 +20%
                "PEER_VERIFIED": 0.12,   # 同行认证 +12%
                "SELF_CLAIM": 0.05,      # 自我声明 +5%
            }.get(cert_type, 0.02)
        else:  # 可选技能
            cert_base = {
                "OFFICIAL": 0.12,        # 官方认证 +12%
                "PEER_VERIFIED": 0.07,   # 同行认证 +7%
                "SELF_CLAIM": 0.03,      # 自我声明 +3%
            }.get(cert_type, 0.02)
        cert_boost = cert_base  # 直接使用，不再乘以importance

        base = 1.0 - gap_penalty
        return _normalize01(base + cert_boost)

    required_score = 0.0
    if required_items:
        required_score = sum(score_item(it, 1.0) for it in required_items) / float(len(required_items))

    optional_score = 0.0
    if optional_items:
        optional_score = sum(score_item(it, 0.6) for it in optional_items) / float(len(optional_items))

    # 必需技能优先
    if required_items and optional_items:
        score = 0.82 * required_score + 0.18 * optional_score
    elif required_items:
        score = required_score
    else:
        score = 0.55 + 0.45 * optional_score

    final_score = _normalize01(score)
    _log_debug(f"技能匹配最终得分: {final_score:.3f} (required={required_score:.3f}, optional={optional_score:.3f})")
    return final_score


def _parse_time_to_minutes(t: Any) -> Optional[int]:
    if t is None:
        return None
    s = str(t).strip()
    if not s:
        return None
    parts = s.split(":")
    if len(parts) < 2:
        return None
    try:
        h = int(parts[0])
        m = int(parts[1])
    except Exception:
        return None
    if h < 0 or h > 23 or m < 0 or m > 59:
        return None
    return h * 60 + m


def _normalize_day(day: Any) -> Optional[int]:
    try:
        d = int(day)
    except Exception:
        return None
    # 兼容 0-6 与 1-7
    if 0 <= d <= 6:
        return d
    if 1 <= d <= 7:
        return d % 7
    return None


def _expand_availability_segments(slots: Any) -> List[Dict[str, int]]:
    segments: List[Dict[str, int]] = []
    if not isinstance(slots, list):
        return segments

    for slot in slots:
        if not isinstance(slot, dict):
            continue
        # 兼容 snake_case / camelCase（dayOfWeek/startTime/endTime）
        day = _normalize_day(slot.get("day_of_week") if slot.get("day_of_week") is not None else slot.get("dayOfWeek"))
        start = _parse_time_to_minutes(slot.get("start_time") if slot.get("start_time") is not None else slot.get("startTime"))
        end = _parse_time_to_minutes(slot.get("end_time") if slot.get("end_time") is not None else slot.get("endTime"))
        if day is None or start is None or end is None:
            continue

        # 跨天处理（如23:00-01:00）
        if end <= start:
            segments.append({"day": day, "start": start, "end": 1440})
            segments.append({"day": (day + 1) % 7, "start": 0, "end": end})
        else:
            segments.append({"day": day, "start": start, "end": end})
    return segments


def _time_overlap_minutes(a: Dict[str, int], b: Dict[str, int]) -> int:
    if a["day"] != b["day"]:
        return 0
    s = max(a["start"], b["start"])
    e = min(a["end"], b["end"])
    return max(0, e - s)


def _slot_label(seg: Dict[str, int]) -> str:
    day_names = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"]
    day = day_names[seg["day"]]
    start = seg["start"]
    if 5 * 60 <= start < 12 * 60:
        part = "上午"
    elif 12 * 60 <= start < 18 * 60:
        part = "下午"
    else:
        part = "晚上"
    return f"{day}{part}"


def _time_overlap_analysis(project: Dict[str, Any], candidate: Dict[str, Any]) -> Dict[str, Any]:
    project_slots = project.get("availability") or project.get("time_slots") or project.get("timeSlots") or []
    candidate_slots = candidate.get("availability") or []

    p_segments = _expand_availability_segments(project_slots)
    c_segments = _expand_availability_segments(candidate_slots)

    # 没有结构化时间时的回退逻辑
    if not p_segments:
        proj_hours = _safe_float(project.get("weekly_hours") or project.get("weeklyHours"), 0.0)
        if proj_hours <= 0:
            return {"score": _missing_score(False), "explanation": "项目未提供可用时段，时间匹配按未知数据策略处理"}
        if not c_segments:
            return {"score": _missing_score(False), "explanation": "候选人未完善可用时段，时间匹配按未知数据策略处理"}
        expected = max(60.0, proj_hours * 60.0)
        available = float(sum(max(0, s["end"] - s["start"]) for s in c_segments))
        score = _normalize01(min(1.0, available / expected))
        return {"score": score, "explanation": "项目缺少时段明细，基于周投入时长与候选人可用时长估算"}

    if not c_segments:
        return {"score": 0.3, "explanation": "候选人未提供可用时段，时间重叠不足"}

    total_project_minutes = sum(max(0, s["end"] - s["start"]) for s in p_segments)
    if total_project_minutes <= 0:
        return {"score": _missing_score(False), "explanation": "项目可用时段无效，时间匹配按未知数据策略处理"}

    overlap_total = 0
    matched: List[Dict[str, Any]] = []
    for p in p_segments:
        best = 0
        for c in c_segments:
            o = _time_overlap_minutes(p, c)
            if o > best:
                best = o
        if best > 0:
            matched.append({"segment": p, "minutes": best})
            overlap_total += best

    score = _normalize01(overlap_total / float(total_project_minutes))

    matched.sort(key=lambda x: x["minutes"], reverse=True)
    labels = []
    for item in matched:
        label = _slot_label(item["segment"])
        if label not in labels:
            labels.append(label)
        if len(labels) >= 2:
            break

    if labels:
        explanation = f"时间重叠集中在{'、'.join(labels)}，重叠率约{int(round(score * 100))}%"
    else:
        explanation = f"时间重叠较少，重叠率约{int(round(score * 100))}%"

    return {"score": score, "explanation": explanation}


def _time_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    return _safe_float(_time_overlap_analysis(project, candidate).get("score"), 0.5)


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
        global SEMANTIC_LOAD_FAIL
        if self._model is not None:
            return
        if not self.enabled:
            return
        if SentenceTransformer is None:
            self.enabled = False
            SEMANTIC_LOAD_FAIL += 1
            return
        kwargs: Dict[str, Any] = {}
        if self.device:
            kwargs["device"] = self.device
        try:
            self._model = SentenceTransformer(self.model_name, **kwargs)
        except Exception:
            self.enabled = False
            SEMANTIC_LOAD_FAIL += 1

    def _fallback_overlap(self, a: str, b: str) -> float:
        tokens_a = [t.strip() for t in a.lower().replace("/", " ").replace("-", " ").split() if t.strip()]
        tokens_b = [t.strip() for t in b.lower().replace("/", " ").replace("-", " ").split() if t.strip()]
        sa, sb = set(tokens_a), set(tokens_b)
        if not sa or not sb:
            return 0.3
        jaccard = len(sa & sb) / float(len(sa | sb))
        contain = 1.0 if any(x in b.lower() for x in sa) or any(x in a.lower() for x in sb) else 0.0
        return _normalize01(0.75 * jaccard + 0.25 * contain)

    def cosine(self, a: str, b: str) -> float:
        global SEMANTIC_CALL_COUNT, SEMANTIC_HIT_COUNT, SEMANTIC_TOTAL_MS
        if not a or not b:
            return 0.3
        started = time.perf_counter()
        SEMANTIC_CALL_COUNT += 1
        self._load()
        if not self.enabled or self._model is None:
            SEMANTIC_TOTAL_MS += (time.perf_counter() - started) * 1000.0
            return self._fallback_overlap(a, b)
        try:
            va = self._model.encode([a], normalize_embeddings=True)[0]
            vb = self._model.encode([b], normalize_embeddings=True)[0]
            score = _normalize01(float(np.dot(va, vb)))
            SEMANTIC_HIT_COUNT += 1
            SEMANTIC_TOTAL_MS += (time.perf_counter() - started) * 1000.0
            return score
        except Exception:
            SEMANTIC_LOAD_FAIL += 1
            self.enabled = False
            SEMANTIC_TOTAL_MS += (time.perf_counter() - started) * 1000.0
            return self._fallback_overlap(a, b)


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


def _collaboration_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    """
    协作历史评分：基于历史评分和协作成功率
    优化：区分"有数据但表现中等"和"无数据(新用户)"
    新增：评价数据充足性判断，数据不足时降低权重影响
    """
    user = candidate.get("user") or {}
    
    # 1. 历史评分（来自evaluations表）
    evaluations = user.get("evaluations") or {}
    has_eval_data = False
    eval_score = 0.0
    eval_data_quality = "none"  # none/limited/good
    
    if isinstance(evaluations, dict):
        tech_score = _safe_float(evaluations.get("avg_tech_contribution"), 0.0) / 5.0  # 假设满分5分
        collab_score = _safe_float(evaluations.get("avg_collaboration"), 0.0) / 5.0
        task_score = _safe_float(evaluations.get("avg_task_completion"), 0.0) / 5.0
        
        # 检查是否有真实评分数据
        if tech_score > 0 or collab_score > 0 or task_score > 0:
            has_eval_data = True
            eval_score = (tech_score + collab_score + task_score) / 3.0
            
            # 判断数据质量：如果所有分数都接近0，可能是数据不足
            overall_avg = _safe_float(evaluations.get("overall_avg_score"), 0.0) / 5.0
            has_data_flag = evaluations.get("has_evaluation_data", True)
            
            if overall_avg > 0.1 or (has_data_flag and eval_score > 0.1):
                eval_data_quality = "good"
            else:
                eval_data_quality = "limited"
        else:
            eval_score = _missing_score(False)  # 无数据，使用未知策略(0.35)
            eval_data_quality = "none"
    else:
        eval_score = _missing_score(False)  # 无数据，使用未知策略(0.35)
        eval_data_quality = "none"
    
    # 2. 协作成功率（来自collaboration_history表）
    history = candidate.get("collaboration_history") or []
    has_history_data = False
    if isinstance(history, list) and len(history) > 0:
        has_history_data = True
        total_score = sum(_safe_float(h.get("collaboration_score"), 0.0) for h in history if isinstance(h, dict))
        success_rate = total_score / float(len(history))
    else:
        success_rate = _missing_score(False)  # 无历史数据，使用未知策略(0.35)
    
    # 综合评分：根据数据质量动态调整权重
    # 如果评价数据充足，历史评分权重更高；如果数据不足，降低历史评分权重
    if eval_data_quality == "good":
        # 数据充足：历史评分60% + 协作成功率40%
        final_score = 0.6 * eval_score + 0.4 * success_rate
    elif eval_data_quality == "limited":
        # 数据有限：历史评分40% + 协作成功率60%
        final_score = 0.4 * eval_score + 0.6 * success_rate
    else:
        # 无评价数据：主要依赖协作历史，历史评分30% + 协作成功率70%
        final_score = 0.3 * eval_score + 0.7 * success_rate

    # 3. 导师对成员的评价（补充信号，仅在有数据时生效）
    # - 该数据来自后端 buildUserData() 下发的 user.mentor_member_evaluations
    # - 只做“轻量融合”，避免导师资源差异导致不公平
    mentor_eval = user.get("mentor_member_evaluations") or {}
    if isinstance(mentor_eval, dict) and bool(mentor_eval.get("has_mentor_member_evaluation")):
        # score: 0-100；其他: 1-5
        m_score = _normalize01(_safe_float(mentor_eval.get("avg_score"), 0.0) / 100.0)
        m_collab = _normalize01(_safe_float(mentor_eval.get("avg_collaboration"), 0.0) / 5.0)
        m_learning = _normalize01(_safe_float(mentor_eval.get("avg_learning_attitude"), 0.0) / 5.0)
        m_task = _normalize01(_safe_float(mentor_eval.get("avg_task_completion"), 0.0) / 5.0)

        # 导师信号：优先用细分项；缺失则回退到综合 score
        dims = [v for v in (m_collab, m_learning, m_task) if v > 0]
        if dims:
            mentor_signal = float(sum(dims)) / float(len(dims))
        else:
            mentor_signal = m_score if m_score > 0 else 0.5

        # 轻量融合：最多提升约 0.06（6 个百分点），不会让它“翻盘”其它维度
        max_gain = 0.06
        gain = max_gain * max(0.0, mentor_signal - 0.5) * 2.0  # mentor_signal 0.5->0, 1.0->max_gain
        final_score = _normalize01(final_score + gain)
        _log_debug(f"协作历史：导师评价加成 gain={gain:.3f}, mentor_signal={mentor_signal:.3f}, new={final_score:.3f}")
    
    # 记录数据质量信息（用于调试和日志）
    if not has_eval_data and not has_history_data:
        _log_debug(f"协作历史：新用户，无评价和历史数据，得分={final_score:.3f}")
    elif eval_data_quality == "none":
        _log_debug(f"协作历史：无评价数据，主要依赖协作历史，得分={final_score:.3f}")
    elif eval_data_quality == "limited":
        _log_debug(f"协作历史：评价数据有限，得分={final_score:.3f}")
    
    return _normalize01(final_score)


def _experience_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    """项目经验评分：优先使用系统验证的项目履历，降级到文本经验"""
    user = candidate.get("user") or {}
    user_id = user.get("id")
    
    # 尝试从候选人数据中获取经验分数（后端已经计算好）
    experience_data = candidate.get("experience_score")
    
    if isinstance(experience_data, dict):
        # 使用后端计算的经验分数
        total_score = _safe_float(experience_data.get("totalScore"), 0.0) / 100.0  # 转换为0-1
        is_verified = experience_data.get("isVerified", False)
        
        if is_verified:
            # 系统验证的数据，直接使用
            return _normalize01(total_score)
    
    # 降级方案：使用原有的文本经验计算
    # 1. 项目经验描述长度（简单指标）
    exp_text = (user.get("project_experience") or "").strip()
    exp_length_score = min(1.0, len(exp_text) / 500.0) if exp_text else 0.0
    
    # 2. 声誉分数（reputation_score，默认60，范围0-100）
    reputation = _safe_float(user.get("reputation_score"), 60.0)
    reputation_score = reputation / 100.0
    
    # 综合评分：经验描述30% + 声誉分数70%
    return _normalize01(0.3 * exp_length_score + 0.7 * reputation_score)


def _academic_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    """学术背景评分：基于专业和年级匹配"""
    user = candidate.get("user") or {}
    
    # 1. 专业匹配（简化版：有专业信息就给分）
    major = (user.get("major") or "").strip()
    major_score = 0.7 if major else 0.3
    
    # 2. 年级评分（假设2-4年级最佳，1年级和研究生次之）
    grade = _safe_float(user.get("grade"), 0)
    if 2 <= grade <= 4:
        grade_score = 1.0
    elif grade == 1 or grade >= 5:
        grade_score = 0.7
    else:
        grade_score = 0.5
    
    # 综合评分：专业70% + 年级30%
    return _normalize01(0.7 * major_score + 0.3 * grade_score)


def _application_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    """申请历史评分：基于申请成功率"""
    user = candidate.get("user") or {}
    
    # 申请历史统计
    app_stats = user.get("application_stats") or {}
    if isinstance(app_stats, dict):
        total = _safe_float(app_stats.get("total"), 0)
        approved = _safe_float(app_stats.get("approved"), 0)
        
        if total > 0:
            success_rate = approved / total
            # 申请次数越多，权重越高（但有上限）
            weight = min(1.0, total / 10.0)
            return _normalize01(success_rate * weight + 0.5 * (1 - weight))
    
    # 无申请历史，给予中等分数
    return 0.5


def _mentor_rating_score(project: Dict[str, Any], candidate: Dict[str, Any]) -> float:
    """导师评分维度（5%权重）⭐ 第9维度"""
    user = candidate.get("user") or {}
    
    # 获取用户的导师评分（0-5分）
    mentor_rating = _safe_float(user.get("mentor_rating"), 0.0)
    
    # 转换为0-1范围
    if mentor_rating > 0:
        score = mentor_rating / 5.0
    else:
        # 无评分时给中等分数（假设导师质量中等）
        score = 0.5
    
    return _normalize01(score)


def _compute_breakdown(project: Dict[str, Any], candidate: Dict[str, Any]) -> Dict[str, float]:
    return {
        "skill": _skills_score(project, candidate),
        "collaboration": _collaboration_score(project, candidate),
        "time": _time_score(project, candidate),
        "experience": _experience_score(project, candidate),
        "goal": _goal_score(project, candidate),
        "mentor_rating": _mentor_rating_score(project, candidate),  # ⭐ 新增第9维度
        "academic": _academic_score(project, candidate),
        "credit": _credit_score(candidate),
        "application": _application_score(project, candidate),
    }


def _final_score(breakdown: Dict[str, float], weights: Dict[str, float]) -> float:
    score = 0.0
    for k, w in weights.items():
        score += _safe_float(breakdown.get(k), 0.0) * _safe_float(w, 0.0)
    return float(score)


def _stable_bucket(value: str) -> int:
    h = hashlib.md5(value.encode("utf-8")).hexdigest()
    return int(h[:8], 16) % AB_BUCKETS


def _is_allowed_segment(value: str, allowlist: set[str]) -> bool:
    if AB_SEGMENT_ALLOW_ALL in allowlist:
        return True
    return value in allowlist


def _parse_allowlist(raw: Optional[str], fallback_all: bool = True) -> set[str]:
    if raw is None:
        return {AB_SEGMENT_ALLOW_ALL} if fallback_all else set()
    parsed = {p.strip().lower() for p in str(raw).split(",") if p.strip()}
    if not parsed and fallback_all:
        return {AB_SEGMENT_ALLOW_ALL}
    return parsed


def _update_ab_runtime_config(payload: Dict[str, Any]) -> Dict[str, Any]:
    with AB_CONFIG_LOCK:
        if "experiment" in payload and payload.get("experiment") is not None:
            AB_RUNTIME["experiment"] = str(payload.get("experiment") or "off").strip().lower()

        if "traffic" in payload and payload.get("traffic") is not None:
            AB_RUNTIME["traffic"] = max(0.0, min(1.0, _safe_float(payload.get("traffic"), AB_RUNTIME.get("traffic", 0.0))))

        if "buckets" in payload and payload.get("buckets") is not None:
            AB_RUNTIME["buckets"] = max(1, int(_safe_float(payload.get("buckets"), AB_RUNTIME.get("buckets", 100))))

        if "project_types" in payload:
            AB_RUNTIME["project_types"] = _parse_allowlist(payload.get("project_types"), fallback_all=True)

        if "user_segments" in payload:
            AB_RUNTIME["user_segments"] = _parse_allowlist(payload.get("user_segments"), fallback_all=True)

        AB_RUNTIME["updated_at"] = int(time.time() * 1000)
        return {
            "experiment": AB_RUNTIME["experiment"],
            "traffic": AB_RUNTIME["traffic"],
            "buckets": AB_RUNTIME["buckets"],
            "project_types": sorted(list(AB_RUNTIME["project_types"])),
            "user_segments": sorted(list(AB_RUNTIME["user_segments"])),
            "updated_at": AB_RUNTIME["updated_at"],
        }


def _choose_ab_variant(
    project_id: Any,
    user_id: Any,
    project_type: Optional[str],
    user_segment: Optional[str] = None,
) -> str:
    experiment = str(AB_RUNTIME.get("experiment", "off")).strip().lower()
    traffic = max(0.0, min(1.0, _safe_float(AB_RUNTIME.get("traffic"), 0.0)))
    buckets = max(1, int(_safe_float(AB_RUNTIME.get("buckets"), AB_BUCKETS)))
    allowed_project_types = AB_RUNTIME.get("project_types") or {AB_SEGMENT_ALLOW_ALL}
    allowed_user_segments = AB_RUNTIME.get("user_segments") or {AB_SEGMENT_ALLOW_ALL}

    if experiment == "off" or traffic <= 0:
        return "control"

    project_type_key = _normalize_text_key(project_type or "default")
    segment_key = _normalize_text_key(user_segment)

    if not _is_allowed_segment(project_type_key, allowed_project_types):
        return "control"
    if not _is_allowed_segment(segment_key, allowed_user_segments):
        return "control"

    key = f"{project_id}:{user_id}:{project_type_key}:{segment_key}"
    bucket = _stable_bucket(key) % buckets
    threshold = int(buckets * traffic)
    return "treatment" if bucket < threshold else "control"


def _default_strategy() -> Dict[str, Any]:
    return {
        "weights_control": DEFAULT_WEIGHTS,
        "weights_treatment": {
            **DEFAULT_WEIGHTS,
            "skill": 0.34,
            "collaboration": 0.26,
            "time": 0.12,
            "goal": 0.09,
            "mentor_rating": 0.06,
        },
    }


def _load_strategy() -> Dict[str, Any]:
    if not STRATEGY_FILE.exists():
        return _default_strategy()
    try:
        data = json.loads(STRATEGY_FILE.read_text(encoding="utf-8"))
        control = _normalize_weights(data.get("weights_control") or DEFAULT_WEIGHTS)
        treatment = _normalize_weights(data.get("weights_treatment") or data.get("weights_control") or DEFAULT_WEIGHTS)
        return {"weights_control": control, "weights_treatment": treatment}
    except Exception:
        return _default_strategy()


def _effective_weights_by_variant(base: Dict[str, float], variant: str) -> Dict[str, float]:
    if AB_EXPERIMENT != "weights_v2":
        return base
    strategy = _load_strategy()
    selected = strategy["weights_treatment"] if variant == "treatment" else strategy["weights_control"]
    merged = {k: _safe_float(selected.get(k), base.get(k, 0.0)) for k in DEFAULT_WEIGHTS.keys()}
    return _normalize_weights(merged)


def _resolve_strategy_version(file_name: str) -> int:
    m = re.match(r"^v(\d+)\.json$", file_name.strip())
    return int(m.group(1)) if m else -1


def _next_strategy_file() -> Path:
    STRATEGY_BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    versions = [_resolve_strategy_version(p.name) for p in STRATEGY_BACKUP_DIR.glob("v*.json")]
    latest = max(versions) if versions else 0
    return STRATEGY_BACKUP_DIR / f"v{latest + 1}.json"


def _list_strategy_versions() -> List[Dict[str, Any]]:
    if not STRATEGY_BACKUP_DIR.exists():
        return []

    items: List[Dict[str, Any]] = []
    for p in sorted(STRATEGY_BACKUP_DIR.glob("v*.json"), key=lambda x: _resolve_strategy_version(x.name), reverse=True):
        version = _resolve_strategy_version(p.name)
        if version <= 0:
            continue
        stat = p.stat()
        items.append(
            {
                "version": version,
                "file": str(p),
                "updated_at": int(stat.st_mtime * 1000),
            }
        )
    return items


def _save_strategy_payload(strategy_payload: Dict[str, Any], note: Optional[str] = None) -> Dict[str, Any]:
    strategy = {
        "weights_control": _normalize_weights(strategy_payload.get("weights_control") or DEFAULT_WEIGHTS),
        "weights_treatment": _normalize_weights(strategy_payload.get("weights_treatment") or strategy_payload.get("weights_control") or DEFAULT_WEIGHTS),
    }
    now_ms = int(time.time() * 1000)
    version_file = _next_strategy_file()
    version = _resolve_strategy_version(version_file.name)

    full_payload = {
        **strategy,
        "version": version,
        "updated_at": now_ms,
        "note": str(note or "").strip(),
    }

    STRATEGY_FILE.parent.mkdir(parents=True, exist_ok=True)
    STRATEGY_FILE.write_text(json.dumps(full_payload, ensure_ascii=False, indent=2), encoding="utf-8")
    version_file.write_text(json.dumps(full_payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return full_payload


def _load_strategy_version(version: int) -> Optional[Dict[str, Any]]:
    if version <= 0:
        return None
    p = STRATEGY_BACKUP_DIR / f"v{version}.json"
    if not p.exists():
        return None
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        return None


def _record_offline_event(event: Dict[str, Any]) -> None:
    try:
        OFFLINE_EVAL_FILE.parent.mkdir(parents=True, exist_ok=True)
        with OFFLINE_EVAL_FILE.open("a", encoding="utf-8") as f:
            f.write(json.dumps(event, ensure_ascii=False) + "\n")
    except Exception:
        pass


def _compute_offline_metrics(events: List[Dict[str, Any]]) -> Dict[str, Any]:
    if not events:
        return {
            "samples": 0,
            "precision_at_k": 0.0,
            "recall_at_k": 0.0,
            "ndcg_at_k": 0.0,
            "coverage": 0.0,
            "cold_start_performance": 0.0,
            "version_compare": {"control_avg": 0.0, "treatment_avg": 0.0, "ltr_avg": 0.0},
        }

    k = max(1, int(_safe_float(os.getenv("MATCHING_OFFLINE_K", "10"), 10)))
    ranked = sorted(events, key=lambda x: _safe_float(x.get("score"), 0.0), reverse=True)
    topk = ranked[:k]
    positives = [e for e in events if bool(e.get("label_positive"))]
    tp_topk = sum(1 for e in topk if bool(e.get("label_positive")))

    precision = tp_topk / float(max(1, len(topk)))
    recall = tp_topk / float(max(1, len(positives)))

    dcg = 0.0
    for i, e in enumerate(topk):
        rel = 1.0 if bool(e.get("label_positive")) else 0.0
        dcg += rel / np.log2(i + 2)
    ideal_count = min(len(positives), k)
    idcg = sum(1.0 / np.log2(i + 2) for i in range(ideal_count)) if ideal_count > 0 else 0.0
    ndcg = (dcg / idcg) if idcg > 0 else 0.0

    unique_users = set(str(e.get("user_id")) for e in events if e.get("user_id") is not None)
    covered_users = set(str(e.get("user_id")) for e in topk if bool(e.get("label_positive")))
    coverage = len(covered_users) / float(max(1, len(unique_users)))

    cold_events = [e for e in events if bool(e.get("cold_start"))]
    cold_perf = sum(1.0 if bool(e.get("label_positive")) else 0.0 for e in cold_events) / float(max(1, len(cold_events)))

    control_scores = [_safe_float(e.get("score"), 0.0) for e in events if (e.get("variant") or "control") == "control"]
    treatment_scores = [_safe_float(e.get("score"), 0.0) for e in events if (e.get("variant") or "control") == "treatment"]
    ltr_scores = [_safe_float(e.get("score"), 0.0) for e in events if bool(e.get("ltr_enabled"))]

    return {
        "samples": len(events),
        "precision_at_k": round(precision, 4),
        "recall_at_k": round(recall, 4),
        "ndcg_at_k": round(ndcg, 4),
        "coverage": round(coverage, 4),
        "cold_start_performance": round(cold_perf, 4),
        "version_compare": {
            "control_avg": round(float(sum(control_scores)) / max(1, len(control_scores)), 4),
            "treatment_avg": round(float(sum(treatment_scores)) / max(1, len(treatment_scores)), 4),
            "ltr_avg": round(float(sum(ltr_scores)) / max(1, len(ltr_scores)), 4),
        },
    }


def _extract_ltr_features(breakdown: Dict[str, float]) -> List[float]:
    return [
        _normalize01(_safe_float(breakdown.get("skill"), 0.0)),
        _normalize01(_safe_float(breakdown.get("collaboration"), 0.0)),
        _normalize01(_safe_float(breakdown.get("time"), 0.0)),
        _normalize01(_safe_float(breakdown.get("experience"), 0.0)),
        _normalize01(_safe_float(breakdown.get("goal"), 0.0)),
        _normalize01(_safe_float(breakdown.get("mentor_rating"), 0.0)),
        _normalize01(_safe_float(breakdown.get("academic"), 0.0)),
        _normalize01(_safe_float(breakdown.get("credit"), 0.0)),
        _normalize01(_safe_float(breakdown.get("application"), 0.0)),
    ]


def _sigmoid(x: float) -> float:
    try:
        return 1.0 / (1.0 + np.exp(-float(x)))
    except Exception:
        return 0.5


def _default_ltr_state() -> Dict[str, Any]:
    return {
        "enabled": False,
        "alpha": 0.45,
        "version": 0,
        "updated_at": 0,
        "coefficients": [0.0] * 9,
        "intercept": 0.0,
        "metrics": {
            "samples": 0,
            "positive_rate": 0.0,
            "auc": 0.0,
            "logloss": 0.0,
        },
        "note": "",
    }


def _safe_ltr_state(raw: Dict[str, Any]) -> Dict[str, Any]:
    base = _default_ltr_state()
    coeffs = raw.get("coefficients")
    if not isinstance(coeffs, list) or len(coeffs) != 9:
        coeffs = base["coefficients"]
    base.update(
        {
            "enabled": bool(raw.get("enabled", base["enabled"])),
            "alpha": max(0.0, min(1.0, _safe_float(raw.get("alpha"), base["alpha"]))),
            "version": max(0, int(_safe_float(raw.get("version"), base["version"]))),
            "updated_at": max(0, int(_safe_float(raw.get("updated_at"), base["updated_at"]))),
            "coefficients": [_safe_float(c, 0.0) for c in coeffs],
            "intercept": _safe_float(raw.get("intercept"), base["intercept"]),
            "metrics": raw.get("metrics") if isinstance(raw.get("metrics"), dict) else base["metrics"],
            "note": str(raw.get("note") or ""),
        }
    )
    return base


def _load_ltr_state() -> Dict[str, Any]:
    if not LTR_MODEL_FILE.exists():
        return _default_ltr_state()
    try:
        return _safe_ltr_state(json.loads(LTR_MODEL_FILE.read_text(encoding="utf-8")))
    except Exception:
        return _default_ltr_state()


def _resolve_ltr_version(file_name: str) -> int:
    m = re.match(r"^v(\d+)\.json$", file_name.strip())
    return int(m.group(1)) if m else -1


def _next_ltr_model_file() -> Path:
    LTR_MODEL_BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    versions = [_resolve_ltr_version(p.name) for p in LTR_MODEL_BACKUP_DIR.glob("v*.json")]
    latest = max(versions) if versions else 0
    return LTR_MODEL_BACKUP_DIR / f"v{latest + 1}.json"


def _save_ltr_state(state: Dict[str, Any]) -> Dict[str, Any]:
    safe_state = _safe_ltr_state(state)
    version_file = _next_ltr_model_file()
    safe_state["version"] = _resolve_ltr_version(version_file.name)
    safe_state["updated_at"] = int(time.time() * 1000)
    LTR_MODEL_FILE.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(safe_state, ensure_ascii=False, indent=2)
    LTR_MODEL_FILE.write_text(payload, encoding="utf-8")
    version_file.write_text(payload, encoding="utf-8")
    return safe_state


def _list_ltr_versions() -> List[Dict[str, Any]]:
    if not LTR_MODEL_BACKUP_DIR.exists():
        return []
    items: List[Dict[str, Any]] = []
    for p in sorted(LTR_MODEL_BACKUP_DIR.glob("v*.json"), key=lambda x: _resolve_ltr_version(x.name), reverse=True):
        version = _resolve_ltr_version(p.name)
        if version <= 0:
            continue
        items.append({"version": version, "file": str(p), "updated_at": int(p.stat().st_mtime * 1000)})
    return items


def _load_ltr_version(version: int) -> Optional[Dict[str, Any]]:
    if version <= 0:
        return None
    p = LTR_MODEL_BACKUP_DIR / f"v{version}.json"
    if not p.exists():
        return None
    try:
        return _safe_ltr_state(json.loads(p.read_text(encoding="utf-8")))
    except Exception:
        return None


def _train_ltr_model(events: List[Dict[str, Any]], min_samples: int) -> Tuple[Optional[Dict[str, Any]], Dict[str, Any]]:
    valid: List[Dict[str, Any]] = []
    for e in events:
        breakdown = e.get("breakdown")
        if not isinstance(breakdown, dict):
            continue
        if e.get("label_positive") is None:
            continue
        valid.append(e)

    if len(valid) < min_samples:
        return None, {"message": "insufficient samples", "samples": len(valid), "required": min_samples}

    X = np.array([_extract_ltr_features(v.get("breakdown") or {}) for v in valid], dtype=float)
    y = np.array([1.0 if bool(v.get("label_positive")) else 0.0 for v in valid], dtype=float)

    coeffs = np.zeros(X.shape[1], dtype=float)
    intercept = 0.0
    lr = 0.1
    l2 = 0.01
    epochs = 400

    for _ in range(epochs):
        logits = X.dot(coeffs) + intercept
        preds = 1.0 / (1.0 + np.exp(-np.clip(logits, -30, 30)))
        error = preds - y
        grad_w = X.T.dot(error) / len(X) + l2 * coeffs
        grad_b = float(np.mean(error))
        coeffs -= lr * grad_w
        intercept -= lr * grad_b

    logits = X.dot(coeffs) + intercept
    preds = 1.0 / (1.0 + np.exp(-np.clip(logits, -30, 30)))
    eps = 1e-9
    logloss = float(-np.mean(y * np.log(preds + eps) + (1 - y) * np.log(1 - preds + eps)))

    pos = preds[y >= 0.5]
    neg = preds[y < 0.5]
    if len(pos) == 0 or len(neg) == 0:
        auc = 0.5
    else:
        compare = 0.0
        total = float(len(pos) * len(neg))
        for p in pos:
            for n in neg:
                if p > n:
                    compare += 1.0
                elif p == n:
                    compare += 0.5
        auc = compare / total if total > 0 else 0.5

    trained = {
        "enabled": True,
        "alpha": _load_ltr_state().get("alpha", 0.45),
        "coefficients": [float(v) for v in coeffs.tolist()],
        "intercept": float(intercept),
        "metrics": {
            "samples": int(len(valid)),
            "positive_rate": round(float(np.mean(y)), 4),
            "auc": round(float(auc), 4),
            "logloss": round(logloss, 6),
        },
    }
    return trained, {"message": "ok", "samples": len(valid)}


def _predict_ltr_probability(breakdown: Dict[str, float], ltr_state: Dict[str, Any]) -> float:
    coeffs = ltr_state.get("coefficients") or []
    if not isinstance(coeffs, list) or len(coeffs) != 9:
        return 0.5
    feats = _extract_ltr_features(breakdown)
    z = float(sum(_safe_float(coeffs[i], 0.0) * feats[i] for i in range(9)) + _safe_float(ltr_state.get("intercept"), 0.0))
    return _normalize01(_sigmoid(z))


def _blend_ltr_score(base_score: float, breakdown: Dict[str, float], ltr_state: Dict[str, Any]) -> Tuple[float, float]:
    if not bool(ltr_state.get("enabled")):
        return _normalize01(base_score), 0.0
    alpha = max(0.0, min(1.0, _safe_float(ltr_state.get("alpha"), 0.45)))
    ltr_prob = _predict_ltr_probability(breakdown, ltr_state)
    blended = (1.0 - alpha) * _normalize01(base_score) + alpha * ltr_prob
    return _normalize01(blended), ltr_prob


FEEDBACK_WEIGHT_FILE = Path(os.getenv("MATCHING_WEIGHTS_FILE", "matching-service/feedback_weights.json"))
_feedback_lock = Lock()


def _normalize_weights(weights: Dict[str, float]) -> Dict[str, float]:
    cleaned = {k: max(0.001, _safe_float(v, DEFAULT_WEIGHTS.get(k, 0.0))) for k, v in weights.items() if k in DEFAULT_WEIGHTS}
    missing_keys = [k for k in DEFAULT_WEIGHTS.keys() if k not in cleaned]
    for key in missing_keys:
        cleaned[key] = DEFAULT_WEIGHTS[key]
    s = sum(cleaned.values())
    if s <= 0:
        return DEFAULT_WEIGHTS.copy()
    return {k: v / s for k, v in cleaned.items()}


def _load_feedback_state() -> Dict[str, Any]:
    if not FEEDBACK_WEIGHT_FILE.exists():
        return {
            "weights": DEFAULT_WEIGHTS.copy(),
            "feedback_count": 0,
            "pending_invites": {},
            "weights_by_type": {},
            "feedback_count_by_type": {},
        }
    try:
        data = json.loads(FEEDBACK_WEIGHT_FILE.read_text(encoding="utf-8"))
        weights = _normalize_weights(data.get("weights") or DEFAULT_WEIGHTS)
        feedback_count = int(data.get("feedback_count") or 0)
        pending_invites = data.get("pending_invites") or {}
        weights_by_type = data.get("weights_by_type") or {}
        feedback_count_by_type = data.get("feedback_count_by_type") or {}

        if not isinstance(pending_invites, dict):
            pending_invites = {}
        if not isinstance(weights_by_type, dict):
            weights_by_type = {}
        if not isinstance(feedback_count_by_type, dict):
            feedback_count_by_type = {}

        normalized_by_type = {}
        for k, v in weights_by_type.items():
            if isinstance(v, dict):
                normalized_by_type[str(k)] = _normalize_weights(v)

        return {
            "weights": weights,
            "feedback_count": max(0, feedback_count),
            "pending_invites": pending_invites,
            "weights_by_type": normalized_by_type,
            "feedback_count_by_type": feedback_count_by_type,
        }
    except Exception:
        return {
            "weights": DEFAULT_WEIGHTS.copy(),
            "feedback_count": 0,
            "pending_invites": {},
            "weights_by_type": {},
            "feedback_count_by_type": {},
        }


def _save_feedback_state(state: Dict[str, Any]) -> None:
    FEEDBACK_WEIGHT_FILE.parent.mkdir(parents=True, exist_ok=True)
    FEEDBACK_WEIGHT_FILE.write_text(
        json.dumps(state, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def _type_key(project_type: Optional[str]) -> str:
    value = (project_type or "default").strip().lower()
    return value if value else "default"


def _get_effective_weights(project_type: Optional[str] = None) -> Dict[str, float]:
    state = _load_feedback_state()
    type_key = _type_key(project_type)
    dynamic = (state.get("weights_by_type") or {}).get(type_key) or state["weights"]
    return {
        "skill": _safe_float(os.getenv("WEIGHT_SKILL"), dynamic["skill"]),
        "collaboration": _safe_float(os.getenv("WEIGHT_COLLABORATION"), dynamic["collaboration"]),
        "time": _safe_float(os.getenv("WEIGHT_TIME"), dynamic["time"]),
        "experience": _safe_float(os.getenv("WEIGHT_EXPERIENCE"), dynamic["experience"]),
        "goal": _safe_float(os.getenv("WEIGHT_GOAL"), dynamic["goal"]),
        "mentor_rating": _safe_float(os.getenv("WEIGHT_MENTOR_RATING"), dynamic["mentor_rating"]),
        "academic": _safe_float(os.getenv("WEIGHT_ACADEMIC"), dynamic["academic"]),
        "credit": _safe_float(os.getenv("WEIGHT_CREDIT"), dynamic["credit"]),
        "application": _safe_float(os.getenv("WEIGHT_APPLICATION"), dynamic["application"]),
    }


def _update_weights_with_feedback(
    project_id: int,
    user_id: int,
    project_type: Optional[str],
    event: str,
    breakdown: Dict[str, float],
) -> MatchWeightsResponse:
    event_norm = (event or "").strip().upper()
    valid_events = {
        "INVITED",
        "INVITE_ACCEPTED",
        "INVITE_DECLINED",
        "APPLICATION_APPROVED",
        "APPLICATION_REJECTED",
    }
    if event_norm not in valid_events:
        raise ValueError("event must be one of INVITED/INVITE_ACCEPTED/INVITE_DECLINED/APPLICATION_APPROVED/APPLICATION_REJECTED")

    reward_map = {
        "INVITED": 0.006,
        "INVITE_ACCEPTED": 0.05,
        "INVITE_DECLINED": -0.04,
        "APPLICATION_APPROVED": 0.045,
        "APPLICATION_REJECTED": -0.035,
    }
    lr = _safe_float(os.getenv("MATCHING_FEEDBACK_LR"), 0.1)
    max_step = _safe_float(os.getenv("MATCHING_FEEDBACK_MAX_STEP"), 0.01)
    warmup_min = int(_safe_float(os.getenv("MATCHING_FEEDBACK_WARMUP_MIN"), 30))
    ema_alpha = _safe_float(os.getenv("MATCHING_FEEDBACK_EMA_ALPHA"), 0.2)

    with _feedback_lock:
        state = _load_feedback_state()
        type_key = _type_key(project_type)
        pending = state.get("pending_invites") or {}
        pair_key = f"{project_id}:{user_id}"

        by_type = state.get("weights_by_type") or {}
        count_by_type = state.get("feedback_count_by_type") or {}
        current = by_type.get(type_key) or state["weights"]
        updated = current.copy()

        effective_breakdown = breakdown or {}

        if event_norm == "INVITED":
            if effective_breakdown:
                pending[pair_key] = {
                    "type": type_key,
                    "breakdown": {k: _normalize01(_safe_float(v, 0.0)) for k, v in effective_breakdown.items() if k in DEFAULT_WEIGHTS},
                }
            state["pending_invites"] = pending
            state["feedback_count"] = int(state.get("feedback_count", 0)) + 1
            count_by_type[type_key] = int(count_by_type.get(type_key, 0)) + 1
            state["feedback_count_by_type"] = count_by_type
            _save_feedback_state(state)
            return MatchWeightsResponse(weights=updated, feedback_count=state["feedback_count"])

        if not effective_breakdown and pair_key in pending:
            cached = pending.get(pair_key) or {}
            if isinstance(cached, dict):
                cached_breakdown = cached.get("breakdown") if isinstance(cached.get("breakdown"), dict) else {}
                effective_breakdown = cached_breakdown
                cached_type = cached.get("type")
                if cached_type:
                    type_key = str(cached_type)
                    current = (state.get("weights_by_type") or {}).get(type_key) or state["weights"]
                    updated = current.copy()

        reward = reward_map[event_norm]
        current_type_count = int(count_by_type.get(type_key, 0))
        warmup_factor = min(1.0, current_type_count / float(max(1, warmup_min)))
        effective_lr = lr * warmup_factor

        if effective_breakdown:
            raw_updated = updated.copy()
            for key in DEFAULT_WEIGHTS.keys():
                dim_score = _normalize01(_safe_float(effective_breakdown.get(key), 0.0))
                delta = effective_lr * reward * (dim_score - 0.5)
                if delta > max_step:
                    delta = max_step
                elif delta < -max_step:
                    delta = -max_step
                raw_updated[key] = _safe_float(raw_updated.get(key), 0.0) + delta

            raw_updated = _normalize_weights(raw_updated)
            updated = {
                k: _normalize01((1 - ema_alpha) * _safe_float(current.get(k), 0.0) + ema_alpha * _safe_float(raw_updated.get(k), 0.0))
                for k in DEFAULT_WEIGHTS.keys()
            }
            updated = _normalize_weights(updated)

        pending.pop(pair_key, None)
        by_type[type_key] = updated
        state["weights_by_type"] = by_type
        state["weights"] = updated if type_key == "default" else state["weights"]
        state["pending_invites"] = pending
        state["feedback_count"] = int(state.get("feedback_count", 0)) + 1
        count_by_type[type_key] = int(count_by_type.get(type_key, 0)) + 1
        state["feedback_count_by_type"] = count_by_type
        _save_feedback_state(state)

    return MatchWeightsResponse(weights=updated, feedback_count=state["feedback_count"])


app = FastAPI(title="Matching Service", version="1.0.0")

# 添加请求验证错误处理
from fastapi import Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    _log_error("请求验证失败")
    _log_error(f"URL: {request.url}")
    _log_error(f"错误详情: {exc.errors()}")
    try:
        body = await request.body()
        _log_error(f"请求体: {body.decode('utf-8')[:500]}")  # 只打印前500字符
    except Exception as e:
        _log_error(f"无法读取请求体: {e}")
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={"detail": exc.errors()},
    )


@app.get("/health")
def health() -> Dict[str, Any]:
    return {
        "status": "ok",
        "semantic_enabled": _embedder.enabled,
        "semantic_model": _embedder.model_name,
    }


@app.post("/api/matching/calculate", response_model=List[MatchResult])
def calculate(req: MatchRequest) -> List[MatchResult]:
    """项目招募成员：为项目匹配候选人"""
    started_at = time.perf_counter()
    project = req.project or {}
    
    _log_info("=" * 60)
    _log_info(f"开始匹配计算 - 项目ID: {req.project_id}")
    _log_info(f"项目标题: {project.get('title')}")
    _log_info(f"候选人数量: {len(req.candidates)}")
    _log_info("=" * 60)

    ab_variant = _choose_ab_variant(req.project_id, "batch", project.get("project_type"), AB_SEGMENT_NOT_SET)
    base_weights = _effective_weights_by_variant(_get_effective_weights(project.get("project_type") or project.get("type")), ab_variant)
    weights = _effective_weights_for_request(project, base_weights)
    ltr_state = _load_ltr_state()
    
    _log_info(f"权重配置: {weights}, AB={ab_variant}, LTR={ltr_state.get('enabled')}")

    hard_filter_required_skills = os.getenv("MATCHING_HARD_FILTER_REQUIRED_SKILLS", "1").strip() != "0"
    hard_filter_time = os.getenv("MATCHING_HARD_FILTER_TIME", "0").strip() != "0"
    hard_filter_time_min_overlap = max(0.0, min(1.0, _safe_float(os.getenv("MATCHING_HARD_FILTER_TIME_MIN_OVERLAP", "0.05"), 0.05)))

    results: List[MatchResult] = []
    for idx, c in enumerate(req.candidates):
        user = c.get("user") or {}
        uid = user.get("id")
        if uid is None:
            continue
        
        _log_info(f"处理候选人 {idx+1}/{len(req.candidates)} - 用户ID: {uid}, 用户名: {user.get('username')}")

        if hard_filter_required_skills and _required_skill_missing(project, c):
            _log_info(f"硬过滤：缺失必需技能，跳过 user_id={uid}")
            continue

        time_analysis = _time_overlap_analysis(project, c)
        if hard_filter_time and _has_project_time_slots(project) and _safe_float(time_analysis.get("score"), 0.0) <= hard_filter_time_min_overlap:
            _log_info(f"硬过滤：时间重叠过低({time_analysis.get('score')}), 跳过 user_id={uid}")
            continue

        breakdown = _compute_breakdown(project, c)
        breakdown["time"] = _safe_float(time_analysis.get("score"), breakdown.get("time", 0.5))
        base_score = _final_score(breakdown, weights)
        score, ltr_prob = _blend_ltr_score(base_score, breakdown, ltr_state)

        confidence, confidence_level, risk_level = _compute_confidence_and_risk(project, c, breakdown)
        
        _log_info("各维度得分:")
        for dim, val in breakdown.items():
            _log_info(f"  - {dim}: {val:.3f} (权重: {weights.get(dim, 0):.2f})")
        _log_info(f"最终得分: {score:.3f} (base={base_score:.3f}, ltr={ltr_prob:.3f})")
        
        results.append(
            MatchResult(
                user_id=int(uid),
                score=float(score),
                breakdown={k: float(v) for k, v in breakdown.items()},
                time_explanation=str(time_analysis.get("explanation") or ""),
                confidence=float(confidence),
                confidenceLevel=str(confidence_level),
                riskLevel=str(risk_level),
            )
        )

        _record_offline_event({
            "ts": int(time.time() * 1000),
            "project_id": req.project_id,
            "user_id": int(uid),
            "project_type": project.get("project_type"),
            "variant": ab_variant,
            "score": float(score),
            "base_score": float(base_score),
            "ltr_score": float(ltr_prob),
            "ltr_enabled": bool(ltr_state.get("enabled")),
            "breakdown": {k: float(v) for k, v in breakdown.items()},
            "confidence": float(confidence),
            "label_positive": bool(c.get("label_positive") or c.get("accepted") or c.get("approved")),
            "cold_start": bool(c.get("is_newbie")),
        })

    results.sort(key=lambda r: r.score, reverse=True)
    
    _log_info(f"匹配完成，返回 {len(results)} 个结果")
    if results:
        _log_info(f"最高分: {results[0].score:.3f}, 最低分: {results[-1].score:.3f}")

    duration_ms = (time.perf_counter() - started_at) * 1000.0
    _record_request_metrics(duration_ms, len(results))
    _log_info(f"匹配耗时: {duration_ms:.2f}ms")
    _log_info("=" * 60)
    
    return results


@app.post("/api/matching/feedback", response_model=MatchWeightsResponse)
def update_matching_feedback(req: MatchFeedbackRequest) -> MatchWeightsResponse:
    """匹配反馈闭环：邀请/通过/拒绝结果回流，自动调权重"""
    _record_feedback_metrics(req.event)
    return _update_weights_with_feedback(req.project_id, req.user_id, req.project_type, req.event, req.breakdown or {})


@app.get("/api/matching/weights", response_model=MatchWeightsResponse)
def get_matching_weights() -> MatchWeightsResponse:
    """查看当前动态权重及累计反馈数"""
    state = _load_feedback_state()
    return MatchWeightsResponse(weights=state["weights"], feedback_count=state["feedback_count"])


@app.get("/api/matching/offline-metrics")
def get_offline_metrics() -> Dict[str, Any]:
    """离线评估指标：Precision@K、Recall@K、NDCG、覆盖率、冷启动表现与版本对比"""
    events: List[Dict[str, Any]] = []
    if OFFLINE_EVAL_FILE.exists():
        try:
            for line in OFFLINE_EVAL_FILE.read_text(encoding="utf-8").splitlines():
                line = line.strip()
                if not line:
                    continue
                try:
                    events.append(json.loads(line))
                except Exception:
                    continue
        except Exception:
            pass
    return _compute_offline_metrics(events)


@app.get("/api/matching/strategy/versions")
def list_matching_strategy_versions() -> Dict[str, Any]:
    """列出策略版本历史（用于回滚）"""
    current: Dict[str, Any] = {}
    if STRATEGY_FILE.exists():
        try:
            current = json.loads(STRATEGY_FILE.read_text(encoding="utf-8"))
        except Exception:
            current = {}
    return {
        "current": {
            "version": int(_safe_float(current.get("version"), 0)),
            "updated_at": int(_safe_float(current.get("updated_at"), 0)),
            "note": str(current.get("note") or ""),
        },
        "versions": _list_strategy_versions(),
    }


@app.post("/api/matching/strategy/update")
def update_matching_strategy(req: StrategyUpdateRequest) -> Dict[str, Any]:
    """更新策略权重并生成新版本"""
    saved = _save_strategy_payload(
        {
            "weights_control": req.weights_control,
            "weights_treatment": req.weights_treatment,
        },
        note=req.note,
    )
    return {
        "message": "strategy updated",
        "version": saved.get("version"),
        "updated_at": saved.get("updated_at"),
        "weights_control": saved.get("weights_control"),
        "weights_treatment": saved.get("weights_treatment"),
    }


@app.post("/api/matching/strategy/rollback")
def rollback_matching_strategy(req: StrategyRollbackRequest) -> Dict[str, Any]:
    """回滚到指定策略版本，并保存为新版本"""
    source = _load_strategy_version(req.version)
    if not source:
        return {"message": "version not found", "version": req.version}

    note = req.note or f"rollback_from_v{req.version}"
    saved = _save_strategy_payload(source, note=note)
    return {
        "message": "rollback success",
        "from_version": req.version,
        "new_version": saved.get("version"),
        "updated_at": saved.get("updated_at"),
    }


@app.get("/api/matching/ab/config")
def get_ab_runtime_config() -> Dict[str, Any]:
    """读取AB实验配置（支持按项目类型与用户分层）"""
    return {
        "experiment": AB_RUNTIME.get("experiment", "off"),
        "traffic": AB_RUNTIME.get("traffic", 0.0),
        "buckets": AB_RUNTIME.get("buckets", AB_BUCKETS),
        "project_types": sorted(list(AB_RUNTIME.get("project_types") or {AB_SEGMENT_ALLOW_ALL})),
        "user_segments": sorted(list(AB_RUNTIME.get("user_segments") or {AB_SEGMENT_ALLOW_ALL})),
        "updated_at": AB_RUNTIME.get("updated_at"),
    }


@app.post("/api/matching/ab/config")
def update_ab_runtime_config(payload: Dict[str, Any]) -> Dict[str, Any]:
    """热更新AB实验配置与灰度策略"""
    updated = _update_ab_runtime_config(payload)
    return {
        "message": "ab config updated",
        **updated,
    }


@app.get("/api/matching/ltr/config")
def get_ltr_config() -> Dict[str, Any]:
    """获取LTR在线配置"""
    state = _load_ltr_state()
    return {
        "enabled": bool(state.get("enabled")),
        "alpha": _safe_float(state.get("alpha"), 0.45),
        "version": int(_safe_float(state.get("version"), 0)),
        "updated_at": int(_safe_float(state.get("updated_at"), 0)),
        "metrics": state.get("metrics") if isinstance(state.get("metrics"), dict) else {},
        "note": str(state.get("note") or ""),
    }


@app.post("/api/matching/ltr/config")
def update_ltr_config(req: LtrConfigRequest) -> Dict[str, Any]:
    """更新LTR在线配置（开关、融合系数）"""
    state = _load_ltr_state()
    if req.enabled is not None:
        state["enabled"] = bool(req.enabled)
    if req.alpha is not None:
        state["alpha"] = max(0.0, min(1.0, _safe_float(req.alpha, state.get("alpha", 0.45))))
    state["note"] = str(state.get("note") or "")
    saved = _save_ltr_state(state)
    return {
        "message": "ltr config updated",
        "enabled": saved.get("enabled"),
        "alpha": saved.get("alpha"),
        "version": saved.get("version"),
        "updated_at": saved.get("updated_at"),
    }


@app.get("/api/matching/ltr/versions")
def list_ltr_versions() -> Dict[str, Any]:
    """列出LTR模型版本"""
    current = _load_ltr_state()
    return {
        "current": {
            "version": int(_safe_float(current.get("version"), 0)),
            "updated_at": int(_safe_float(current.get("updated_at"), 0)),
            "enabled": bool(current.get("enabled")),
            "alpha": _safe_float(current.get("alpha"), 0.45),
            "note": str(current.get("note") or ""),
        },
        "versions": _list_ltr_versions(),
    }


@app.post("/api/matching/ltr/train")
def train_ltr_model(req: LtrTrainRequest) -> Dict[str, Any]:
    """基于离线反馈样本训练LTR模型并发布新版本"""
    events: List[Dict[str, Any]] = []
    if OFFLINE_EVAL_FILE.exists():
        try:
            for line in OFFLINE_EVAL_FILE.read_text(encoding="utf-8").splitlines():
                line = line.strip()
                if not line:
                    continue
                try:
                    events.append(json.loads(line))
                except Exception:
                    continue
        except Exception:
            pass

    min_samples = max(20, int(_safe_float(req.min_samples, 200)))
    trained, info = _train_ltr_model(events, min_samples)
    if not trained:
        return {"message": info.get("message"), "samples": info.get("samples"), "required": info.get("required")}

    trained["note"] = str(req.note or "trained_from_offline_eval")
    saved = _save_ltr_state(trained)
    return {
        "message": "ltr trained",
        "version": saved.get("version"),
        "updated_at": saved.get("updated_at"),
        "enabled": saved.get("enabled"),
        "alpha": saved.get("alpha"),
        "metrics": saved.get("metrics"),
    }


@app.post("/api/matching/ltr/rollback")
def rollback_ltr_model(req: LtrRollbackRequest) -> Dict[str, Any]:
    """回滚到指定LTR版本并发布新版本"""
    source = _load_ltr_version(req.version)
    if not source:
        return {"message": "version not found", "version": req.version}
    source["note"] = str(req.note or f"rollback_from_v{req.version}")
    saved = _save_ltr_state(source)
    return {
        "message": "ltr rollback success",
        "from_version": req.version,
        "new_version": saved.get("version"),
        "updated_at": saved.get("updated_at"),
        "enabled": saved.get("enabled"),
        "alpha": saved.get("alpha"),
    }


@app.get("/api/matching/metrics")
def get_matching_metrics() -> Dict[str, Any]:
    """关键观测指标：QPS、P95耗时、召回数、空结果率、反馈闭环指标"""
    with _METRIC_LOCK:
        durations = list(_REQUEST_TIMES_MS)
        recalls = list(_RECENT_RESULTS)
        total_requests = int(_TOTAL_REQUESTS)
        total_feedback = int(_TOTAL_FEEDBACK)
        feedback_events = dict(_FEEDBACK_EVENTS)

    qps = 0.0
    if durations:
        qps = min(float(len(durations)), 1000.0) / max(1.0, sum(durations) / 1000.0)

    empty_count = sum(1 for n in recalls if int(n) <= 0)
    invite_count = feedback_events.get("INVITED", 0)
    invite_accepted = feedback_events.get("INVITE_ACCEPTED", 0)
    invite_declined = feedback_events.get("INVITE_DECLINED", 0)
    app_approved = feedback_events.get("APPLICATION_APPROVED", 0)
    app_rejected = feedback_events.get("APPLICATION_REJECTED", 0)

    return {
        "qps": round(qps, 4),
        "p95_latency_ms": round(_p95(durations), 2),
        "avg_recall_count": round(float(sum(recalls)) / len(recalls), 2) if recalls else 0.0,
        "empty_result_rate": round(float(empty_count) / len(recalls), 4) if recalls else 0.0,
        "total_requests": total_requests,
        "semantic": {
            "model": _embedder.model_name,
            "enabled": _embedder.enabled,
            "hit_rate": round(float(SEMANTIC_HIT_COUNT) / max(1, SEMANTIC_CALL_COUNT), 4),
            "load_fail_count": SEMANTIC_LOAD_FAIL,
            "avg_latency_ms": round(float(SEMANTIC_TOTAL_MS) / max(1, SEMANTIC_CALL_COUNT), 2),
        },
        "ab": {
            "experiment": AB_RUNTIME.get("experiment", "off"),
            "traffic": AB_RUNTIME.get("traffic", 0.0),
            "buckets": AB_RUNTIME.get("buckets", AB_BUCKETS),
            "project_types": sorted(list(AB_RUNTIME.get("project_types") or {AB_SEGMENT_ALLOW_ALL})),
            "user_segments": sorted(list(AB_RUNTIME.get("user_segments") or {AB_SEGMENT_ALLOW_ALL})),
        },
        "feedback": {
            "total_events": total_feedback,
            "invitation_conversion_rate": round(float(invite_accepted) / invite_count, 4) if invite_count > 0 else 0.0,
            "invitation_decline_rate": round(float(invite_declined) / invite_count, 4) if invite_count > 0 else 0.0,
            "application_approval_rate": round(float(app_approved) / (app_approved + app_rejected), 4) if (app_approved + app_rejected) > 0 else 0.0,
            "application_rejection_rate": round(float(app_rejected) / (app_approved + app_rejected), 4) if (app_approved + app_rejected) > 0 else 0.0,
            "event_breakdown": feedback_events,
        },
    }


class UserMatchRequest(BaseModel):
    user_id: int = Field(alias="userId")  # 支持Java的驼峰命名
    user: Dict[str, Any]
    projects: List[Dict[str, Any]]
    
    class Config:
        # 允许通过别名填充字段
        populate_by_name = True
        # 允许额外字段，提高兼容性
        extra = "allow"


class UserMatchResult(BaseModel):
    projectId: int  # 直接使用驼峰命名
    matchScore: float  # 直接使用驼峰命名
    breakdown: Dict[str, float] = Field(default_factory=dict)
    timeExplanation: str = ""


class UserTeamMatchRequest(BaseModel):
    user_id: int = Field(alias="userId")
    user: Dict[str, Any]
    teams: List[Dict[str, Any]]

    class Config:
        populate_by_name = True
        extra = "allow"


class UserTeamMatchResult(BaseModel):
    teamId: int
    matchScore: float
    breakdown: Dict[str, float] = Field(default_factory=dict)
    matchReason: str = ""


@app.post("/api/matching/user-to-projects", response_model=List[UserMatchResult])
def match_user_to_projects(req: UserMatchRequest) -> List[UserMatchResult]:
    """成员找项目：为用户匹配合适的项目"""
    try:
        _log_debug(f"收到用户匹配请求 - user_id: {req.user_id}")
        _log_debug(f"user字段类型: {type(req.user)}")
        _log_debug(f"projects数量: {len(req.projects) if req.projects else 0}")
        
        user_data = req.user or {}
        
        # 构建候选人数据结构（复用现有逻辑）
        candidate = {
            "user": user_data,
            "skills": user_data.get("skills", []),
            "availability": user_data.get("availability", []),
            "credit": user_data.get("credit", {}),
            "collaboration_history": user_data.get("collaboration_history", []),
        }
        
        _log_debug(f"用户技能数: {len(candidate['skills'])}")
        _log_debug(f"用户可用时间数: {len(candidate['availability'])}")
    except Exception as e:
        _log_error(f"解析请求数据失败: {e}")
        logger.exception("match_user_to_projects 请求解析异常")
        raise

    user_segment = _extract_user_segment(user_data)
    ltr_state = _load_ltr_state()

    results: List[UserMatchResult] = []
    for project in req.projects:
        pid = project.get("id")
        if pid is None:
            continue

        # AB：按项目+用户稳定分桶，支持灰度（可按项目类型与用户分层过滤）
        ab_variant = _choose_ab_variant(pid, req.user_id, project.get("project_type"), user_segment)
        project_weights = _effective_weights_by_variant(_get_effective_weights(project.get("project_type")), ab_variant)
        
        # 计算匹配度（复用现有的breakdown计算逻辑）
        time_analysis = _time_overlap_analysis(project, candidate)
        breakdown = _compute_breakdown(project, candidate)
        breakdown["time"] = _safe_float(time_analysis.get("score"), breakdown.get("time", 0.5))
        base_score = _final_score(breakdown, project_weights)
        score, _ = _blend_ltr_score(base_score, breakdown, ltr_state)
        
        results.append(
            UserMatchResult(
                projectId=int(pid),  # 使用驼峰命名
                matchScore=float(score),  # 使用驼峰命名
                breakdown={k: float(v) for k, v in breakdown.items()},
                timeExplanation=str(time_analysis.get("explanation") or ""),
            )
        )

    results.sort(key=lambda r: r.matchScore, reverse=True)  # 使用matchScore排序
    return results


@app.post("/api/matching/user-to-teams", response_model=List[UserTeamMatchResult])
def match_user_to_teams(req: UserTeamMatchRequest) -> List[UserTeamMatchResult]:
    """成员找团队：为用户匹配长期团队（复用现有匹配维度）"""
    user_data = req.user or {}
    candidate = {
        "user": user_data,
        "skills": user_data.get("skills", []),
        "availability": user_data.get("availability", []),
        "credit": user_data.get("credit", {}),
        "collaboration_history": user_data.get("collaboration_history", []),
    }

    user_segment = _extract_user_segment(user_data)
    ltr_state = _load_ltr_state()

    results: List[UserTeamMatchResult] = []
    for team in req.teams or []:
        tid = team.get("id")
        if tid is None:
            continue

        ab_variant = _choose_ab_variant(tid, req.user_id, team.get("project_type"), user_segment)
        weights = _effective_weights_by_variant(_get_effective_weights(team.get("project_type")), ab_variant)

        time_analysis = _time_overlap_analysis(team, candidate)
        breakdown = _compute_breakdown(team, candidate)
        breakdown["time"] = _safe_float(time_analysis.get("score"), breakdown.get("time", 0.5))

        base_score = _final_score(breakdown, weights)
        score, _ = _blend_ltr_score(base_score, breakdown, ltr_state)

        reason = "综合评估推荐"
        if _safe_float(breakdown.get("skill"), 0.0) > 0.7:
            reason = "技能匹配度高"
        elif _safe_float(breakdown.get("goal"), 0.0) > 0.65:
            reason = "团队方向契合"

        results.append(
            UserTeamMatchResult(
                teamId=int(tid),
                matchScore=float(score),
                breakdown={k: float(v) for k, v in breakdown.items()},
                matchReason=reason,
            )
        )

    results.sort(key=lambda r: r.matchScore, reverse=True)
    return results


class TeamToUsersRequest(BaseModel):
    """团队找成员：Team -> Users"""
    team_id: int = Field(alias="teamId")
    team: Dict[str, Any]
    candidates: List[Dict[str, Any]]

    class Config:
        populate_by_name = True
        extra = "allow"


@app.post("/api/matching/team-to-users", response_model=List[MatchResult])
def match_team_to_users(req: TeamToUsersRequest) -> List[MatchResult]:
    """团队找成员：为团队匹配候选人（复用项目招人逻辑与9维度打分）"""
    started_at = time.perf_counter()
    team = req.team or {}

    _log_info("=" * 60)
    _log_info(f"开始团队找成员匹配 - team_id: {req.team_id}")
    _log_info(f"团队名称: {team.get('title') or team.get('name')}")
    _log_info(f"候选人数量: {len(req.candidates) if req.candidates else 0}")
    _log_info("=" * 60)

    ab_variant = _choose_ab_variant(req.team_id, "batch", team.get("project_type"), AB_SEGMENT_NOT_SET)
    weights = _effective_weights_by_variant(_get_effective_weights(team.get("project_type")), ab_variant)
    ltr_state = _load_ltr_state()

    _log_info(f"[team-to-users] 权重配置: {weights}, AB={ab_variant}, LTR={ltr_state.get('enabled')}")

    results: List[MatchResult] = []
    for idx, c in enumerate(req.candidates or []):
        user = c.get("user") or {}
        uid = user.get("id")
        if uid is None:
            continue

        _log_info(
            f"[team-to-users] 处理候选人 {idx + 1}/{len(req.candidates or [])} - "
            f"用户ID: {uid}, 用户名: {user.get('username')}"
        )

        time_analysis = _time_overlap_analysis(team, c)
        breakdown = _compute_breakdown(team, c)
        breakdown["time"] = _safe_float(time_analysis.get("score"), breakdown.get("time", 0.5))
        base_score = _final_score(breakdown, weights)
        score, ltr_prob = _blend_ltr_score(base_score, breakdown, ltr_state)

        _log_info("[team-to-users] 各维度得分:")
        for dim, val in breakdown.items():
            _log_info(f"  - {dim}: {val:.3f} (权重: {weights.get(dim, 0):.2f})")
        _log_info(f"[team-to-users] 最终得分: {score:.3f} (base={base_score:.3f}, ltr={ltr_prob:.3f})")

        results.append(
            MatchResult(
                user_id=int(uid),
                score=float(score),
                breakdown={k: float(v) for k, v in breakdown.items()},
                time_explanation=str(time_analysis.get("explanation") or ""),
            )
        )

    results.sort(key=lambda r: r.score, reverse=True)

    duration_ms = (time.perf_counter() - started_at) * 1000.0
    _record_request_metrics(duration_ms, len(results))
    _log_info(f"[team-to-users] 匹配完成，返回 {len(results)} 个结果，耗时 {duration_ms:.2f}ms")
    _log_info("=" * 60)

    return results


class TeammateRecommendRequest(BaseModel):
    """智能组队推荐：为用户推荐可组队的同学"""
    user_id: int = Field(alias="userId")
    user: Dict[str, Any]
    candidates: List[Dict[str, Any]]

    class Config:
        populate_by_name = True
        extra = "allow"


@app.post("/api/matching/recommend-teammates", response_model=List[MatchResult])
def recommend_teammates(req: TeammateRecommendRequest) -> List[MatchResult]:
    """智能组队推荐：基于兴趣、时间重叠与候选人质量为用户推荐队友"""
    started_at = time.perf_counter()
    center_user = req.user or {}

    _log_info("=" * 60)
    _log_info(f"[recommend-teammates] 开始为用户 {req.user_id} 推荐队友")
    _log_info(f"[recommend-teammates] 候选人数: {len(req.candidates) if req.candidates else 0}")
    _log_info("=" * 60)

    # 构造“虚拟项目”用于时间匹配与目标相似度计算
    center_availability = center_user.get("availability") or []
    pseudo_project = {
        "title": "",
        "description": _extract_user_text({"user": center_user.get("user") or {}}),
        "project_type": "TEAMMATE_RECOMMEND",
        "weekly_hours": 6.0,
        "creator_id": req.user_id,
        "time_slots": center_availability,
        "skill_requirements": [],  # skill 维度在本场景中主要依赖候选人自身质量
    }

    ab_variant = _choose_ab_variant(f"teammates:{req.user_id}", "batch", pseudo_project.get("project_type"), AB_SEGMENT_NOT_SET)
    base_weights = _effective_weights_by_variant(_get_effective_weights(pseudo_project.get("project_type")), ab_variant)
    weights = _effective_weights_for_request(pseudo_project, base_weights)
    ltr_state = _load_ltr_state()

    _log_info(f"[recommend-teammates] 权重配置: {weights}, AB={ab_variant}, LTR={ltr_state.get('enabled')}")

    results: List[MatchResult] = []
    for idx, cand in enumerate(req.candidates or []):
        user = cand.get("user") or {}
        uid = user.get("id")
        if uid is None:
            continue

        _log_info(
            f"[recommend-teammates] 处理候选人 {idx + 1}/{len(req.candidates or [])} - "
            f"用户ID: {uid}, 用户名: {user.get('username')}"
        )

        # 时间维度：使用中心用户可用时间与候选人可用时间重叠
        time_analysis = _time_overlap_analysis(pseudo_project, cand)

        # 其余维度复用 _compute_breakdown，但 goal/skill 等更多体现兴趣方向与候选人质量
        breakdown = _compute_breakdown(pseudo_project, cand)
        breakdown["time"] = _safe_float(time_analysis.get("score"), breakdown.get("time", 0.5))

        base_score = _final_score(breakdown, weights)
        score, ltr_prob = _blend_ltr_score(base_score, breakdown, ltr_state)
        confidence, confidence_level, risk_level = _compute_confidence_and_risk(pseudo_project, cand, breakdown)

        _log_info("[recommend-teammates] 各维度得分:")
        for dim, val in breakdown.items():
            _log_info(f"  - {dim}: {val:.3f} (权重: {weights.get(dim, 0):.2f})")
        _log_info(f"[recommend-teammates] 最终得分: {score:.3f} (base={base_score:.3f}, ltr={ltr_prob:.3f})")

        results.append(
            MatchResult(
                user_id=int(uid),
                score=float(score),
                breakdown={k: float(v) for k, v in breakdown.items()},
                time_explanation=str(time_analysis.get("explanation") or ""),
                confidence=float(confidence),
                confidenceLevel=str(confidence_level),
                riskLevel=str(risk_level),
            )
        )

    results.sort(key=lambda r: r.score, reverse=True)

    duration_ms = (time.perf_counter() - started_at) * 1000.0
    _record_request_metrics(duration_ms, len(results))
    _log_info(f"[recommend-teammates] 匹配完成，返回 {len(results)} 个结果，耗时 {duration_ms:.2f}ms")
    _log_info("=" * 60)

    return results
