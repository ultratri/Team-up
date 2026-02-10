package com.teamup.server.common;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-Based Testing Setup Verification
 * 
 * This test verifies that the property-based testing infrastructure is correctly configured.
 * Tests should run with minimum 100 iterations as configured in jqwik.properties.
 */
public class PropertyTestSetupTest extends BasePropertyTest {

    @Property
    @Label("Setup Test: String reversal property")
    void stringReversalProperty(@ForAll @StringLength(max = 100) String str) {
        // Property: reversing a string twice returns the original string
        String reversed = new StringBuilder(str).reverse().toString();
        String doubleReversed = new StringBuilder(reversed).reverse().toString();
        
        assertThat(doubleReversed).isEqualTo(str);
    }

    @Property
    @Label("Setup Test: Integer addition commutativity")
    void integerAdditionCommutativity(
            @ForAll @IntRange(min = -1000, max = 1000) int a,
            @ForAll @IntRange(min = -1000, max = 1000) int b) {
        // Property: a + b = b + a
        assertThat(a + b).isEqualTo(b + a);
    }

    @Property
    @Label("Setup Test: List size preservation")
    void listSizePreservation(@ForAll("stringLists") java.util.List<String> list) {
        // Property: mapping a list preserves its size
        int originalSize = list.size();
        java.util.List<Integer> mapped = list.stream()
                .map(String::length)
                .collect(java.util.stream.Collectors.toList());
        
        assertThat(mapped.size()).isEqualTo(originalSize);
    }

    @Provide
    Arbitrary<java.util.List<String>> stringLists() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(0)
                .ofMaxLength(20)
                .list()
                .ofMinSize(0)
                .ofMaxSize(100);
    }

    @Property
    @Label("Setup Test: Performance measurement utility")
    void performanceMeasurementWorks(@ForAll @IntRange(min = 10, max = 100) int delayMs) {
        // Test that performance measurement utility works correctly
        long duration = measureExecutionTime(() -> {
            simulateQueryExecution(delayMs);
        });
        
        // Duration should be approximately equal to delay (with some tolerance)
        assertThat(duration).isGreaterThanOrEqualTo(delayMs - 5);
        assertThat(duration).isLessThanOrEqualTo(delayMs + 50);
    }

    @Property
    @Label("Setup Test: Performance threshold verification")
    void performanceThresholdVerification(@ForAll @IntRange(min = 1, max = 200) int durationMs) {
        // Test that performance threshold checking works correctly
        boolean meetsThreshold100 = meetsPerformanceThreshold(durationMs, 100);
        boolean meetsThreshold200 = meetsPerformanceThreshold(durationMs, 200);
        
        if (durationMs <= 100) {
            assertThat(meetsThreshold100).isTrue();
        } else {
            assertThat(meetsThreshold100).isFalse();
        }
        
        assertThat(meetsThreshold200).isTrue();
    }
}
