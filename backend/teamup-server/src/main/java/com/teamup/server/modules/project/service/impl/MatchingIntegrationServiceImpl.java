package com.teamup.server.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.project.client.MatchingFeignClient;
import com.teamup.server.modules.project.dto.matching.MatchRequest;
import com.teamup.server.modules.project.dto.matching.MatchResult;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.project.service.MatchingIntegrationService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserCredit;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.entity.UserSkill;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingIntegrationServiceImpl implements MatchingIntegrationService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final ProfileService profileService;
    private final MatchingFeignClient matchingClient;

    @Override
    public List<MatchResult> matchCandidates(Long projectId) {
        // 1. 获取项目信息
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("Project not found");
        }

        // 2. 构建项目数据 Map
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("id", project.getId());
        projectData.put("title", project.getTitle());
        projectData.put("description", project.getDescription());
        projectData.put("project_type", project.getProjectType());
        projectData.put("weekly_hours", project.getWeeklyHours());
        projectData.put("creator_id", project.getCreatorId());
        // TODO: Fetch skill requirements from DB
        projectData.put("skill_requirements", new ArrayList<>()); 

        // 3. 获取候选人列表 (简化：获取最近活跃的50个用户)
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getStatus, "ACTIVE")
                   .last("LIMIT 50");
        List<User> users = userMapper.selectList(userWrapper);

        List<Map<String, Object>> candidates = users.stream()
                .filter(u -> !u.getId().equals(project.getCreatorId())) // 排除创建者自己
                .map(this::buildCandidateData)
                .collect(Collectors.toList());

        // 4. 调用 Python 服务
        MatchRequest request = new MatchRequest();
        request.setProject_id(projectId);
        request.setProject(projectData);
        request.setCandidates(candidates);

        try {
            log.info("Calling matching service via Feign");
            return matchingClient.calculateMatch(request);
        } catch (Exception e) {
            log.error("Failed to call matching service", e);
            return new ArrayList<>();
        }
    }

    private Map<String, Object> buildCandidateData(User user) {
        Map<String, Object> data = new HashMap<>();
        
        // User basic info
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        
        UserProfile profile = profileService.getProfileByUserId(user.getId());
        if (profile != null) {
            userInfo.put("bio", profile.getBio());
            userInfo.put("project_experience", profile.getProjectExperience());
            userInfo.put("department", profile.getDepartment());
            // Interests are not in profile entity in this version, assuming empty for now
            userInfo.put("interests", new ArrayList<>()); 
        }
        data.put("user", userInfo);

        // Skills
        List<UserSkill> skills = profileService.getUserSkills(user.getId());
        List<Map<String, Object>> skillList = skills.stream().map(s -> {
            Map<String, Object> sm = new HashMap<>();
            sm.put("skill_name", s.getSkillName());
            sm.put("proficiency_level", s.getProficiencyLevel());
            return sm;
        }).collect(Collectors.toList());
        data.put("skills", skillList);

        // Availability (Mock for now)
        data.put("availability", new ArrayList<>());

        // Credit
        UserCredit credit = profileService.getUserCredit(user.getId());
        Map<String, Object> creditMap = new HashMap<>();
        if (credit != null) {
            creditMap.put("credit_level", credit.getCreditLevel());
            creditMap.put("total_credit", credit.getTotalCredit());
        }
        data.put("credit", creditMap);

        // History (Mock)
        data.put("collaboration_history", new ArrayList<>());

        return data;
    }
}
