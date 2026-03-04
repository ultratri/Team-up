package com.teamup.server.modules.project.service;

import com.teamup.server.modules.project.dto.matching.MatchResult;
import com.teamup.server.modules.project.dto.matching.ProjectWithMatchScore;

import java.util.List;
import java.util.Map;

public interface MatchingIntegrationService {
    /**
     * 项目招募成员：为项目匹配候选人
     */
    List<MatchResult> matchCandidates(Long projectId);

    /**
     * 团队找成员：为团队匹配候选人
     */
    List<MatchResult> matchTeamCandidates(Long teamId, String keyword);

    /**
     * 智能组队推荐：为当前用户推荐可组队的同学
     */
    List<MatchResult> recommendTeammates(Long userId, int limit);

    /**
     * 项目内任务匹配：为任务推荐负责人（仅在所属团队成员内匹配）
     */
    List<MatchResult> matchTaskAssignees(Long taskId, int limit);

    /**
     * 成员找项目：为用户匹配合适的项目
     */
    List<ProjectWithMatchScore> matchProjectsForUser(Long userId, int page, int size);

    /**
     * 结果反馈闭环：邀请/通过/拒绝回流
     */
    void reportMatchFeedback(Long projectId, Long userId, String projectType, String event, String source, Long eventTime, Map<String, Double> breakdown);


    /**
     * 为项目推荐队友：基于项目需求推荐合适的队友
     */
    List<MatchResult> recommendTeammatesForProject(Long userId, String projectTitle, String projectType,
                                                   List<Map<String, Object>> skillRequirements,
                                                   List<Map<String, Object>> timeSlots,
                                                   Integer weeklyHours, Integer expectedDuration);

}
