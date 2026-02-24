package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.Team;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 团队Mapper
 */
@Mapper
public interface TeamMapper extends BaseMapper<Team> {

    /**
     * 比赛排行榜：按完成率/完成数排序
     */
    @Select(
            "SELECT " +
            " t.id AS teamId, " +
            " t.team_name AS teamName, " +
            " (SELECT COUNT(*) FROM team_members tm WHERE tm.team_id = t.id) AS memberCount, " +
            " CASE WHEN t.mentor_id IS NULL THEN 0 ELSE 1 END AS hasMentor, " +
            " (SELECT COUNT(*) FROM tasks tk WHERE tk.team_id = t.id) AS totalTasks, " +
            " (SELECT COUNT(*) FROM tasks tk WHERE tk.team_id = t.id AND tk.status = 'DONE') AS doneTasks " +
            "FROM teams t " +
            "WHERE t.competition_id = #{competitionId} " +
            "ORDER BY " +
            "  (CASE WHEN (SELECT COUNT(*) FROM tasks tk WHERE tk.team_id = t.id) = 0 THEN 0 " +
            "        ELSE ( (SELECT COUNT(*) FROM tasks tk WHERE tk.team_id = t.id AND tk.status = 'DONE') * 1.0 / (SELECT COUNT(*) FROM tasks tk WHERE tk.team_id = t.id) ) END) DESC, " +
            "  (SELECT COUNT(*) FROM tasks tk WHERE tk.team_id = t.id AND tk.status = 'DONE') DESC, " +
            "  t.created_at ASC " +
            "LIMIT #{limit}"
    )
    List<Map<String, Object>> selectCompetitionLeaderboard(@Param("competitionId") Long competitionId, @Param("limit") int limit);

    /**
     * 按天统计某比赛下新建队伍数
     */
    @Select(
            "SELECT DATE(created_at) AS day, COUNT(*) AS teamCount " +
            "FROM teams " +
            "WHERE competition_id = #{competitionId} " +
            "  AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY day ASC"
    )
    List<Map<String, Object>> aggregateDailyTeamCount(@Param("competitionId") Long competitionId, @Param("days") int days);
    
    /**
     * 查询团队关联的比赛ID列表
     */
    @Select("SELECT competition_id FROM team_competitions WHERE team_id = #{teamId}")
    List<Long> selectTeamCompetitionIds(@Param("teamId") Long teamId);
    
    /**
     * 检查团队和比赛的关联是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM team_competitions WHERE team_id = #{teamId} AND competition_id = #{competitionId}")
    boolean isTeamCompetitionExists(@Param("teamId") Long teamId, @Param("competitionId") Long competitionId);
    
    /**
     * 添加团队和比赛的关联
     */
    @org.apache.ibatis.annotations.Insert("INSERT INTO team_competitions (team_id, competition_id, created_at) VALUES (#{teamId}, #{competitionId}, NOW())")
    void insertTeamCompetition(@Param("teamId") Long teamId, @Param("competitionId") Long competitionId);
    
    /**
     * 删除团队和比赛的关联
     */
    @org.apache.ibatis.annotations.Delete("DELETE FROM team_competitions WHERE team_id = #{teamId} AND competition_id = #{competitionId}")
    void deleteTeamCompetition(@Param("teamId") Long teamId, @Param("competitionId") Long competitionId);
}
