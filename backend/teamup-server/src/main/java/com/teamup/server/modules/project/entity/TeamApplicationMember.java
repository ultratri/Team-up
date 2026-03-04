package com.teamup.server.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队申请成员实体
 */
@Data
@TableName("team_application_members")
public class TeamApplicationMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 团队申请ID
     */
    private Long teamApplicationId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 是否确认参与
     */
    private Boolean confirmed;
    
    /**
     * 确认时间
     */
    private LocalDateTime confirmedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
