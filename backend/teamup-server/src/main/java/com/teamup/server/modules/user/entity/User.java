package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体
 */
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String userCode;  // 用户编号（学号/工号/账号等）
    private String username;
    private String password;
    private String email;
    private String phone;
    private String status;  // ACTIVE, INACTIVE, BANNED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    
    // 角色列表（不映射到数据库，仅用于传输）
    @TableField(exist = false)
    private List<String> roles;
}

