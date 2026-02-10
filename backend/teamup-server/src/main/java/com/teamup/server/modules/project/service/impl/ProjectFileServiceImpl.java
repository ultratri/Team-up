package com.teamup.server.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.file.service.FileStorageService;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.entity.ProjectFile;
import com.teamup.server.modules.project.mapper.ProjectFileMapper;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.project.service.ProjectFileService;
import com.teamup.server.modules.project.vo.ProjectFileVO;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.entity.UserProfile;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目文件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectFileMapper fileMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final FileStorageService fileStorageService;

    @Override
    public Page<ProjectFileVO> getProjectFiles(Long projectId, String category, int page, int size) {
        // 验证项目是否存在
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        // 查询文件列表
        Page<ProjectFile> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ProjectFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectFile::getProjectId, projectId);
        
        
        wrapper.orderByDesc(ProjectFile::getUploadedAt);
        Page<ProjectFile> filePage = fileMapper.selectPage(pageParam, wrapper);

        // 转换为VO
        Page<ProjectFileVO> voPage = new Page<>(page, size, filePage.getTotal());
        List<ProjectFileVO> voList = new ArrayList<>();

        if (!filePage.getRecords().isEmpty()) {
            // 批量获取用户信息
            List<Long> userIds = filePage.getRecords().stream()
                    .map(ProjectFile::getUploaderId)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, User> userMap = new HashMap<>();
            Map<Long, UserProfile> profileMap = new HashMap<>();

            if (!userIds.isEmpty()) {
                List<User> users = userMapper.selectBatchIds(userIds);
                userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

                LambdaQueryWrapper<UserProfile> profileWrapper = new LambdaQueryWrapper<>();
                profileWrapper.in(UserProfile::getUserId, userIds);
                List<UserProfile> profiles = profileMapper.selectList(profileWrapper);
                profileMap = profiles.stream().collect(Collectors.toMap(UserProfile::getUserId, p -> p));
            }

            // 转换为VO
            for (ProjectFile file : filePage.getRecords()) {
                ProjectFileVO vo = toVO(file, userMap, profileMap);
                voList.add(vo);
            }
        }

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectFileVO uploadFile(Long projectId, Long userId, MultipartFile file, String category, String description) {
        // 验证项目是否存在
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        // 上传文件
        String filePath = fileStorageService.uploadFile(file, "project");

        // 判断文件类型
        String fileType = determineFileType(file.getContentType(), file.getOriginalFilename());

        // 保存文件记录
        ProjectFile projectFile = new ProjectFile();
        projectFile.setProjectId(projectId);
        projectFile.setUploaderId(userId);
        projectFile.setFileName(file.getOriginalFilename());
        projectFile.setFilePath(filePath);
        projectFile.setFileType(fileType);
        projectFile.setFileSize(file.getSize());
        projectFile.setUploadedAt(LocalDateTime.now());

        fileMapper.insert(projectFile);

        // 转换为VO
        User user = userMapper.selectById(userId);
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        UserProfile profile = profileMapper.selectOne(wrapper);

        Map<Long, User> userMap = new HashMap<>();
        Map<Long, UserProfile> profileMap = new HashMap<>();
        userMap.put(userId, user);
        if (profile != null) {
            profileMap.put(userId, profile);
        }

        return toVO(projectFile, userMap, profileMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId, Long userId) {
        ProjectFile file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 验证权限：只有上传者或项目创建者可以删除
        Project project = projectMapper.selectById(file.getProjectId());
        if (!file.getUploaderId().equals(userId) && !project.getCreatorId().equals(userId)) {
            throw new BusinessException("无权删除此文件");
        }

        // 删除文件
        try {
            fileStorageService.deleteFile(file.getFilePath());
        } catch (Exception e) {
            log.warn("删除物理文件失败: {}", file.getFilePath(), e);
        }

        // 删除记录
        fileMapper.deleteById(fileId);
    }

    @Override
    public List<String> getFileCategories(Long projectId) {
        // 当前数据库结构没有 category 字段，先返回空列表避免报错
        return List.of();
    }

    private ProjectFileVO toVO(ProjectFile file, Map<Long, User> userMap, Map<Long, UserProfile> profileMap) {
        ProjectFileVO vo = new ProjectFileVO();
        vo.setId(file.getId());
        vo.setProjectId(file.getProjectId());
        vo.setUploaderId(file.getUploaderId());
        vo.setFileName(file.getFileName());
        vo.setFilePath(file.getFilePath());
        vo.setFileType(file.getFileType());
        vo.setFileSize(file.getFileSize());
        vo.setUploadedAt(file.getUploadedAt());

        // 填充用户信息
        User user = userMap.get(file.getUploaderId());
        if (user != null) {
            vo.setUploaderName(user.getUsername());
            
            UserProfile profile = profileMap.get(file.getUploaderId());
            if (profile != null && profile.getAvatarUrl() != null) {
                vo.setUploaderAvatar(profile.getAvatarUrl());
            }
        }

        return vo;
    }

    private String determineFileType(String mimeType, String fileName) {
        if (mimeType == null && fileName == null) {
            return "OTHER";
        }

        String lowerMime = mimeType != null ? mimeType.toLowerCase() : "";
        String lowerName = fileName != null ? fileName.toLowerCase() : "";

        if (lowerMime.startsWith("image/") || lowerName.matches(".*\\.(jpg|jpeg|png|gif|webp|svg)$")) {
            return "IMAGE";
        }

        if (lowerMime.contains("pdf") || lowerMime.contains("word") || lowerMime.contains("excel") ||
            lowerMime.contains("powerpoint") || lowerName.matches(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx)$")) {
            return "DOCUMENT";
        }

        if (lowerMime.contains("text") || lowerName.matches(".*\\.(java|js|ts|py|cpp|c|h|html|css|json|xml|md)$")) {
            return "CODE";
        }

        return "OTHER";
    }
}
