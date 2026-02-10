package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {
    
    /**
     * 根据团队ID和用户ID查询团队成员
     */
    @Select("SELECT * FROM team_members WHERE team_id = #{teamId} AND user_id = #{userId} LIMIT 1")
    TeamMember selectByTeamAndUser(@Param("teamId") Long teamId, @Param("userId") Long userId);

    /**
     * 统计某个比赛下的参赛成员总数（team_members join teams）
     */
    @Select("SELECT COUNT(*) FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE t.competition_id = #{competitionId}")
    Long countMembersByCompetitionId(@Param("competitionId") Long competitionId);

    /**
     * 判断某用户是否已在某比赛的任意队伍中
     */
    @Select("SELECT COUNT(*) FROM team_members tm JOIN teams t ON tm.team_id = t.id WHERE tm.user_id = #{userId} AND t.competition_id = #{competitionId}")
    Long countUserMembershipInCompetition(@Param("userId") Long userId, @Param("competitionId") Long competitionId);

    /**
     * 按天统计某比赛下新增成员数（按加入时间）
     */
    @Select(
            "SELECT DATE(tm.joined_at) AS day, COUNT(*) AS memberCount " +
            "FROM team_members tm " +
            "JOIN teams t ON tm.team_id = t.id " +
            "WHERE t.competition_id = #{competitionId} " +
            "  AND tm.joined_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(tm.joined_at) " +
            "ORDER BY day ASC"
    )
    java.util.List<java.util.Map<String, Object>> aggregateDailyMemberCount(@Param("competitionId") Long competitionId, @Param("days") int days);
}
