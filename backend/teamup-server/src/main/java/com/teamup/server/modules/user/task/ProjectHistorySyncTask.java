package com.teamup.server.modules.user.task;

import com.teamup.server.modules.user.service.ProjectHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 项目履历同步定时任务
 * 每天凌晨2点执行，同步所有项目履历数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectHistorySyncTask {
    
    private final ProjectHistoryService projectHistoryService;
    
    /**
     * 同步项目履历
     * 更新评价分数、计算参与天数等
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncProjectHistory() {
        log.info("开始执行项目履历同步任务");
        
        try {
            projectHistoryService.syncAllProjectHistory();
            log.info("项目履历同步任务执行成功");
        } catch (Exception e) {
            log.error("项目履历同步任务执行失败", e);
        }
    }
}
