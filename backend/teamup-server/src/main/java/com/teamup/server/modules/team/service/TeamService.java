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
     * 邀请成员加入团队
     * @param teamId 团队ID
     * @param inviterId 邀请人ID
     * @param inviteeId 被邀请人ID
     * @param message 邀请留言
     */
    void inviteMember(Long teamId, Long inviterId, Long inviteeId, String message);
    
    /**
     * 获取邀请详情
     * @param invitationId 邀请ID
     * @param userId 用户ID
     * @return 邀请详情
     */
    Map<String, Object> getInvitationDetail(Long invitationId, Long userId);
    
    /**
     * 接受团队邀请
     * @param invitationId 邀请ID
     * @param userId 用户ID
     */
    void acceptInvitation(Long invitationId, Long userId);
    
    /**
     * 拒绝团队邀请
     * @param invitationId 邀请ID
     * @param userId 用户ID
     */
    void rejectInvitation(Long invitationId, Long userId);
    
    /**
     * 获取用户发出的邀请列表
     * @param userId 用户ID
     * @return 邀请列表
     */
    List<Map<String, Object>> getSentInvitations(Long userId);
    
    /**
     * 获取用户收到的邀请列表
     * @param userId 用户ID
     * @return 邀请列表
     */
    List<Map<String, Object>> getReceivedInvitations(Long userId);
    
    /**
     * 撤回邀请
     * @param invitationId 邀请ID
     * @param userId 用户ID
     */
    void cancelInvitation(Long invitationId, Long userId);
    
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
    
    /**
     * 获取团队关联的比赛列表
     */
    List<Map<String, Object>> getTeamCompetitions(Long teamId);

    /**
     * 成员找团队：为当前用户匹配长期团队
     * @deprecated 此功能已废弃，建议使用"成员找项目"功能
     */
    @Deprecated
    List<Map<String, Object>> matchTeamsForUser(Long userId, int page, int size);
    
    /**
     * 添加团队关联比赛
     */
    void addTeamCompetition(Long teamId, Long competitionId);
    
    /**
     * 移除团队关联比赛
     */
    void removeTeamCompetition(Long teamId, Long competitionId);
    
    /**
     * 解散团队（项目完成后）
     */
    void dissolveTeam(Long teamId, Long userId);
}
