package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目评论实体
 */
@Data
@TableName("project_comments")
public class ProjectComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目ID */
    private Long projectId;

    /** 父评论ID（为空表示顶级评论） */
    private Long parentId;

    /** 评论用户ID */
    private Long userId;

    /** 评论内容（纯文本或富文本简化版） */
    private String content;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}

