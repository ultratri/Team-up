package com.teamup.server.modules.user.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户更新请求DTO（管理员）
 */
@Data
public class UserUpdateRequest {
    private String userCode;
    
    @Size(min = 2, max = 50, message = "用户名长度在2-50之间")
    private String username;
    
    @Size(min = 6, max = 20, message = "密码长度在6-20之间")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phone;
    
    private String status;  // ACTIVE, INACTIVE, BANNED
    
    private String role;  // STUDENT, MENTOR, PLATFORM_ADMIN
}
