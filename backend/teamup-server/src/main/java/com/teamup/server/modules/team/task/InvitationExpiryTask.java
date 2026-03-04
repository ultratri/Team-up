package com.teamup.server.modules.team.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.team.entity.TeamInvitation;
import com.teamup.server.modules.team.mapper.TeamInvitationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 团队邀请过期处理定时任务
 * 每小时检查一次过期的邀请，自动更新状态
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationExpiryTask {

    private final TeamInvitationMapper teamInvitationMapper;

    /**
     * 处理过期邀请
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void processExpiredInvitations() {
        log.info("开始处理过期邀请...");
        
        try {
            // 查询所有待处理且已过期的邀请
            LambdaQueryWrapper<TeamInvitation> query = new LambdaQueryWrapper<>();
            query.eq(TeamInvitation::getStatus, "PENDING")
                 .le(TeamInvitation::getExpiresAt, LocalDateTime.now())
                 .isNotNull(TeamInvitation::getExpiresAt);
            
            List<TeamInvitation> expiredInvitations = teamInvitationMapper.selectList(query);
            
            if (expiredInvitations.isEmpty()) {
                log.info("没有需要处理的过期邀请");
                return;
            }
            
            // 批量更新状态为EXPIRED
            int count = 0;
            for (TeamInvitation invitation : expiredInvitations) {
                invitation.setStatus("EXPIRED");
                invitation.setUpdatedAt(LocalDateTime.now());
                teamInvitationMapper.updateById(invitation);
                count++;
            }
            
            log.info("成功处理 {} 条过期邀请", count);
            
        } catch (Exception e) {
            log.error("处理过期邀请失败", e);
            throw e;
        }
    }
}
