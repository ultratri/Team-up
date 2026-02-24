package com.teamup.server.modules.competition.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队导师申请VO
 */
@Data
public class TeamMentorApplicationVO {
    private Long id;
    private Long teamId;
    private String teamName;
    private Long competitionId;
    private String competitionName;
    private Long mentorId;
    private Long requestedBy;
    private String requesterName;
    private String status;
    private String reason;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
}
