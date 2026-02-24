package com.teamup.server.modules.team.vo;

import lombok.Data;
import java.util.List;

/**
 * 工具分组 VO
 */
@Data
public class ToolGroupVO {
    private String id;
    private String name;
    private Integer order;
    private List<GroupLinkVO> links;
}
