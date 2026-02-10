package com.teamup.server.modules.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.search.dto.SearchItemDTO;
import com.teamup.server.modules.search.dto.SearchResultDTO;
import com.teamup.server.modules.search.service.SearchService;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;

    @Override
    public SearchResultDTO globalSearch(String keyword, Long userId) {
        SearchResultDTO result = new SearchResultDTO();
        result.setProjects(new ArrayList<>());
        result.setUsers(new ArrayList<>());
        result.setTeams(new ArrayList<>());

        if (keyword == null || keyword.trim().isEmpty()) {
            result.setTotalCount(0);
            return result;
        }

        String searchKeyword = "%" + keyword.trim() + "%";

        // 搜索项目
        LambdaQueryWrapper<Project> projectWrapper = new LambdaQueryWrapper<>();
        projectWrapper.like(Project::getTitle, searchKeyword)
                     .or()
                     .like(Project::getDescription, searchKeyword)
                     .last("LIMIT 10");
        
        List<Project> projects = projectMapper.selectList(projectWrapper);
        for (Project project : projects) {
            SearchItemDTO item = new SearchItemDTO();
            item.setType("projects");
            item.setId(project.getId());
            item.setTitle(project.getTitle());
            item.setDescription(project.getDescription());
            item.setIcon("🚀");
            result.getProjects().add(item);
        }

        // 搜索用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.like(User::getUsername, searchKeyword)
                   .or()
                   .like(User::getStudentId, searchKeyword)
                   .last("LIMIT 10");
        
        List<User> users = userMapper.selectList(userWrapper);
        for (User user : users) {
            SearchItemDTO item = new SearchItemDTO();
            item.setType("users");
            item.setId(user.getId());
            item.setTitle(user.getUsername());
            item.setDescription("学号: " + user.getStudentId());
            item.setIcon("👤");
            result.getUsers().add(item);
        }

        // TODO: 搜索团队（待实现）

        result.setTotalCount(result.getProjects().size() + result.getUsers().size() + result.getTeams().size());

        log.info("全局搜索关键词: {}, 找到 {} 个结果", keyword, result.getTotalCount());

        return result;
    }
}
