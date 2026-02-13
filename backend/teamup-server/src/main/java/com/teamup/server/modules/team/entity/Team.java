package com.teamup.server.modules.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队实体
 */
@Data
@TableName("teams")
public class Team {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 团队性质：TEMPORARY（临时）/ LONG_TERM（长期）
     */
    private String teamNature;

    /**
     * 关联的比赛ID（仅比赛队伍使用）
     */
    private Long competitionId;

    private Long projectId;
    private String teamName;
    private String description;
    private String avatar;
    private Long leaderId;
    /**
     * 队伍人数上限（可为空，使用比赛默认值）
     */
    private Integer maxMembers;

    /**
     * 指导老师用户ID（可为空）
     */
    private Long mentorId;
    
    /**
     * 团队状态：ACTIVE（活跃）/ INACTIVE（不活跃）/ DISSOLVED（已解散）
     */
    private String status;
    
    /**
     * 来源项目ID（从项目创建的团队）
     */
    private Long sourceProjectId;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

