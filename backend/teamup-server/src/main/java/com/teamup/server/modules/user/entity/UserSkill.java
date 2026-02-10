package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户技能实体
 */
@Data
@TableName("user_skills")
public class UserSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private String skillName;
    private String skillCategory;
    private String proficiencyLevel;  // BEGINNER, INTERMEDIATE, EXPERT
    private Boolean isCustom;
    private LocalDateTime createdAt;
}

