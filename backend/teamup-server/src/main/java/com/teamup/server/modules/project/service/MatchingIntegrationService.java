package com.teamup.server.modules.project.service;

import com.teamup.server.modules.project.dto.matching.MatchResult;
import java.util.List;

public interface MatchingIntegrationService {
    List<MatchResult> matchCandidates(Long projectId);
}
