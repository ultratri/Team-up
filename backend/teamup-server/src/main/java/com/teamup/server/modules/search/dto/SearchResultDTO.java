package com.teamup.server.modules.search.dto;

import lombok.Data;
import java.util.List;

/**
 * 搜索结果DTO
 */
@Data
public class SearchResultDTO {
    private List<SearchItemDTO> projects;
    private List<SearchItemDTO> users;
    private List<SearchItemDTO> teams;
    private Integer totalCount;
}
