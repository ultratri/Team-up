package com.teamup.server.modules.file.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.activity.entity.TeamActivity;
import com.teamup.server.modules.activity.mapper.TeamActivityMapper;
import com.teamup.server.modules.activity.vo.ActivityVO;
import com.teamup.server.modules.activity.service.ActivityService;
import com.teamup.server.modules.file.entity.FileEntity;
import com.teamup.server.modules.file.mapper.FileMapper;
import com.teamup.server.modules.file.service.FileService;
import com.teamup.server.modules.file.vo.FileVO;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.user.security.UserContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FileController {
    
    private final FileService fileService;
    private final TeamMemberMapper teamMemberMapper;
    private final FileMapper fileMapper;
    private final ActivityService activityService;
    private final TeamActivityMapper teamActivityMapper;
    
    /**
     * 获取团队文件列表
     * 
     * @param teamId 团队ID
     * @param folderId 文件夹ID（可选，null表示根目录）
     * @return 文件列表
     */
    @GetMapping("/teams/{teamId}/files")
    @PreAuthorize("isAuthenticated()")
    public Result<List<FileVO>> getTeamFiles(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long folderId) {
        
        // 验证用户是否为团队成员
        Long currentUserId = UserContext.getCurrentUserId();
        QueryWrapper<TeamMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", teamId)
                   .eq("user_id", currentUserId);
        
        TeamMember member = teamMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            return Result.error(403, "无权限访问该团队文件");
        }
        
        // 获取文件列表
        List<FileVO> files = fileService.getFileList(teamId, folderId);
        return Result.success(files);
    }
    
    /**
     * 下载文件
     * 
     * @param fileId 文件ID
     * @param response HTTP响应对象
     */
    @GetMapping("/files/{fileId}/download")
    @PreAuthorize("isAuthenticated()")
    public void downloadFile(
            @PathVariable Long fileId,
            HttpServletResponse response) {
        
        // 获取当前用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 查询文件信息
        FileEntity file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        
        // 验证用户是否为团队成员
        QueryWrapper<TeamMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", file.getTeamId())
                   .eq("user_id", currentUserId);
        
        TeamMember member = teamMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            throw new BusinessException("无权限下载该文件");
        }
        
        // 执行文件下载
        fileService.downloadFile(fileId, response);
        
        // 记录下载活动
        activityService.trackFileActivity(
            file.getTeamId(),
            currentUserId,
            "download",
            "下载了文件「" + file.getFileName() + "」",
            fileId
        );
    }

    /**
     * 预览文件（inline）：图片/PDF/文本等在浏览器中直接打开（类似 GitHub）
     */
    @GetMapping("/files/{fileId}/preview")
    @PreAuthorize("isAuthenticated()")
    public void previewFile(
            @PathVariable Long fileId,
            HttpServletResponse response) {

        Long currentUserId = UserContext.getCurrentUserId();

        FileEntity file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 验证用户是否为团队成员
        QueryWrapper<TeamMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", file.getTeamId())
                .eq("user_id", currentUserId);

        TeamMember member = teamMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            throw new BusinessException("无权限预览该文件");
        }

        fileService.previewFile(fileId, response);

        // 记录预览活动
        activityService.trackFileActivity(
                file.getTeamId(),
                currentUserId,
                "preview",
                "预览了文件「" + file.getFileName() + "」",
                fileId
        );
    }
    
    /**
     * 删除文件
     * 
     * @param fileId 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> deleteFile(@PathVariable Long fileId) {
        
        // 获取当前用户ID
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 查询文件信息
        FileEntity file = fileMapper.selectById(fileId);
        if (file == null) {
            return Result.error(404, "文件不存在");
        }
        
        // 验证用户是否为团队成员
        QueryWrapper<TeamMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", file.getTeamId())
                   .eq("user_id", currentUserId);
        
        TeamMember member = teamMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            return Result.error(403, "无权限访问该团队");
        }
        
        // 执行文件删除（权限验证在service层进行）
        try {
            fileService.deleteFile(fileId, currentUserId);
            return Result.success();
        } catch (BusinessException e) {
            // 处理业务异常（如权限不足、文件不存在等）
            String message = e.getMessage();
            if (message.contains("无权限")) {
                return Result.error(403, message);
            } else if (message.contains("不存在")) {
                return Result.error(404, message);
            } else {
                return Result.error(400, message);
            }
        } catch (Exception e) {
            // 处理其他异常
            log.error("文件删除失败: fileId={}", fileId, e);
            return Result.error(500, "文件删除失败");
        }
    }
    
    /**
     * 上传团队文件
     */
    @PostMapping("/files/upload")
    @PreAuthorize("isAuthenticated()")
    public Result<FileVO> uploadTeamFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long teamId,
            @RequestParam(required = false) Long folderId) {
        
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证用户是否为团队成员
        QueryWrapper<TeamMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", teamId)
                   .eq("user_id", currentUserId);
        
        TeamMember member = teamMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            return Result.error(403, "无权限上传文件到该团队");
        }
        
        try {
            FileVO uploadedFile = fileService.uploadTeamFile(file, teamId, folderId, currentUserId);
            
            // 记录上传活动
            activityService.trackFileActivity(
                teamId,
                currentUserId,
                "upload",
                "上传了文件「" + file.getOriginalFilename() + "」",
                uploadedFile.getId()
            );
            
            return Result.success(uploadedFile);
        } catch (Exception e) {
            log.error("文件上传失败: teamId={}, fileName={}", teamId, file.getOriginalFilename(), e);
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建文件夹
     */
    @PostMapping("/teams/{teamId}/folders")
    @PreAuthorize("isAuthenticated()")
    public Result<FileVO> createFolder(
            @PathVariable Long teamId,
            @RequestParam String folderName,
            @RequestParam(required = false) Long parentFolderId) {
        
        Long currentUserId = UserContext.getCurrentUserId();
        
        // 验证用户是否为团队成员
        QueryWrapper<TeamMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", teamId)
                   .eq("user_id", currentUserId);
        
        TeamMember member = teamMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            return Result.error(403, "无权限在该团队创建文件夹");
        }
        
        try {
            FileVO folder = fileService.createFolder(teamId, folderName, parentFolderId, currentUserId);
            return Result.success(folder);
        } catch (Exception e) {
            log.error("创建文件夹失败: teamId={}, folderName={}", teamId, folderName, e);
            return Result.error(500, "创建文件夹失败: " + e.getMessage());
        }
    }

    /**
     * 获取某个文件的活动历史（类似 git log 的最小实现：上传/下载/删除等）
     */
    @GetMapping("/files/{fileId}/activities")
    @PreAuthorize("isAuthenticated()")
    public Result<List<ActivityVO>> getFileActivities(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "50") Integer limit) {

        Long currentUserId = UserContext.getCurrentUserId();

        FileEntity file = fileMapper.selectById(fileId);
        if (file == null) {
            return Result.error(404, "文件不存在");
        }

        // 验证用户是否为团队成员
        QueryWrapper<TeamMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_id", file.getTeamId())
                .eq("user_id", currentUserId);
        TeamMember member = teamMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            return Result.error(403, "无权限访问该文件历史");
        }

        int safeLimit = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);

        List<TeamActivity> activities = teamActivityMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TeamActivity>()
                        .eq(TeamActivity::getTeamId, file.getTeamId())
                        .eq(TeamActivity::getActivityType, "file")
                        .eq(TeamActivity::getRelatedId, fileId)
                        .orderByDesc(TeamActivity::getCreatedAt)
                        .last("LIMIT " + safeLimit)
        );

        List<ActivityVO> vos = activities.stream().map(a -> {
            ActivityVO vo = new ActivityVO();
            BeanUtils.copyProperties(a, vo);
            return vo;
        }).collect(Collectors.toList());

        return Result.success(vos);
    }
}
