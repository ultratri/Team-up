package com.teamup.server.modules.newbie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新手保护配置实体
 */
@Data
@TableName("newbie_config")
public class NewbieConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Integer protectionDays;        // 新手保护期天数
    private Integer baseReputationScore;   // 新手基础信誉分
    private Integer matchingBonus;         // 新手匹配加成分数
    private Long updatedBy;                // 更新者ID
    private LocalDateTime updatedAt;       // 更新时间
}
