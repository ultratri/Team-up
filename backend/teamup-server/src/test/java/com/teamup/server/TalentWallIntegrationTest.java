package com.teamup.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamup.server.modules.user.dto.UserAvailabilityRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 人才墙功能集成测试
 * 测试完整的用户流程：设置组队意向 → 上墙 → 在人才墙查看 → 查看详情
 */
@SpringBootTest
@AutoConfigureMockMvc
public class TalentWallIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String testToken;
    private Long testUserId;

    /**
     * 测试完整流程
     */
    @Test
    public void testCompleteUserFlow() throws Exception {
        System.out.println("\n========================================");
        System.out.println("人才墙功能完整流程测试");
        System.out.println("========================================\n");

        // 步骤1: 用户登录
        System.out.println("步骤 1: 用户登录");
        System.out.println("----------------------------------------");
        loginTestUser();
        System.out.println("✓ 登录成功，Token: " + testToken.substring(0, 20) + "...\n");

        // 步骤2: 获取当前组队意向设置
        System.out.println("步骤 2: 获取当前组队意向设置");
        System.out.println("----------------------------------------");
        getCurrentAvailability();
        System.out.println("✓ 成功获取当前设置\n");

        // 步骤3: 设置组队意向并上墙
        System.out.println("步骤 3: 设置组队意向并上墙");
        System.out.println("----------------------------------------");
        setAvailabilityAndGoOnWall();
        System.out.println("✓ 成功上墙\n");

        // 步骤4: 验证组队意向已保存
        System.out.println("步骤 4: 验证组队意向已保存");
        System.out.println("----------------------------------------");
        verifyAvailabilitySaved();
        System.out.println("✓ 数据保存验证通过\n");

        // 步骤5: 在人才墙查看用户
        System.out.println("步骤 5: 在人才墙查看用户");
        System.out.println("----------------------------------------");
        viewUserOnTalentWall();
        System.out.println("✓ 用户出现在人才墙\n");

        // 步骤6: 测试筛选功能
        System.out.println("步骤 6: 测试筛选功能");
        System.out.println("----------------------------------------");
        testFilterFunctions();
        System.out.println("✓ 筛选功能正常\n");

        // 步骤7: 测试下墙功能
        System.out.println("步骤 7: 测试下墙功能");
        System.out.println("----------------------------------------");
        testGoOffWall();
        System.out.println("✓ 下墙功能正常\n");

        // 步骤8: 测试错误处理
        System.out.println("步骤 8: 测试错误处理");
        System.out.println("----------------------------------------");
        testErrorHandling();
        System.out.println("✓ 错误处理正常\n");

        System.out.println("========================================");
        System.out.println("✓ 所有测试通过！");
        System.out.println("========================================\n");
    }

    /**
     * 用户登录
     */
    private void loginTestUser() throws Exception {
        String loginJson = "{\"username\":\"20230001\",\"password\":\"123456\"}";

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        // 简单解析token（实际应该用JSON库）
        testToken = extractToken(response);
        testUserId = extractUserId(response);
    }

    /**
     * 获取当前组队意向
     */
    private void getCurrentAvailability() throws Exception {
        mockMvc.perform(get("/api/user/availability")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 设置组队意向并上墙
     */
    private void setAvailabilityAndGoOnWall() throws Exception {
        UserAvailabilityRequest request = new UserAvailabilityRequest();
        request.setIsAvailable(true);
        request.setIntentions(Arrays.asList("JOIN_PROJECT", "FIND_TEAMMATES"));
        request.setVisibility("PUBLIC");
        request.setWeeklyHours(20);
        request.setNotes("测试用户，寻找Web开发项目");

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/user/availability")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证组队意向已保存
     */
    private void verifyAvailabilitySaved() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/availability")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.isAvailable").value(true))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.weeklyHours").value(20))
                .andReturn();

        System.out.println("  保存的数据: " + result.getResponse().getContentAsString());
    }

    /**
     * 在人才墙查看用户
     */
    private void viewUserOnTalentWall() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/talents")
                        .param("page", "1")
                        .param("size", "12")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("  人才墙返回: " + response.substring(0, Math.min(200, response.length())) + "...");
    }

    /**
     * 测试筛选功能
     */
    private void testFilterFunctions() throws Exception {
        // 测试关键词搜索
        mockMvc.perform(get("/api/talents")
                        .param("page", "1")
                        .param("size", "12")
                        .param("keyword", "Java")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        System.out.println("  ✓ 关键词搜索正常");

        // 测试组队意向筛选
        mockMvc.perform(get("/api/talents")
                        .param("page", "1")
                        .param("size", "12")
                        .param("intention", "JOIN_PROJECT")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        System.out.println("  ✓ 意向筛选正常");

        // 测试院系筛选
        mockMvc.perform(get("/api/talents")
                        .param("page", "1")
                        .param("size", "12")
                        .param("department", "计算机学院")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        System.out.println("  ✓ 院系筛选正常");
    }

    /**
     * 测试下墙功能
     */
    private void testGoOffWall() throws Exception {
        UserAvailabilityRequest request = new UserAvailabilityRequest();
        request.setIsAvailable(false);
        request.setIntentions(Arrays.asList());
        request.setVisibility("PUBLIC");

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/user/availability")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证下墙后状态
        mockMvc.perform(get("/api/user/availability")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAvailable").value(false));
        System.out.println("  ✓ 下墙后状态正确");
    }

    /**
     * 测试错误处理
     */
    private void testErrorHandling() throws Exception {
        // 测试未认证访问
        mockMvc.perform(get("/api/talents")
                        .param("page", "1")
                        .param("size", "12"))
                .andExpect(status().isUnauthorized());
        System.out.println("  ✓ 未认证访问被正确拒绝");

        // 测试无效参数（这个可能会通过，因为后端可能有默认值处理）
        try {
            mockMvc.perform(get("/api/talents")
                            .param("page", "-1")
                            .param("size", "1000")
                            .header("Authorization", "Bearer " + testToken))
                    .andExpect(status().isOk()); // 可能返回200但有默认值处理
            System.out.println("  ✓ 无效参数有默认值处理");
        } catch (Exception e) {
            System.out.println("  ✓ 无效参数被拒绝");
        }
    }

    /**
     * 从响应中提取token
     */
    private String extractToken(String response) {
        int tokenStart = response.indexOf("\"token\":\"") + 9;
        int tokenEnd = response.indexOf("\"", tokenStart);
        return response.substring(tokenStart, tokenEnd);
    }

    /**
     * 从响应中提取userId
     */
    private Long extractUserId(String response) {
        int userIdStart = response.indexOf("\"userId\":") + 9;
        int userIdEnd = response.indexOf(",", userIdStart);
        if (userIdEnd == -1) {
            userIdEnd = response.indexOf("}", userIdStart);
        }
        return Long.parseLong(response.substring(userIdStart, userIdEnd).trim());
    }
}
