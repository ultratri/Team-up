package com.teamup.server.modules.newbie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 技能认证实体
 */
@Data
@TableName("skill_certifications")
public class SkillCertification {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private String skillName;
    private String skillCategory;
    private String proficiencyLevel;       // BEGINNER, INTERMEDIATE, EXPERT
    private String certificationType;      // SELF_CLAIM, PEER_VERIFIED, OFFICIAL
    private String proofUrl;
    private String proofDescription;
    
    // 审核信息
    private String status;                 // PENDING, APPROVED, REJECTED
    private Long verifiedBy;
    private LocalDateTime verifiedAt;
    private String rejectReason;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
