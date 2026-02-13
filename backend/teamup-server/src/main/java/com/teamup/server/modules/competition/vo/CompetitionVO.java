package com.teamup.server.modules.competition.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 比赛视图对象（包含额外的统计信息）
 */
@Data
public class CompetitionVO {
    private Long id;
    private String name;
    private String organizer;
    private String level;
    private String scope;
    private String audience;
    private String type;
    private LocalDateTime signupStartAt;
    private LocalDateTime signupEndAt;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer maxTeamMembers;
    private Integer minTeamMembers;
    private Boolean requireMentor;
    private Integer maxTeamsPerUser;
    private Boolean eligibilityEnabled;
    private String status;
    private String description;
    private String attachments;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 队伍数量（动态计算）
     */
    private Integer teamCount;
    
    /**
     * 从Competition实体创建CompetitionVO
     */
    public static CompetitionVO fromEntity(com.teamup.server.modules.competition.entity.Competition competition) {
        CompetitionVO vo = new CompetitionVO();
        vo.setId(competition.getId());
        vo.setName(competition.getName());
        vo.setOrganizer(competition.getOrganizer());
        vo.setLevel(competition.getLevel());
        vo.setScope(competition.getScope());
        vo.setAudience(competition.getAudience());
        vo.setType(competition.getType());
        vo.setSignupStartAt(competition.getSignupStartAt());
        vo.setSignupEndAt(competition.getSignupEndAt());
        vo.setStartAt(competition.getStartAt());
        vo.setEndAt(competition.getEndAt());
        vo.setMaxTeamMembers(competition.getMaxTeamMembers());
        vo.setMinTeamMembers(competition.getMinTeamMembers());
        vo.setRequireMentor(competition.getRequireMentor());
        vo.setMaxTeamsPerUser(competition.getMaxTeamsPerUser());
        vo.setEligibilityEnabled(competition.getEligibilityEnabled());
        vo.setStatus(competition.getStatus());
        vo.setDescription(competition.getDescription());
        vo.setAttachments(competition.getAttachments());
        vo.setCreatedBy(competition.getCreatedBy());
        vo.setCreatedAt(competition.getCreatedAt());
        vo.setUpdatedAt(competition.getUpdatedAt());
        return vo;
    }
}
