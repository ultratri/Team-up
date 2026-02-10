package com.teamup.server.common.exception;

import com.teamup.server.common.api.Result;
import net.jqwik.api.*;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for GlobalExceptionHandler
 */
public class GlobalExceptionHandlerPropertyTest {

    /**
     * Property 26: API 错误响应格式
     * For any API error response, it should contain code and message fields
     * 
     * Feature: team-features-implementation, Property 26: API 错误响应格式
     * Validates: Requirements 8.1, 8.7
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 26: API 错误响应格式")
    void testApiErrorResponseFormat(@ForAll("errorMessages") String errorMessage) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        
        // Test BusinessException
        BusinessException businessException = new BusinessException(errorMessage);
        Result<?> result = handler.handle(businessException);
        
        // Verify response format
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getCode(), "Code should not be null");
        assertNotNull(result.getMessage(), "Message should not be null");
        assertNotNull(result.getTimestamp(), "Timestamp should not be null");
        assertTrue(result.getTimestamp() > 0, "Timestamp should be positive");
    }

    /**
     * Property 27: 统一响应格式
     * For any API response (success or failure), it should use unified Result format
     * with code, message, data, and timestamp fields
     * 
     * Feature: team-features-implementation, Property 27: 统一响应格式
     * Validates: Requirements 8.1, 8.7
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property 27: 统一响应格式")
    void testUnifiedResponseFormat(@ForAll("exceptionTypes") Exception exception) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Result<?> result;
        
        if (exception instanceof BusinessException) {
            result = handler.handle((BusinessException) exception);
        } else if (exception instanceof AccessDeniedException) {
            result = handler.handleAccessDeniedException((AccessDeniedException) exception);
        } else if (exception instanceof DataAccessException) {
            result = handler.handleDataAccessException((DataAccessException) exception);
        } else {
            result = handler.handle(exception);
        }
        
        // Verify unified format
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getCode(), "Code field should exist");
        assertNotNull(result.getMessage(), "Message field should exist");
        assertNotNull(result.getTimestamp(), "Timestamp field should exist");
        
        // Verify timestamp is recent (within last minute)
        long now = System.currentTimeMillis();
        assertTrue(result.getTimestamp() <= now, "Timestamp should not be in the future");
        assertTrue(result.getTimestamp() > now - 60000, "Timestamp should be recent");
    }

    /**
     * Property: Error codes should be appropriate for exception types
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property: Error codes match exception types")
    void testErrorCodesMatchExceptionTypes(@ForAll("errorMessages") String message) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        
        // Test AccessDeniedException returns 403
        AccessDeniedException accessDenied = new AccessDeniedException(message);
        Result<?> accessResult = handler.handleAccessDeniedException(accessDenied);
        assertEquals(403, accessResult.getCode(), "AccessDeniedException should return 403");
        
        // Test DataAccessException returns 500
        DataAccessException dataAccess = new TestDataAccessException(message);
        Result<?> dataResult = handler.handleDataAccessException(dataAccess);
        assertEquals(500, dataResult.getCode(), "DataAccessException should return 500");
        
        // Test generic Exception returns 500
        Exception generic = new Exception(message);
        Result<?> genericResult = handler.handle(generic);
        assertEquals(500, genericResult.getCode(), "Generic Exception should return 500");
    }

    /**
     * Property: Error messages should not be null or empty
     */
    @Property(tries = 100)
    @Label("Feature: team-features-implementation, Property: Error messages are not empty")
    void testErrorMessagesNotEmpty(@ForAll("exceptionTypes") Exception exception) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Result<?> result;
        
        if (exception instanceof BusinessException) {
            result = handler.handle((BusinessException) exception);
        } else if (exception instanceof AccessDeniedException) {
            result = handler.handleAccessDeniedException((AccessDeniedException) exception);
        } else if (exception instanceof DataAccessException) {
            result = handler.handleDataAccessException((DataAccessException) exception);
        } else {
            result = handler.handle(exception);
        }
        
        assertNotNull(result.getMessage(), "Error message should not be null");
        assertFalse(result.getMessage().isEmpty(), "Error message should not be empty");
    }

    // Arbitraries (generators)

    @Provide
    Arbitrary<String> errorMessages() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(100);
    }

    @Provide
    Arbitrary<Exception> exceptionTypes() {
        return Arbitraries.oneOf(
                errorMessages().map(BusinessException::new),
                errorMessages().map(AccessDeniedException::new),
                errorMessages().map(TestDataAccessException::new),
                errorMessages().map(Exception::new)
        );
    }

    // Test helper class
    private static class TestDataAccessException extends DataAccessException {
        public TestDataAccessException(String msg) {
            super(msg);
        }
    }
}
