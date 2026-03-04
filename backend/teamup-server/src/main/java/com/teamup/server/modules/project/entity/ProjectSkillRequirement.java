package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目技能需求实体
 */
@Data
@TableName("project_skill_requirements")
public class ProjectSkillRequirement {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    private String skillName;
    
    @TableField("is_required")
    private Boolean required;  // 是否必需技能
    
    @TableField("expected_level")
    private String proficiencyLevel;  // 要求的熟练度等级
    
    private LocalDateTime createdAt;
}
