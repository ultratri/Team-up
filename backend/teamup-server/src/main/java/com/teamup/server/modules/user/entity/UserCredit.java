package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信誉实体
 */
@Data
@TableName("user_credits")
public class UserCredit {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Integer totalCredit;
    private String creditLevel;  // NEWBIE, RELIABLE, EXCELLENT, OUTSTANDING
    private LocalDateTime updatedAt;
}

