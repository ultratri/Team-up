package com.teamup.server.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.project.entity.ProjectComment;
import com.teamup.server.modules.project.mapper.ProjectCommentMapper;
import com.teamup.server.modules.project.service.ProjectCommentService;
import com.teamup.server.modules.project.vo.ProjectCommentVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectCommentServiceImpl implements ProjectCommentService {

    private final ProjectCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public Page<ProjectCommentVO> getProjectComments(Long projectId, int page, int size) {
        Page<ProjectComment> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<ProjectComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectComment::getProjectId, projectId)
                .isNull(ProjectComment::getParentId)
                .orderByDesc(ProjectComment::getCreatedAt);

        Page<ProjectComment> commentPage = commentMapper.selectPage(pageParam, wrapper);

        if (commentPage.getRecords().isEmpty()) {
            return new Page<>(page, size);
        }

        List<Long> commentIds = commentPage.getRecords().stream()
                .map(ProjectComment::getId)
                .toList();

        // 查询子评论
        LambdaQueryWrapper<ProjectComment> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.in(ProjectComment::getParentId, commentIds)
                .orderByAsc(ProjectComment::getCreatedAt);

        List<ProjectComment> replies = commentMapper.selectList(replyWrapper);

        // 收集所有涉及的用户
        Set<Long> userIds = new HashSet<>();
        commentPage.getRecords().forEach(c -> userIds.add(c.getUserId()));
        replies.forEach(c -> userIds.add(c.getUserId()));

        Map<Long, User> userMap = Collections.emptyMap();
        Map<Long, UserProfile> profileMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            LambdaQueryWrapper<UserProfile> profileWrapper = new LambdaQueryWrapper<>();
            profileWrapper.in(UserProfile::getUserId, userIds);
            profileMap = userProfileMapper.selectList(profileWrapper).stream()
                    .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
        }

        // 构建 VO
        Map<Long, ProjectCommentVO> voMap = new HashMap<>();
        List<ProjectCommentVO> topLevel = new ArrayList<>();

        for (ProjectComment comment : commentPage.getRecords()) {
            ProjectCommentVO vo = toVO(comment, userMap, profileMap);
            vo.setReplies(new ArrayList<>());
            voMap.put(comment.getId(), vo);
            topLevel.add(vo);
        }

        for (ProjectComment reply : replies) {
            ProjectCommentVO vo = toVO(reply, userMap, profileMap);
            vo.setReplies(Collections.emptyList());
            ProjectCommentVO parent = voMap.get(reply.getParentId());
            if (parent != null) {
                parent.getReplies().add(vo);
            }
        }

        Page<ProjectCommentVO> result = new Page<>(page, size, commentPage.getTotal());
        result.setRecords(topLevel);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectCommentVO addComment(Long projectId, Long userId, Long parentId, Long replyToUserId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("评论内容不能为空");
        }

        ProjectComment comment = new ProjectComment();
        comment.setProjectId(projectId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        commentMapper.insert(comment);

        User user = userMapper.selectById(userId);
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );

        Map<Long, User> userMap = new HashMap<>();
        Map<Long, UserProfile> profileMap = new HashMap<>();
        if (user != null) {
            userMap.put(userId, user);
        }
        if (profile != null) {
            profileMap.put(userId, profile);
        }

        // 如果有回复目标用户，补充其信息
        if (replyToUserId != null) {
            User replyUser = userMapper.selectById(replyToUserId);
            if (replyUser != null) {
                userMap.put(replyToUserId, replyUser);
            }
        }

        ProjectCommentVO vo = toVO(comment, userMap, profileMap);
        vo.setReplies(Collections.emptyList());
        return vo;
    }

    private ProjectCommentVO toVO(ProjectComment comment,
                                  Map<Long, User> userMap,
                                  Map<Long, UserProfile> profileMap) {
        ProjectCommentVO vo = new ProjectCommentVO();
        vo.setId(comment.getId());
        vo.setProjectId(comment.getProjectId());
        vo.setParentId(comment.getParentId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());

        User user = userMap.get(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
        }
        UserProfile profile = profileMap.get(comment.getUserId());
        if (profile != null) {
            vo.setAvatar(profile.getAvatarUrl());
        }

        return vo;
    }
}

