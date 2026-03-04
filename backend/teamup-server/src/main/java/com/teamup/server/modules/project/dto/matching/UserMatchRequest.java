package com.teamup.server.modules.project.dto.matching;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 用户找项目的匹配请求
 */
@Data
public class UserMatchRequest {
    private Long userId;
    private Map<String, Object> user;  // 用户信息
    private List<Map<String, Object>> projects;  // 候选项目列表
}
