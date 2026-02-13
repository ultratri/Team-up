package com.teamup.server.modules.mentor.vo;

import lombok.Data;

/**
 * 导师信息VO
 */
@Data
public class MentorInfoVO {
    private Long id;
    private String username;
    private String realName;
    private String department;
    private String major;
    private Integer totalMentees;
    private Integer activeMentees;
    private Integer completedMentees;
    private Integer successfulMentees;
    private Double averageMenteeScore;
    private Integer totalRewardPoints;
    private Double rating;
}
