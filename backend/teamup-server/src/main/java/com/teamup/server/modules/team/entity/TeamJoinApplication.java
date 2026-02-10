package com.teamup.server.modules.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 队伍加入申请实体
 */
@Data
@TableName("team_join_applications")
public class TeamJoinApplication {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;
    private Long competitionId;
    private Long applicantId;
    private String reason;
    /**
     * PENDING / APPROVED / REJECTED / WITHDRAWN
     */
    private String status;
    private Long reviewedBy;
    private String reviewComment;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}

