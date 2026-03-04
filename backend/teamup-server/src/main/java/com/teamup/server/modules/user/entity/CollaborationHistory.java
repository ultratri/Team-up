package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 协作历史实体
 */
@Data
@TableName("collaboration_history")
public class CollaborationHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Long partnerId;
    private Long projectId;
    private BigDecimal collaborationScore;  // 协作评分(0-1)
    private LocalDateTime createdAt;
}
