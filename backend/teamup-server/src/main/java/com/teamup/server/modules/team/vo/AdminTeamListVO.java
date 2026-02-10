package com.teamup.server.modules.team.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员团队列表视图对象
 */
@Data
public class AdminTeamListVO {
    /**
     * 团队ID
     */
    private Long id;

    /**
     * 团队名称
     */
    private String name;

    /**
     * 团队类型
     */
    private String type;

    /**
     * 团队领导者ID
     */
    private Long leaderId;

    /**
     * 团队领导者姓名
     */
    private String leaderName;

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 项目数量
     */
    private Integer projectCount;

    /**
     * 是否活跃
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
