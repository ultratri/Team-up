package com.teamup.server.modules.competition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.teamup.server.common.utils.StringJsonDeserializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 比赛实体，对应表 competitions
 */
@Data
@TableName("competitions")
public class Competition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String organizer;
    private String level;
    private String scope;
    /**
     * JSON：{departments:[], majors:[], grades:[]}
     * 为空表示全体
     */
    @JsonDeserialize(using = StringJsonDeserializer.class)
    private String audience;
    private String type;

    private LocalDateTime signupStartAt;
    private LocalDateTime signupEndAt;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private Integer maxTeamMembers;
    private Integer minTeamMembers;
    private Boolean requireMentor;
    /**
     * 同一比赛每个用户可参加的队伍上限，null 表示不限制
     */
    private Integer maxTeamsPerUser;
    /**
     * 是否启用报名/参赛资格限制（按 audience 校验），null 视为 false
     */
    private Boolean eligibilityEnabled;

    private String status;
    private String description;
    /**
     * JSON 字符串：[{name,url}]
     */
    @JsonDeserialize(using = StringJsonDeserializer.class)
    private String attachments;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

