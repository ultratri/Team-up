package com.teamup.server.modules.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.project.vo.ProjectFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 项目文件服务接口
 */
public interface ProjectFileService {
    /**
     * 获取项目文件列表（分页）
     */
    Page<ProjectFileVO> getProjectFiles(Long projectId, String category, int page, int size);
    
    /**
     * 上传项目文件
     */
    ProjectFileVO uploadFile(Long projectId, Long userId, MultipartFile file, String category, String description);
    
    /**
     * 删除项目文件
     */
    void deleteFile(Long fileId, Long userId);
    
    /**
     * 获取文件分类列表
     */
    List<String> getFileCategories(Long projectId);
}
