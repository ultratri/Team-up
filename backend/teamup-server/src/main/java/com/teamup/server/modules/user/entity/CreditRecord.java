package com.teamup.server.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 信誉变更记录实体
 */
@Data
@TableName("credit_records")
public class CreditRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    private Integer changeAmount;
    private String changeType;
    private Long relatedProjectId;
    private Long relatedUserId;
    private String reason;
    private LocalDateTime createdAt;
    
    // 为了兼容旧代码,添加别名方法
    public void setProjectId(Long projectId) {
        this.relatedProjectId = projectId;
    }
    
    public Long getProjectId() {
        return this.relatedProjectId;
    }
}
