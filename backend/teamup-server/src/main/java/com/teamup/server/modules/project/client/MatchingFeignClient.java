package com.teamup.server.modules.project.client;

import com.teamup.server.modules.project.dto.matching.MatchFeedbackRequest;
import com.teamup.server.modules.project.dto.matching.MatchRequest;
import com.teamup.server.modules.project.dto.matching.MatchResult;
import com.teamup.server.modules.project.dto.matching.ProjectMatchRequest;
import com.teamup.server.modules.project.dto.matching.TeamMatchRequest;
import com.teamup.server.modules.project.dto.matching.TeammateRecommendRequest;
import com.teamup.server.modules.project.dto.matching.UserMatchRequest;
import com.teamup.server.modules.project.dto.matching.UserMatchResult;
import com.teamup.server.modules.project.dto.matching.UserTeamMatchRequest;
import com.teamup.server.modules.project.dto.matching.UserTeamMatchResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "matching-service", url = "${matching-service.url}")
public interface MatchingFeignClient {

    /**
     * 项目招募成员：为项目匹配候选人
     */
    @PostMapping("/api/matching/calculate")
    List<MatchResult> calculateMatch(@RequestBody MatchRequest request);
    
    /**
     * 团队找成员：为团队匹配候选人
     */
    @PostMapping("/api/matching/team-to-users")
    List<MatchResult> matchTeamToUsers(@RequestBody TeamMatchRequest request);

    /**
     * 成员找项目：为用户匹配合适的项目
     */
    @PostMapping("/api/matching/user-to-projects")
    List<UserMatchResult> matchUserToProjects(@RequestBody UserMatchRequest request);

    /**
     * 匹配反馈闭环：邀请/通过/拒绝结果回流
     */
    @PostMapping("/api/matching/feedback")
    Object sendMatchFeedback(@RequestBody MatchFeedbackRequest request);

    /**
     * 成员找团队：为用户匹配长期团队
     */
    @PostMapping("/api/matching/user-to-teams")
    List<UserTeamMatchResult> matchUserToTeams(@RequestBody UserTeamMatchRequest request);

    /**
     * 智能组队推荐：为用户推荐可组队的同学
     * 
     * @deprecated 独立的智能组队推荐页面已移除，此接口保留供未来在项目流程中使用
     */
    @Deprecated
    @PostMapping("/api/matching/recommend-teammates")
    List<MatchResult> recommendTeammates(@RequestBody TeammateRecommendRequest request);
    
    /**
     * 为项目推荐队友：基于项目需求推荐合适的队友
     * 使用 calculate 接口（项目招募成员）
     */
    @PostMapping("/api/matching/calculate")
    List<MatchResult> matchProjectCandidates(@RequestBody ProjectMatchRequest request);
}
