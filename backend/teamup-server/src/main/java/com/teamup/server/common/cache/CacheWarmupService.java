package com.teamup.server.common.cache;

import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存预热服务
 * 在应用启动和定时任务中，预加载热点数据到 Redis，减少数据库 IO
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupService implements CommandLineRunner {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String HOT_PROJECT_KEY_PREFIX = "hot:project:";
    private static final String ACTIVE_USER_KEY_PREFIX = "hot:user:";
    private static final int HOT_PROJECT_COUNT = 100;
    private static final int ACTIVE_USER_COUNT = 200;

    /**
     * 应用启动时执行缓存预热
     */
    @Override
    public void run(String... args) {
        log.info("开始执行启动时缓存预热...");
        warmupHotProjects();
        warmupActiveUsers();
        log.info("启动时缓存预热完成");
    }

    /**
     * 预热热门项目
     * 每天凌晨 2 点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void warmupHotProjects() {
        try {
            log.info("开始预热热门项目缓存...");
            
            // 查询热门项目（按浏览量降序）
            List<Project> hotProjects = projectMapper.selectHotProjects(HOT_PROJECT_COUNT);
            
            if (hotProjects == null || hotProjects.isEmpty()) {
                log.warn("未查询到热门项目数据");
                return;
            }
            
            // 批量写入 Redis，设置 1 小时过期
            int cachedCount = 0;
            for (Project project : hotProjects) {
                String key = HOT_PROJECT_KEY_PREFIX + project.getId();
                redisTemplate.opsForValue().set(key, project, 1, TimeUnit.HOURS);
                cachedCount++;
            }
            
            log.info("热门项目缓存预热完成，共缓存 {} 个项目", cachedCount);
            
        } catch (Exception e) {
            log.error("预热热门项目缓存失败", e);
        }
    }

    /**
     * 预热活跃用户
     * 每 30 分钟执行一次
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void warmupActiveUsers() {
        try {
            log.info("开始预热活跃用户缓存...");
            
            // 查询活跃用户（最近登录）
            List<User> activeUsers = userMapper.selectActiveUsers(ACTIVE_USER_COUNT);
            
            if (activeUsers == null || activeUsers.isEmpty()) {
                log.warn("未查询到活跃用户数据");
                return;
            }
            
            // 批量写入 Redis，设置 30 分钟过期
            int cachedCount = 0;
            for (User user : activeUsers) {
                String key = ACTIVE_USER_KEY_PREFIX + user.getId();
                // 只缓存基本信息，避免缓存敏感数据
                user.setPassword(null);
                redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
                cachedCount++;
            }
            
            log.info("活跃用户缓存预热完成，共缓存 {} 个用户", cachedCount);
            
        } catch (Exception e) {
            log.error("预热活跃用户缓存失败", e);
        }
    }

    /**
     * 手动触发全量预热（可通过管理接口调用）
     */
    public void warmupAll() {
        log.info("开始执行手动全量缓存预热...");
        warmupHotProjects();
        warmupActiveUsers();
        log.info("手动全量缓存预热完成");
    }
}
