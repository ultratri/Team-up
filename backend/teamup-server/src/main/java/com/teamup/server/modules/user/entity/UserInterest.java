package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户兴趣标签实体
 */
@Data
@TableName("user_interests")
public class UserInterest {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private String interestName;
    private LocalDateTime createdAt;
}
