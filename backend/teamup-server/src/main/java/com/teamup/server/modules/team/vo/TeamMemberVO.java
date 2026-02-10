package com.teamup.server.modules.team.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeamMemberVO {
    private Long id;
    private Long teamId;
    private Long userId;
    private String role;
    private LocalDateTime joinedAt;
    
    // 用户信息
    private String username;
    private String nickname;
    private String avatar;
}
