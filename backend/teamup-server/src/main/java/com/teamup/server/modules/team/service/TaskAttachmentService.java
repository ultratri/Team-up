package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teamup.server.modules.team.dto.TaskAttachmentDTO;
import com.teamup.server.modules.team.entity.TaskAttachment;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 任务附件服务接口
 * Requirements: 4.1, 4.3
 */
public interface TaskAttachmentService extends IService<TaskAttachment> {
    
    /**
     * 上传任务附件
     * @param taskId 任务ID
     * @param file 文件
     * @param uploadedBy 上传者ID
     * @return 附件DTO
     */
    TaskAttachmentDTO uploadAttachment(Long taskId, MultipartFile file, Long uploadedBy);
    
    /**
     * 删除任务附件
     * @param attachmentId 附件ID
     */
    void deleteAttachment(Long attachmentId);
    
    /**
     * 获取任务的所有附件
     * @param taskId 任务ID
     * @return 附件DTO列表
     */
    List<TaskAttachmentDTO> getAttachmentsByTaskId(Long taskId);
    
    /**
     * 下载附件
     * @param attachmentId 附件ID
     * @return 文件资源
     */
    Resource downloadAttachment(Long attachmentId);
}
