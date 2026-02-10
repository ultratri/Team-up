package com.teamup.server.modules.project.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目评论返回对象（带用户信息和子评论）
 */
@Data
public class ProjectCommentVO {

    private Long id;
    private Long projectId;
    private Long parentId;
    private Long userId;

    // 评论内容
    private String content;

    // 用户信息
    private String username;
    private String nickname;
    private String avatar;

    private LocalDateTime createdAt;

    // 子评论列表
    private List<ProjectCommentVO> replies;
}

