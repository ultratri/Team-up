package com.teamup.server.modules.user.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    @NotBlank(message = "学号/工号不能为空")
    private String studentId;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}

