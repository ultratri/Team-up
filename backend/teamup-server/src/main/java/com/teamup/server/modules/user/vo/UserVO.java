package com.teamup.server.modules.user.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象（包含角色信息和个人资料）
 */
@Data
public class UserVO {
    private Long id;
    private String userCode;
    private String username;
    private String email;
    private String phone;
    private String status;
    private List<String> roles;
    private UserProfileVO profile;  // 添加个人资料
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}
