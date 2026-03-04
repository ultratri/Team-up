package com.teamup.server.modules.project.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 团队申请DTO
 */
@Data
public class TeamApplicationDTO {
    private Long id;
    private Long projectId;
    private String projectTitle;
    private Long leaderId;
    private String leaderName;
    private String message;
    private String status;
    private Long reviewedBy;
    private String reviewerName;
    private String reviewComment;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
    
    /**
     * 团队成员列表
     */
    private List<TeamMemberDTO> members;
    
    @Data
    public static class TeamMemberDTO {
        private Long userId;
        private String username;
        private String nickname;
        private String avatar;
        private Boolean confirmed;
        private LocalDateTime confirmedAt;
    }
}
