package com.teamup.server.common;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

/**
 * Base Property Test Class
 * 
 * Provides common setup and utilities for property-based tests.
 * All property tests should extend this class to ensure consistent configuration.
 * 
 * Configuration:
 * - Minimum 100 iterations per property test (configured in jqwik.properties)
 * - Automatic shrinking enabled for failed test cases
 * - Edge cases included in generation
 */
public abstract class BasePropertyTest {

    /**
     * Setup method called before each property test
     */
    @BeforeEach
    public void setUp() {
        // Common setup for all property tests
        // Override in subclasses if needed
    }

    /**
     * Cleanup method called after each property test
     */
    @AfterEach
    public void tearDown() {
        // Common cleanup for all property tests
        // Override in subclasses if needed
    }

    /**
     * Helper method to measure execution time
     */
    protected long measureExecutionTime(Runnable operation) {
        long startTime = System.nanoTime();
        operation.run();
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    /**
     * Helper method to verify performance threshold
     */
    protected boolean meetsPerformanceThreshold(long durationMs, long thresholdMs) {
        return durationMs <= thresholdMs;
    }

    /**
     * Helper method to simulate database query execution
     */
    protected void simulateQueryExecution(long durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Query simulation interrupted", e);
        }
    }
}
