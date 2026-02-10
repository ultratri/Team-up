package com.teamup.server.common;

import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Mock Database Utilities
 * 
 * Provides utilities for mocking database queries in tests.
 * Used for testing services and repositories that interact with the database.
 */
public class MockDatabaseUtils {

    /**
     * Creates a mock JdbcTemplate with configurable query results
     */
    public static JdbcTemplate createMockJdbcTemplate() {
        return Mockito.mock(JdbcTemplate.class);
    }

    /**
     * Creates a mock Connection
     */
    public static Connection createMockConnection() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement statement = Mockito.mock(PreparedStatement.class);
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        
        return connection;
    }

    /**
     * Creates a mock ResultSet with predefined data
     */
    public static ResultSet createMockResultSet() throws SQLException {
        return Mockito.mock(ResultSet.class);
    }

    /**
     * Configures a mock JdbcTemplate to return specific results for a query
     */
    public static <T> void mockQueryResult(
            JdbcTemplate jdbcTemplate,
            String sql,
            List<T> results,
            RowMapper<T> rowMapper) {
        when(jdbcTemplate.query(eq(sql), any(RowMapper.class)))
                .thenReturn(results);
    }

    /**
     * Configures a mock JdbcTemplate to return specific results for a query with parameters
     */
    public static <T> void mockQueryResultWithParams(
            JdbcTemplate jdbcTemplate,
            String sql,
            List<T> results) {
        when(jdbcTemplate.query(eq(sql), any(RowMapper.class), any()))
                .thenReturn(results);
    }

    /**
     * Configures a mock JdbcTemplate to throw an exception for a query
     */
    public static void mockQueryError(
            JdbcTemplate jdbcTemplate,
            String sql,
            Exception exception) {
        when(jdbcTemplate.query(eq(sql), any(RowMapper.class), any()))
                .thenThrow(exception);
    }

    /**
     * Simulates a database timeout
     */
    public static SQLException createTimeoutException() {
        return new SQLException("Query timeout", "HY000", 1205);
    }

    /**
     * Simulates a database connection error
     */
    public static SQLException createConnectionException() {
        return new SQLException("Connection failed", "08S01", 0);
    }

    /**
     * Simulates a SQL syntax error
     */
    public static SQLException createSyntaxException() {
        return new SQLException("SQL syntax error", "42000", 1064);
    }

    /**
     * Mock query execution time tracker
     */
    public static class QueryTimeTracker {
        private long startTime;
        private long endTime;

        public void start() {
            this.startTime = System.currentTimeMillis();
        }

        public void end() {
            this.endTime = System.currentTimeMillis();
        }

        public long getDuration() {
            return endTime - startTime;
        }

        public boolean exceeds(long thresholdMs) {
            return getDuration() > thresholdMs;
        }
    }

    /**
     * Simulates query execution with configurable delay
     */
    public static void simulateQueryDelay(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Query simulation interrupted", e);
        }
    }
}
