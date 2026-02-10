package com.teamup.server.modules.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.modules.project.vo.ProjectCommentVO;

public interface ProjectCommentService {

    /**
     * 分页获取项目的评论（顶级评论 + 嵌套子评论）
     */
    Page<ProjectCommentVO> getProjectComments(Long projectId, int page, int size);

    /**
     * 新增评论或回复
     */
    ProjectCommentVO addComment(
            Long projectId,
            Long userId,
            Long parentId,
            Long replyToUserId,
            String content
    );
}

