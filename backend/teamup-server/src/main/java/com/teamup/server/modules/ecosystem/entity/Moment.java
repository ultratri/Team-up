package com.teamup.server.modules.ecosystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态实体类
 * 用于动态广场展示用户的关键活动
 */
@Data
@TableName("moments")
public class Moment {
    
    /**
     * 动态ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 动态类型：CREATE_PROJECT(创建项目)/JOIN_PROJECT(加入项目)/COMPLETE_PROJECT(完成项目)/GET_BADGE(获得徽章)
     */
    private String type;
    
    /**
     * 动态内容
     */
    private String content;
    
    /**
     * 相关项目ID（可选）
     */
    private Long relatedProjectId;
    
    /**
     * 点赞数
     */
    private Integer likes;
    
    /**
     * 评论数
     */
    private Integer comments;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Boolean deleted;
}
