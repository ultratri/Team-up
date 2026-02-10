package com.teamup.server.modules.ecosystem.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资源VO
 */
@Data
public class ResourceVO {
    
    /**
     * 资源ID
     */
    private Long id;
    
    /**
     * 资源标题
     */
    private String title;
    
    /**
     * 资源描述
     */
    private String description;
    
    /**
     * 资源类型
     */
    private String type;
    
    /**
     * 封面图URL
     */
    private String cover;
    
    /**
     * 资源内容
     */
    private String content;
    
    /**
     * 作者信息
     */
    private AuthorInfo author;
    
    /**
     * 标签列表
     */
    private List<String> tags;
    
    /**
     * 浏览量
     */
    private Integer views;
    
    /**
     * 点赞数
     */
    private Integer likes;
    
    /**
     * 是否已点赞
     */
    private Boolean liked;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 作者信息内部类
     */
    @Data
    public static class AuthorInfo {
        private Long id;
        private String realName;
        private String avatar;
    }
}
