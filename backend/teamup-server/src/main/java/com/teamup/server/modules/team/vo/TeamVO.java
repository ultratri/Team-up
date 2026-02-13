package com.teamup.server.modules.team.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队视图对象（包含额外的统计信息）
 */
@Data
public class TeamVO {
    private Long id;
    private String teamNature;
    private Long competitionId;
    private Long projectId;
    private String teamName;
    private String description;
    private String avatar;
    private Long leaderId;
    private Integer maxMembers;
    private Long mentorId;
    private String status;
    private Long sourceProjectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 成员数量（动态计算）
     */
    private Integer memberCount;
    
    /**
     * 从Team实体创建TeamVO
     */
    public static TeamVO fromEntity(com.teamup.server.modules.team.entity.Team team) {
        TeamVO vo = new TeamVO();
        vo.setId(team.getId());
        vo.setTeamNature(team.getTeamNature());
        vo.setCompetitionId(team.getCompetitionId());
        vo.setProjectId(team.getProjectId());
        vo.setTeamName(team.getTeamName());
        vo.setDescription(team.getDescription());
        vo.setAvatar(team.getAvatar());
        vo.setLeaderId(team.getLeaderId());
        vo.setMaxMembers(team.getMaxMembers());
        vo.setMentorId(team.getMentorId());
        vo.setStatus(team.getStatus());
        vo.setSourceProjectId(team.getSourceProjectId());
        vo.setCreatedAt(team.getCreatedAt());
        vo.setUpdatedAt(team.getUpdatedAt());
        return vo;
    }
}
