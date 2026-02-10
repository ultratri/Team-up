package com.teamup.server.modules.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.competition.entity.TeamCompetitionScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TeamCompetitionScoreMapper extends BaseMapper<TeamCompetitionScore> {

    @Select(
            "SELECT " +
            " s.team_id AS teamId, " +
            " t.team_name AS teamName, " +
            " (SELECT COUNT(*) FROM team_members tm WHERE tm.team_id = t.id) AS memberCount, " +
            " CASE WHEN t.mentor_id IS NULL THEN 0 ELSE 1 END AS hasMentor, " +
            " s.score AS score, " +
            " s.comment AS comment, " +
            " s.scored_by AS scoredBy, " +
            " u.username AS scoredByName " +
            "FROM team_competition_scores s " +
            "JOIN teams t ON t.id = s.team_id " +
            "LEFT JOIN users u ON u.id = s.scored_by " +
            "WHERE s.competition_id = #{competitionId} " +
            "ORDER BY s.score DESC, t.created_at ASC " +
            "LIMIT #{limit}"
    )
    List<Map<String, Object>> selectScoreLeaderboard(@Param("competitionId") Long competitionId, @Param("limit") int limit);

    /**
     * 查询所有已评分的队伍（用于前端筛选已评分/未评分）
     */
    @Select(
            "SELECT competition_id AS competitionId, team_id AS teamId " +
            "FROM team_competition_scores"
    )
    List<Map<String, Object>> selectAllScoredTeams();
}

