package com.teamup.server.modules.mentor.dto;

import lombok.Data;

/**
 * 审核导师申请请求DTO
 */
@Data
public class ReviewApplicationRequest {
    
    private Boolean approved;
    
    private String reviewComment;
}
