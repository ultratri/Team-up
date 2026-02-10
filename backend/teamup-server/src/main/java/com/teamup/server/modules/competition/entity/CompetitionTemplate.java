package com.teamup.server.modules.competition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 比赛模板实体
 */
@Data
@TableName("competition_templates")
public class CompetitionTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    /**
     * JSON 字符串，存储比赛字段模板
     */
    private String payload;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

