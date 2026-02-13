package com.teamup.server.modules.mentor.vo;

import lombok.Data;

/**
 * 导师详情VO
 */
@Data
public class MentorDetailVO {
    private Long id;
    private String username;
    private String realName;
    private String department;
    private String major;
    private String email;
    private String phone;
    private String bio;
    private Integer totalMentees;
    private Integer activeMentees;
    private Integer completedMentees;
    private Integer successfulMentees;
    private Double averageMenteeScore;
    private Integer totalRewardPoints;
    private Double rating;
    private String becameMentorAt;
    private String applicationReason;
}
