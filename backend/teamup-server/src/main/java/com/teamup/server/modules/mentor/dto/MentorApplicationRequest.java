package com.teamup.server.modules.mentor.dto;

import lombok.Data;

/**
 * 鐎电厧绗€閻㈠疇顕拠閿嬬湴DTO
 */
@Data
public class MentorApplicationRequest {
    
    private String realName;
    
    private String userCode;
    
    private String department;
    
    private String major;
    
    private String email;
    
    private String phone;
    
    private String bio;
    
    private String projectExperience;
    
    private String guidanceExperience;
    
    private String applicationReason;
}
