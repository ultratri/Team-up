package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.dto.TaskDetailDTO;
import com.teamup.server.modules.team.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务Mapper
 * Performance optimized with batch queries
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
    
    /**
     * 批量查询任务详情（优化N+1查询）
     * 使用JOIN一次性获取任务、负责人、评论、附件信息
     */
    @Select("SELECT t.*, " +
            "COUNT(DISTINCT ta.id) as assignee_count, " +
            "COUNT(DISTINCT tc.id) as comment_count, " +
            "COUNT(DISTINCT tat.id) as attachment_count " +
            "FROM tasks t " +
            "LEFT JOIN task_assignees ta ON t.id = ta.task_id " +
            "LEFT JOIN task_comments tc ON t.id = tc.task_id " +
            "LEFT JOIN task_attachments tat ON t.id = tat.task_id " +
            "WHERE t.team_id = #{teamId} " +
            "GROUP BY t.id " +
            "ORDER BY t.created_at DESC")
    List<Task> selectTasksWithCountsByTeamId(@Param("teamId") Long teamId);
}

