package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.teamup.server.modules.team.dto.TeamCreateRequest;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.modules.team.entity.TeamMember;
import com.teamup.server.modules.team.vo.AdminTeamDetailVO;
import com.teamup.server.modules.team.vo.AdminTeamListVO;
import com.teamup.server.modules.team.vo.TeamMemberVO;
import java.util.List;
import java.util.Map;

public interface TeamService extends IService<Team> {
    Team createTeam(TeamCreateRequest request);
    List<Team> getUserTeams(Long userId);
    Team getTeamById(Long teamId);
    
    void addMember(Long teamId, Long userId);
    void removeMember(Long teamId, Long userId);
    List<TeamMemberVO> getTeamMembers(Long teamId);
    
    /**
     * 退出团队（普通成员）
     */
    void leaveTeam(Long teamId, Long userId);
    
    /**
     * 删除团队（仅领导者）
     */
    void deleteTeam(Long teamId, Long userId);
    
    /**
     * 更新团队头像
     */
    void updateTeamAvatar(Long teamId, String avatarUrl);
    
    /**
     * 更新团队信息
     */
    Team updateTeam(Long teamId, Map<String, Object> updates);
    
    /**
     * 管理员获取团队列表（分页）
     */
    Page<AdminTeamListVO> getAdminTeamList(Integer page, Integer size, String type, Boolean isActive, String keyword);
    
    /**
     * 管理员获取团队详情
     */
    AdminTeamDetailVO getAdminTeamDetail(Long teamId);
    
    /**
     * 获取团队成员列表（用于通知）
     */
    List<TeamMember> getTeamMembersByTeamId(Long teamId);
    
    /**
     * 获取管理员团队统计信息
     */
    Map<String, Object> getAdminTeamStatistics();
}
