package com.teamup.server.modules.team.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员团队详情视图对象
 */
@Data
public class AdminTeamDetailVO {
    /**
     * 团队ID
     */
    private Long id;

    /**
     * 团队名称
     */
    private String name;

    /**
     * 团队描述
     */
    private String description;

    /**
     * 团队类型
     */
    private String type;

    /**
     * 团队专长
     */
    private String specialization;

    /**
     * 是否活跃
     */
    private Boolean isActive;

    /**
     * 团队领导者信息
     */
    private LeaderInfo leader;

    /**
     * 团队成员列表
     */
    private List<MemberInfo> members;

    /**
     * 团队项目列表
     */
    private List<ProjectInfo> projects;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    @Data
    public static class LeaderInfo {
        private Long id;
        private String name;
        private String email;
        private String department;
        private String major;
    }

    @Data
    public static class MemberInfo {
        private Long id;
        private Long userId;
        private String userName;
        private String role;
        private LocalDateTime joinedAt;
        private LocalDateTime leftAt;
    }

    @Data
    public static class ProjectInfo {
        private Long id;
        private String name;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }
}
