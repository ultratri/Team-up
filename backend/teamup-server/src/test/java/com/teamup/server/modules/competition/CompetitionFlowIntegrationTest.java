package com.teamup.server.modules.competition;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.competition.controller.CompetitionController;
import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.vo.CompetitionLeaderboardEntryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 比赛模块关键流程集成测试（基础版）
 *
 * 覆盖范围：
 * - 比赛列表 & 详情
 * - 热门比赛
 * - 排行榜
 *
 * 说明：
 * - 为避免对真实数据有强依赖，这里主要校验接口可正常返回、不抛异常。
 * - 具体业务断言可在后续有稳定测试数据后逐步补充。
 */
@SpringBootTest
@ActiveProfiles("test")
public class CompetitionFlowIntegrationTest {

    @Autowired
    private CompetitionController competitionController;

    @Test
    void testListCompetitions_basicFlow() {
        Result<Page<Competition>> result = competitionController.list(1, 10, null, null);
        assertNotNull(result, "比赛列表返回结果不应为 null");
        // 允许 200 或无数据等情况，这里只要调用成功即认为通过基础流转
        assertTrue(result.getCode() == 200 || result.getCode() == 0);
    }

    @Test
    void testHotCompetitions_noException() {
        Result<List<Competition>> result = competitionController.hot(6);
        assertNotNull(result, "热门比赛返回结果不应为 null");
    }

    @Test
    void testLeaderboard_safeCall() {
        // 没有固定比赛 ID，这里选用一个常见的 ID=1，若不存在则期望返回 404
        Result<List<CompetitionLeaderboardEntryVO>> result = competitionController.leaderboard(1L, 10);
        assertNotNull(result, "排行榜接口返回结果不应为 null");
        assertTrue(result.getCode() == 200 || result.getCode() == 404);
    }
}

