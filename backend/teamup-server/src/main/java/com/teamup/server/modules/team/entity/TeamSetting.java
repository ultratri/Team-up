package com.teamup.server.modules.team.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 团队设置实体类
 * 用于存储团队的各种配置，如第三方工具链接等
 */
@Data
@TableName("team_settings")
public class TeamSetting {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long teamId;
    private String settingKey;
    private String settingValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
