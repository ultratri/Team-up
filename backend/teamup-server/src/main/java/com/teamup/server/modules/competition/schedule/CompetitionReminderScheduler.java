package com.teamup.server.modules.competition.schedule;

import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.mapper.CompetitionMapper;
import com.teamup.server.modules.notification.service.NotificationService;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 比赛提醒定时任务（简单版）
 * - 报名截止前 3 天提醒
 * - 比赛开始前 1 天提醒
 *
 * 为避免重复发送，使用 Redis 做幂等标记（按天）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionReminderScheduler {

    private final CompetitionMapper competitionMapper;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int NOTIFY_USER_LIMIT = 500;

    @Scheduled(cron = "0 0 9 * * ?")
    public void remindSignupEndingSoon() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(3);
        List<Competition> competitions = competitionMapper.selectSignupEndingSoon(now, end);
        if (competitions == null || competitions.isEmpty()) return;

        List<Long> userIds = userMapper.selectActiveUserIds(NOTIFY_USER_LIMIT);
        if (userIds == null || userIds.isEmpty()) return;

        for (Competition c : competitions) {
            String key = buildDailyKey("signup_end", c.getId());
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) continue;

            try {
                notificationService.createBatchNotifications(
                        userIds,
                        "COMPETITION_SIGNUP_ENDING_SOON",
                        "报名即将截止：" + c.getName(),
                        "报名截止时间：" + c.getSignupEndAt(),
                        "COMPETITION",
                        c.getId()
                );
                redisTemplate.opsForValue().set(key, 1, 36, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Failed to send signup ending soon reminder for competition {}: {}", c.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 30 9 * * ?")
    public void remindStartingSoon() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(1);
        List<Competition> competitions = competitionMapper.selectStartingSoon(now, end);
        if (competitions == null || competitions.isEmpty()) return;

        List<Long> userIds = userMapper.selectActiveUserIds(NOTIFY_USER_LIMIT);
        if (userIds == null || userIds.isEmpty()) return;

        for (Competition c : competitions) {
            String key = buildDailyKey("start", c.getId());
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) continue;

            try {
                notificationService.createBatchNotifications(
                        userIds,
                        "COMPETITION_STARTING_SOON",
                        "比赛即将开始：" + c.getName(),
                        "开始时间：" + c.getStartAt(),
                        "COMPETITION",
                        c.getId()
                );
                redisTemplate.opsForValue().set(key, 1, 36, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Failed to send starting soon reminder for competition {}: {}", c.getId(), e.getMessage());
            }
        }
    }

    private String buildDailyKey(String kind, Long competitionId) {
        LocalDate today = LocalDate.now();
        return "reminder:competition:" + kind + ":" + competitionId + ":" + today;
    }
}

