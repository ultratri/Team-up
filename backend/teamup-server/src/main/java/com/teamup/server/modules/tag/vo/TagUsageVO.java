package com.teamup.server.modules.tag.vo;

import com.teamup.server.modules.tag.entity.Tag;
import lombok.Data;

/**
 * 标签使用统计VO
 */
@Data
public class TagUsageVO {
    
    private Long id;
    private String name;
    private Tag.TagCategory category;
    private Long userCount;      // 使用该标签的用户数
    private Long projectCount;   // 使用该标签的项目数
    private Integer totalUsage;  // 总使用次数
}
