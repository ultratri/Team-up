package com.teamup.server.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Mock API Utilities
 * 
 * Provides utilities for mocking API calls and responses in tests.
 * Used for testing controllers and API endpoints.
 */
public class MockApiUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a mock GET request
     */
    public static MockHttpServletRequestBuilder createGetRequest(String url) {
        return get(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    /**
     * Creates a mock POST request with JSON body
     */
    public static MockHttpServletRequestBuilder createPostRequest(String url, Object body) throws Exception {
        return post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    /**
     * Creates a mock PUT request with JSON body
     */
    public static MockHttpServletRequestBuilder createPutRequest(String url, Object body) throws Exception {
        return put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    /**
     * Creates a mock DELETE request
     */
    public static MockHttpServletRequestBuilder createDeleteRequest(String url) {
        return delete(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    /**
     * Verifies a successful response with expected status and data
     */
    public static ResultActions verifySuccessResponse(
            ResultActions resultActions,
            HttpStatus expectedStatus) throws Exception {
        return resultActions
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Verifies an error response with expected status and error message
     */
    public static ResultActions verifyErrorResponse(
            ResultActions resultActions,
            HttpStatus expectedStatus,
            String expectedErrorCode) throws Exception {
        return resultActions
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value(expectedErrorCode));
    }

    /**
     * Creates a mock error response body
     */
    public static Map<String, Object> createErrorResponse(
            String errorCode,
            String message,
            String details) {
        Map<String, Object> error = new HashMap<>();
        error.put("errorCode", errorCode);
        error.put("message", message);
        error.put("details", details);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    /**
     * Creates a mock success response body
     */
    public static Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Adds authentication header to request
     */
    public static MockHttpServletRequestBuilder withAuth(
            MockHttpServletRequestBuilder request,
            String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    /**
     * Adds custom headers to request
     */
    public static MockHttpServletRequestBuilder withHeaders(
            MockHttpServletRequestBuilder request,
            Map<String, String> headers) {
        headers.forEach(request::header);
        return request;
    }

    /**
     * Simulates API delay for performance testing
     */
    public static void simulateApiDelay(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("API simulation interrupted", e);
        }
    }

    /**
     * Performance timer for measuring API response times
     */
    public static class ApiPerformanceTimer {
        private long startTime;
        private long endTime;

        public void start() {
            this.startTime = System.nanoTime();
        }

        public void end() {
            this.endTime = System.nanoTime();
        }

        public long getDurationMs() {
            return (endTime - startTime) / 1_000_000;
        }

        public boolean exceeds(long thresholdMs) {
            return getDurationMs() > thresholdMs;
        }
    }
}
