package com.teamup.server.modules.ecosystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态VO
 */
@Data
public class MomentVO {
    
    /**
     * 动态ID
     */
    private Long id;
    
    /**
     * 动态类型
     */
    private String type;
    
    /**
     * 动态内容
     */
    private String content;
    
    /**
     * 用户信息
     */
    private UserInfo user;
    
    /**
     * 相关项目信息
     */
    private RelatedProject relatedProject;
    
    /**
     * 点赞数
     */
    private Integer likes;
    
    /**
     * 评论数
     */
    private Integer comments;
    
    /**
     * 是否已点赞
     */
    private Boolean liked;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfo {
        private Long id;
        private String realName;
        private String avatar;
    }
    
    /**
     * 相关项目内部类
     */
    @Data
    public static class RelatedProject {
        private Long id;
        private String title;
    }
}
