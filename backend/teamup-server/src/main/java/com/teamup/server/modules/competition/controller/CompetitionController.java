package com.teamup.server.modules.competition.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.service.CompetitionService;
import com.teamup.server.modules.file.service.FileStorageService;
import com.teamup.server.modules.notification.service.NotificationService;
import com.teamup.server.modules.team.dto.TeamCreateRequest;
import com.teamup.server.modules.team.entity.Team;
import com.teamup.server.common.audit.AuditLogService;
import com.teamup.server.modules.team.service.TeamService;
import com.teamup.server.modules.team.mapper.TeamMemberMapper;
import com.teamup.server.modules.team.mapper.TeamMapper;
import com.teamup.server.modules.competition.vo.CompetitionStatsVO;
import com.teamup.server.modules.competition.vo.CompetitionLeaderboardEntryVO;
import com.teamup.server.modules.competition.mapper.TeamCompetitionScoreMapper;
import com.teamup.server.modules.user.mapper.UserMapper;
import com.teamup.server.modules.user.mapper.UserSkillMapper;
import com.teamup.server.modules.user.entity.UserSkill;
import com.teamup.server.modules.user.security.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 比赛管理接口（基础版）
 *
 * 后续可根据需要增加更细权限控制（如仅管理员/教务可写）
 */
@RestController
@RequestMapping("/api/competitions")
@RequiredArgsConstructor
public class CompetitionController {

    private static final Logger log = LoggerFactory.getLogger(CompetitionController.class);
    private final CompetitionService competitionService;
    private final TeamService teamService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamMapper teamMapper;
    private final AuditLogService auditLogService;
    private final UserSkillMapper userSkillMapper;
    private final TeamCompetitionScoreMapper teamCompetitionScoreMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String viewKey(Long competitionId) {
        return "competition:view:" + competitionId;
    }

    private String dailyViewKey(Long competitionId, java.time.LocalDate day) {
        return "competition:view:daily:" + competitionId + ":" + day.toString();
    }

    private String viewRankKey() {
        return "competition:views:rank";
    }

    /**
     * 比赛列表（分页 + 简单筛选）
     */
    @GetMapping
    public Result<Page<Competition>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        long start = System.currentTimeMillis();
        Page<Competition> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(Competition::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Competition::getName, keyword);
        }
        wrapper.orderByDesc(Competition::getSignupStartAt);

        Page<Competition> result = competitionService.page(pageParam, wrapper);
        long cost = System.currentTimeMillis() - start;
        log.info("metrics|endpoint=competitions.list page={} size={} status={} keyword={} costMs={}",
                page, size, status, keyword, cost);
        return Result.success(result);
    }

    /**
     * 比赛推荐（登录用户）
     * 简单版：取用户技能作为关键词，对比赛名称/主办方/类型/描述做 like 匹配，返回前 N 条已发布比赛
     */
    @GetMapping("/recommendations")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<Competition>> recommend(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        long start = System.currentTimeMillis();
        Long userId = UserContext.getCurrentUserId();
        Page<Competition> pageParam = new Page<>(page, size);

        List<UserSkill> skills = userSkillMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
        );

        // 没技能时返回空（避免给不相关推荐）
        if (skills == null || skills.isEmpty()) {
            long cost = System.currentTimeMillis() - start;
            log.info("metrics|endpoint=competitions.recommendations userId={} page={} size={} emptySkills costMs={}",
                    userId, page, size, cost);
            return Result.success(new Page<>(page, size, 0));
        }

        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Competition::getStatus, "PUBLISHED");

        wrapper.and(w -> {
            boolean first = true;
            for (UserSkill s : skills) {
                if (s == null || !StringUtils.hasText(s.getSkillName())) continue;
                String kw = s.getSkillName();
                if (first) {
                    w.like(Competition::getName, kw)
                            .or().like(Competition::getOrganizer, kw)
                            .or().like(Competition::getType, kw)
                            .or().like(Competition::getDescription, kw);
                    first = false;
                } else {
                    w.or().like(Competition::getName, kw)
                            .or().like(Competition::getOrganizer, kw)
                            .or().like(Competition::getType, kw)
                            .or().like(Competition::getDescription, kw);
                }
            }
        });

        wrapper.orderByDesc(Competition::getSignupStartAt);
        Page<Competition> result = competitionService.page(pageParam, wrapper);
        long cost = System.currentTimeMillis() - start;
        log.info("metrics|endpoint=competitions.recommendations userId={} page={} size={} resultCount={} costMs={}",
                userId, page, size, result.getTotal(), cost);
        return Result.success(result);
    }

    /**
     * 比赛详情
     */
    @GetMapping("/{id}")
    public Result<Competition> detail(@PathVariable Long id) {
        Competition competition = competitionService.getById(id);
        if (competition == null) {
            return Result.error(404, "比赛不存在");
        }
        // 记录浏览量（Redis不可用时自动降级）
        try {
            redisTemplate.opsForValue().increment(viewKey(id), 1);
            // 同时维护热度榜（ZSET）
            redisTemplate.opsForZSet().incrementScore(viewRankKey(), String.valueOf(id), 1);
            // 按天记录浏览量
            java.time.LocalDate today = java.time.LocalDate.now();
            redisTemplate.opsForValue().increment(dailyViewKey(id, today), 1);
        } catch (Exception ignored) {
        }
        return Result.success(competition);
    }

    /**
     * 热门比赛（基于浏览量 ZSET）
     */
    @GetMapping("/hot")
    public Result<List<Competition>> hot(@RequestParam(defaultValue = "6") int size) {
        long start = System.currentTimeMillis();
        if (size < 1) size = 6;
        if (size > 50) size = 50;

        try {
            var tuples = redisTemplate.opsForZSet().reverseRangeWithScores(viewRankKey(), 0, size - 1);
            if (tuples == null || tuples.isEmpty()) {
                long cost = System.currentTimeMillis() - start;
                log.info("metrics|endpoint=competitions.hot size={} resultCount=0 costMs={} (empty zset)", size, cost);
                return Result.success(List.of());
            }
            List<Long> ids = new ArrayList<>();
            for (var t : tuples) {
                if (t == null || t.getValue() == null) continue;
                try {
                    ids.add(Long.valueOf(String.valueOf(t.getValue())));
                } catch (Exception ignored) {
                }
            }
            if (ids.isEmpty()) {
                long cost = System.currentTimeMillis() - start;
                log.info("metrics|endpoint=competitions.hot size={} resultCount=0 costMs={} (no ids)", size, cost);
                return Result.success(List.of());
            }

            // listByIds 不保证顺序，这里按 ids 顺序重排
            List<Competition> comps = competitionService.listByIds(ids);
            Map<Long, Competition> map = new HashMap<>();
            for (Competition c : comps) {
                map.put(c.getId(), c);
            }
            List<Competition> ordered = new ArrayList<>();
            for (Long id : ids) {
                Competition c = map.get(id);
                if (c != null && "PUBLISHED".equals(c.getStatus())) {
                    ordered.add(c);
                }
            }
            long cost = System.currentTimeMillis() - start;
            log.info("metrics|endpoint=competitions.hot size={} resultCount={} costMs={}",
                    size, ordered.size(), cost);
            return Result.success(ordered);
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("metrics|endpoint=competitions.hot size={} error={} costMs={}", size, e.getMessage(), cost);
            return Result.success(List.of());
        }
    }

    /**
     * 比赛统计（公开可读）
     */
    @GetMapping("/{id}/stats")
    public Result<CompetitionStatsVO> stats(@PathVariable Long id) {
        Competition competition = competitionService.getById(id);
        if (competition == null) {
            return Result.error(404, "比赛不存在");
        }

        long teamCount = teamService.count(new LambdaQueryWrapper<Team>().eq(Team::getCompetitionId, id));
        long mentorTeamCount = teamService.count(
                new LambdaQueryWrapper<Team>()
                        .eq(Team::getCompetitionId, id)
                        .isNotNull(Team::getMentorId)
        );
        Long memberCount = teamMemberMapper.countMembersByCompetitionId(id);
        if (memberCount == null) memberCount = 0L;

        CompetitionStatsVO vo = new CompetitionStatsVO();
        vo.setCompetitionId(id);
        vo.setTeamCount(teamCount);
        vo.setMentorTeamCount(mentorTeamCount);
        vo.setMemberCount(memberCount);
        vo.setMentorCoverageRate(teamCount == 0 ? 0 : (int) ((mentorTeamCount * 100) / teamCount));
        try {
            Object v = redisTemplate.opsForValue().get(viewKey(id));
            long viewCount = v == null ? 0L : Long.parseLong(String.valueOf(v));
            vo.setViewCount(viewCount);
        } catch (Exception e) {
            vo.setViewCount(0L);
        }
        return Result.success(vo);
    }

    /**
     * 比赛时间维度趋势数据（最近 N 天）
     */
    @GetMapping("/{id}/stats/trend")
    public Result<java.util.List<java.util.Map<String, Object>>> trend(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") int days
    ) {
        if (days <= 0) days = 7;
        if (days > 60) days = 60;

        Competition competition = competitionService.getById(id);
        if (competition == null) {
            return Result.error(404, "比赛不存在");
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        // 预先按天初始化
        for (int i = days - 1; i >= 0; i--) {
            java.time.LocalDate day = today.minusDays(i);
            java.util.Map<String, Object> row = new java.util.HashMap<>();
            row.put("date", day.toString());
            row.put("teamCount", 0L);
            row.put("memberCount", 0L);
            row.put("viewCount", 0L);
            result.add(row);
        }

        // 队伍数按天聚合
        java.util.List<java.util.Map<String, Object>> teamRows = teamMapper.aggregateDailyTeamCount(id, days);
        for (java.util.Map<String, Object> r : teamRows) {
            Object dayObj = r.get("day");
            Object cntObj = r.get("teamCount");
            if (dayObj == null || cntObj == null) continue;
            String dayStr = String.valueOf(dayObj);
            long cnt = Long.parseLong(String.valueOf(cntObj));
            result.stream()
                    .filter(row -> dayStr.equals(row.get("date")))
                    .findFirst()
                    .ifPresent(row -> row.put("teamCount", cnt));
        }

        // 成员数按天聚合
        java.util.List<java.util.Map<String, Object>> memberRows = teamMemberMapper.aggregateDailyMemberCount(id, days);
        for (java.util.Map<String, Object> r : memberRows) {
            Object dayObj = r.get("day");
            Object cntObj = r.get("memberCount");
            if (dayObj == null || cntObj == null) continue;
            String dayStr = String.valueOf(dayObj);
            long cnt = Long.parseLong(String.valueOf(cntObj));
            result.stream()
                    .filter(row -> dayStr.equals(row.get("date")))
                    .findFirst()
                    .ifPresent(row -> row.put("memberCount", cnt));
        }

        // 浏览量按天聚合（Redis：competition:view:daily:{id}:{yyyy-MM-dd}）
        for (java.util.Map<String, Object> row : result) {
            String dayStr = String.valueOf(row.get("date"));
            try {
                String key = "competition:view:daily:" + id + ":" + dayStr;
                Object v = redisTemplate.opsForValue().get(key);
                long viewCount = v == null ? 0L : Long.parseLong(String.valueOf(v));
                row.put("viewCount", viewCount);
            } catch (Exception ignored) {
            }
        }

        return Result.success(result);
    }

    /**
     * 比赛排行榜（基于队伍任务完成度）
     */
    @GetMapping("/{id}/leaderboard")
    public Result<List<CompetitionLeaderboardEntryVO>> leaderboard(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (limit < 1) limit = 10;
        if (limit > 100) limit = 100;

        Competition competition = competitionService.getById(id);
        if (competition == null) {
            return Result.error(404, "比赛不存在");
        }

        // 优先：如果存在评分记录，则按评分输出（导师/管理员录入）
        List<Map<String, Object>> scoreRows = teamCompetitionScoreMapper.selectScoreLeaderboard(id, limit);
        if (scoreRows != null && !scoreRows.isEmpty()) {
            List<CompetitionLeaderboardEntryVO> scored = new ArrayList<>();
            for (Map<String, Object> r : scoreRows) {
                CompetitionLeaderboardEntryVO vo = new CompetitionLeaderboardEntryVO();
                vo.setTeamId(r.get("teamId") != null ? Long.valueOf(String.valueOf(r.get("teamId"))) : null);
                vo.setTeamName(r.get("teamName") != null ? String.valueOf(r.get("teamName")) : null);
                long memberCount = r.get("memberCount") != null ? Long.parseLong(String.valueOf(r.get("memberCount"))) : 0L;
                boolean hasMentor = r.get("hasMentor") != null && !"0".equals(String.valueOf(r.get("hasMentor")));
                vo.setMemberCount(memberCount);
                vo.setHasMentor(hasMentor);
                vo.setTotalTasks(0L);
                vo.setDoneTasks(0L);
                vo.setCompletionRate(0);
                if (r.get("score") != null) {
                    try {
                        vo.setScore(new java.math.BigDecimal(String.valueOf(r.get("score"))));
                    } catch (Exception ignored) {
                    }
                }
                vo.setComment(r.get("comment") != null ? String.valueOf(r.get("comment")) : null);
                if (r.get("scoredBy") != null) {
                    try {
                        vo.setScoredBy(Long.valueOf(String.valueOf(r.get("scoredBy"))));
                    } catch (Exception ignored) {
                    }
                }
                vo.setScoredByName(r.get("scoredByName") != null ? String.valueOf(r.get("scoredByName")) : null);
                scored.add(vo);
            }
            return Result.success(scored);
        }

        List<Map<String, Object>> rows = teamMapper.selectCompetitionLeaderboard(id, limit);
        List<CompetitionLeaderboardEntryVO> result = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                CompetitionLeaderboardEntryVO vo = new CompetitionLeaderboardEntryVO();
                vo.setTeamId(r.get("teamId") != null ? Long.valueOf(String.valueOf(r.get("teamId"))) : null);
                vo.setTeamName(r.get("teamName") != null ? String.valueOf(r.get("teamName")) : null);
                long memberCount = r.get("memberCount") != null ? Long.parseLong(String.valueOf(r.get("memberCount"))) : 0L;
                long totalTasks = r.get("totalTasks") != null ? Long.parseLong(String.valueOf(r.get("totalTasks"))) : 0L;
                long doneTasks = r.get("doneTasks") != null ? Long.parseLong(String.valueOf(r.get("doneTasks"))) : 0L;
                boolean hasMentor = r.get("hasMentor") != null && !"0".equals(String.valueOf(r.get("hasMentor")));

                vo.setMemberCount(memberCount);
                vo.setTotalTasks(totalTasks);
                vo.setDoneTasks(doneTasks);
                vo.setHasMentor(hasMentor);
                vo.setCompletionRate(totalTasks == 0 ? 0 : (int) ((doneTasks * 100) / totalTasks));
                result.add(vo);
            }
        }
        return Result.success(result);
    }

    /**
     * 创建比赛（默认仅允许已登录用户，后续可加角色判断）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<Competition> create(@RequestBody Competition competition) {
        Long userId = UserContext.getCurrentUserId();
        competition.setCreatedBy(userId);
        competition.setStatus(competition.getStatus() == null ? "DRAFT" : competition.getStatus());
        competitionService.save(competition);
        auditLogService.logSensitiveOperation(
                "CREATE_COMPETITION",
                "COMPETITION",
                competition.getId(),
                "创建比赛：" + competition.getName(),
                "SUCCESS",
                null
        );
        return Result.success(competition);
    }

    /**
     * 更新比赛基础信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<Competition> update(@PathVariable Long id, @RequestBody Competition payload) {
        Competition existing = competitionService.getById(id);
        if (existing == null) {
            return Result.error(404, "比赛不存在");
        }
        if (StringUtils.hasText(payload.getName())) {
            existing.setName(payload.getName());
        }
        if (payload.getOrganizer() != null) {
            existing.setOrganizer(payload.getOrganizer());
        }
        if (payload.getLevel() != null) {
            existing.setLevel(payload.getLevel());
        }
        if (payload.getScope() != null) {
            existing.setScope(payload.getScope());
        }
        if (payload.getType() != null) {
            existing.setType(payload.getType());
        }
        if (payload.getSignupStartAt() != null) {
            existing.setSignupStartAt(payload.getSignupStartAt());
        }
        if (payload.getSignupEndAt() != null) {
            existing.setSignupEndAt(payload.getSignupEndAt());
        }
        if (payload.getStartAt() != null) {
            existing.setStartAt(payload.getStartAt());
        }
        if (payload.getEndAt() != null) {
            existing.setEndAt(payload.getEndAt());
        }
        if (payload.getMaxTeamMembers() != null) {
            existing.setMaxTeamMembers(payload.getMaxTeamMembers());
        }
        if (payload.getMinTeamMembers() != null) {
            existing.setMinTeamMembers(payload.getMinTeamMembers());
        }
        if (payload.getRequireMentor() != null) {
            existing.setRequireMentor(payload.getRequireMentor());
        }
        if (payload.getDescription() != null) {
            existing.setDescription(payload.getDescription());
        }
        if (payload.getAttachments() != null) {
            existing.setAttachments(payload.getAttachments());
        }

        competitionService.updateById(existing);
        auditLogService.logSensitiveOperation(
                "UPDATE_COMPETITION",
                "COMPETITION",
                existing.getId(),
                "更新比赛：" + existing.getName(),
                "SUCCESS",
                null
        );
        return Result.success(existing);
    }

    /**
     * 发布比赛（状态置为 PUBLISHED）
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<Competition> publish(@PathVariable Long id) {
        Competition competition = competitionService.getById(id);
        if (competition == null) {
            return Result.error(404, "比赛不存在");
        }
        competition.setStatus("PUBLISHED");
        competitionService.updateById(competition);
        auditLogService.logSensitiveOperation(
                "PUBLISH_COMPETITION",
                "COMPETITION",
                competition.getId(),
                "发布比赛：" + competition.getName(),
                "SUCCESS",
                null
        );

        // 通知：目前采用“活跃用户”简化策略，audience 仅作为配置预留
        try {
            java.util.List<Long> userIds = userMapper.selectActiveUserIds(500);
            if (userIds != null && !userIds.isEmpty()) {
                notificationService.createBatchNotifications(
                        userIds,
                        "COMPETITION_PUBLISHED",
                        "新比赛发布：" + competition.getName(),
                        "报名时间：" + competition.getSignupStartAt() + " - " + competition.getSignupEndAt(),
                        "COMPETITION",
                        competition.getId()
                );
            }
        } catch (Exception ignored) {
            // 失败不影响发布
        }
        return Result.success(competition);
    }

    /**
     * 归档比赛（状态置为 ARCHIVED）
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<Competition> archive(@PathVariable Long id) {
        Competition competition = competitionService.getById(id);
        if (competition == null) {
            return Result.error(404, "比赛不存在");
        }
        competition.setStatus("ARCHIVED");
        competitionService.updateById(competition);
        auditLogService.logSensitiveOperation(
                "ARCHIVE_COMPETITION",
                "COMPETITION",
                competition.getId(),
                "归档比赛：" + competition.getName(),
                "SUCCESS",
                null
        );
        return Result.success(competition);
    }

    /**
     * 删除比赛（管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        Competition competition = competitionService.getById(id);
        if (competition == null) {
            return Result.error(404, "比赛不存在");
        }
        competitionService.removeById(id);
        auditLogService.logSensitiveOperation(
                "DELETE_COMPETITION",
                "COMPETITION",
                id,
                "删除比赛：" + competition.getName(),
                "SUCCESS",
                null
        );
        return Result.success();
    }

    /**
     * 获取某个比赛下的队伍列表（分页）
     */
    @GetMapping("/{id}/teams")
    public Result<Page<Team>> listTeams(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        long start = System.currentTimeMillis();
        Page<Team> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Team::getCompetitionId, id);
        wrapper.orderByDesc(Team::getCreatedAt);
        Page<Team> result = teamService.page(pageParam, wrapper);
        long cost = System.currentTimeMillis() - start;
        log.info("metrics|endpoint=competitions.teams competitionId={} page={} size={} resultCount={} costMs={}",
                id, page, size, result.getTotal(), cost);
        return Result.success(result);
    }

    /**
     * 在比赛下创建一支新队伍（仍要求前端提供对应的 projectId）
     */
    @PostMapping("/{id}/teams")
    public Result<Team> createTeamForCompetition(
            @PathVariable Long id,
            @RequestBody TeamCreateRequest request
    ) {
        Long userId = UserContext.getCurrentUserId();
        request.setLeaderId(userId);
        request.setType(request.getType() != null ? request.getType() : "COMPETITION");
        request.setCompetitionId(id);
        Team team = teamService.createTeam(request);
        return Result.success(team);
    }

    /**
     * 上传比赛附件
     */
    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<Map<String, String>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        Competition competition = competitionService.getById(id);
        if (competition == null) {
            return Result.error(404, "比赛不存在");
        }

        try {
            // 上传文件到 public/competition 目录
            String fileUrl = fileStorageService.uploadFile(file, "competition");
            String fileName = file.getOriginalFilename();

            // 解析现有附件列表
            List<Map<String, String>> attachments = new ArrayList<>();
            if (StringUtils.hasText(competition.getAttachments())) {
                try {
                    attachments = objectMapper.readValue(
                            competition.getAttachments(),
                            new TypeReference<List<Map<String, String>>>() {}
                    );
                } catch (Exception e) {
                    // JSON解析失败，使用空列表
                }
            }

            // 添加新附件
            Map<String, String> newAttachment = new HashMap<>();
            newAttachment.put("name", fileName);
            newAttachment.put("url", fileUrl);
            attachments.add(newAttachment);

            // 保存附件列表
            competition.setAttachments(objectMapper.writeValueAsString(attachments));
            competitionService.updateById(competition);
            auditLogService.logSensitiveOperation(
                    "UPLOAD_COMPETITION_ATTACHMENT",
                    "COMPETITION",
                    competition.getId(),
                    "上传比赛附件：" + fileName,
                    "SUCCESS",
                    null
            );

            Map<String, String> result = new HashMap<>();
            result.put("name", fileName);
            result.put("url", fileUrl);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除比赛附件
     */
    @DeleteMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MENTOR')")
    public Result<Void> deleteAttachment(
            @PathVariable Long id,
            @RequestParam String url
    ) {
        Competition competition = competitionService.getById(id);
        if (competition == null) {
            return Result.error(404, "比赛不存在");
        }

        try {
            // 解析现有附件列表
            List<Map<String, String>> attachments = new ArrayList<>();
            if (StringUtils.hasText(competition.getAttachments())) {
                try {
                    attachments = objectMapper.readValue(
                            competition.getAttachments(),
                            new TypeReference<List<Map<String, String>>>() {}
                    );
                } catch (Exception e) {
                    return Result.error(400, "附件列表格式错误");
                }
            }

            // 删除指定附件
            attachments.removeIf(att -> url.equals(att.get("url")));

            // 删除物理文件
            try {
                fileStorageService.deleteFile(url);
            } catch (Exception e) {
                // 文件删除失败不影响数据库更新
            }

            // 保存更新后的附件列表
            competition.setAttachments(objectMapper.writeValueAsString(attachments));
            competitionService.updateById(competition);
            auditLogService.logSensitiveOperation(
                    "DELETE_COMPETITION_ATTACHMENT",
                    "COMPETITION",
                    competition.getId(),
                    "删除比赛附件：" + url,
                    "SUCCESS",
                    null
            );

            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "删除附件失败: " + e.getMessage());
        }
    }
}

