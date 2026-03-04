package com.teamup.server.modules.newbie.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 技能认证VO
 */
@Data
public class SkillCertificationVO {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String skillName;
    private String skillCategory;
    private String proficiencyLevel;
    private String proficiencyLevelText;
    private String certificationType;
    private String certificationTypeText;
    private String proofUrl;
    private String proofDescription;
    private String status;
    private String statusText;
    private Long verifiedBy;
    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
    
    public String getProficiencyLevelText() {
        return com.teamup.server.common.enums.ProficiencyLevel.getName(proficiencyLevel);
    }
    
    public String getCertificationTypeText() {
        if (certificationType == null) return "";
        switch (certificationType) {
            case "SELF_CLAIM": return "自我声明";
            case "PEER_VERIFIED": return "同行验证";
            case "OFFICIAL": return "官方认证";
            default: return certificationType;
        }
    }
    
    public String getStatusText() {
        if (status == null) return "";
        switch (status) {
            case "PENDING": return "待审核";
            case "APPROVED": return "已通过";
            case "REJECTED": return "已拒绝";
            default: return status;
        }
    }
}
