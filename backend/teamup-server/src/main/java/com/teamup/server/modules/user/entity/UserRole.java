package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户角色实体
 */
@Data
@TableName("user_roles")
public class UserRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private String roleName;  // STUDENT, PROJECT_CREATOR, TEAM_LEADER, MENTOR, PLATFORM_ADMIN
    private LocalDateTime grantedAt;
    private Long grantedBy;
}

