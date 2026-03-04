package com.teamup.server.modules.user.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 不活跃用户信誉分衰减定时任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InactiveUserCreditTask {
    
    private final UserMapper userMapper;
    private final CreditService creditService;
    
    /**
     * 每天凌晨2点执行
     * 查询30天无活动的用户，扣除5分信誉分
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkInactiveUsers() {
        log.info("开始执行不活跃用户信誉衰减任务");
        
        try {
            // 查询30天前的时间
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            // 查询30天无活动的用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(User::getLastLoginAt, thirtyDaysAgo)
                   .or()
                   .isNull(User::getLastLoginAt);
            
            List<User> inactiveUsers = userMapper.selectList(wrapper);
            
            if (inactiveUsers.isEmpty()) {
                log.info("没有不活跃用户需要处理");
                return;
            }
            
            int processedCount = 0;
            for (User user : inactiveUsers) {
                try {
                    creditService.addCreditRecord(
                        user.getId(),
                        -5,
                        "INACTIVE_DECAY",
                        null,
                        "30天无活动，信誉分衰减"
                    );
                    processedCount++;
                } catch (Exception e) {
                    log.error("处理用户{}的信誉衰减失败", user.getId(), e);
                }
            }
            
            log.info("不活跃用户信誉衰减任务完成，共处理{}个用户", processedCount);
        } catch (Exception e) {
            log.error("执行不活跃用户信誉衰减任务失败", e);
        }
    }
}
