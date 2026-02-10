package com.teamup.server.modules.competition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 比赛队伍指导老师申请实体
 */
@Data
@TableName("team_mentor_applications")
public class TeamMentorApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;
    private Long competitionId;
    private Long mentorId;
    private Long requestedBy;
    /**
     * PENDING / APPROVED / REJECTED
     */
    private String status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
}

