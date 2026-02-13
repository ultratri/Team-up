package com.teamup.server.modules.mentor.vo;

import lombok.Data;

/**
 * 导师排行VO
 */
@Data
public class MentorRankingVO {
    private Integer rank;
    private Long mentorId;
    private String mentorName;
    private String department;
    private Integer successfulMentees;
    private Double averageMenteeScore;
    private Integer totalRewardPoints;
    private Double rating;
}
