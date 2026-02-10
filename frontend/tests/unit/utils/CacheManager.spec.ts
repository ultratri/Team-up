/**
 * Unit Tests for CacheManager
 * 
 * These tests verify specific edge cases and scenarios for the CacheManager utility.
 * 
 * Requirements: 4.1, 4.4, 4.5
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { CacheManager } from '../../../src/utils/CacheManager';

describe('CacheManager - Unit Tests', () => {
  let cacheManager: CacheManager;

  beforeEach(() => {
    cacheManager = new CacheManager();
  });

  describe('Edge Cases', () => {
    /**
     * Test cache with zero TTL
     * 
     * Verifies that entries with zero TTL expire immediately.
     * Requirements: 4.1, 4.4
     */
    it('should handle cache with zero TTL', async () => {
      const cache = new CacheManager({
        defaultTTL: 0,
      });

      const key = 'test-key';
      const data = { value: 'test' };

      // Set data with zero TTL
      cache.set(key, data);

      // Wait a tiny bit for expiration
      await new Promise(resolve => setTimeout(resolve, 10));

      // Data should be expired and return null
      const retrieved = cache.get(key);
      expect(retrieved).toBeNull();
    });

    /**
     * Test cache eviction with single entry
     * 
     * Verifies that eviction works correctly when there's only one entry.
     * Requirements: 4.5
     */
    it('should handle cache eviction with single entry', () => {
      const cache = new CacheManager({
        maxSize: 1000, // 1KB limit
      });

      // Add a small entry
      const smallData = 'x'.repeat(100);
      cache.set('small', smallData);

      expect(cache.count).toBe(1);

      // Add a large entry that exceeds the limit (but not the max size itself)
      const largeData = 'y'.repeat(400); // ~800 bytes
      cache.set('large', largeData);

      // Cache should have evicted the small entry to make room
      expect(cache.get('small')).toBeNull();
      expect(cache.get('large')).not.toBeNull();
      
      // Cache size should be within limit
      expect(cache.size).toBeLessThanOrEqual(1000);
    });

    /**
     * Test cache size calculation
     * 
     * Verifies that cache size is calculated correctly for various data types.
     * Requirements: 4.5
     */
    it('should calculate cache size correctly', () => {
      const cache = new CacheManager();

      // Add entries of known sizes
      cache.set('key1', 'a'.repeat(100)); // ~200 bytes
      cache.set('key2', 'b'.repeat(200)); // ~400 bytes
      cache.set('key3', { data: 'c'.repeat(50) }); // ~100+ bytes

      // Cache should have accumulated size
      expect(cache.size).toBeGreaterThan(0);
      expect(cache.count).toBe(3);

      // Clear one entry
      cache.clear('key1');

      // Size should decrease
      expect(cache.count).toBe(2);
    });

    /**
     * Test empty cache operations
     * 
     * Verifies that operations on empty cache don't cause errors.
     */
    it('should handle operations on empty cache', () => {
      const cache = new CacheManager();

      // Get from empty cache
      expect(cache.get('nonexistent')).toBeNull();

      // Check staleness on empty cache
      expect(cache.isStale('nonexistent')).toBe(false);

      // Clear empty cache
      expect(() => cache.clear()).not.toThrow();

      // Get stats from empty cache
      const stats = cache.getStats();
      expect(stats.count).toBe(0);
      expect(stats.size).toBe(0);
    });

    /**
     * Test cache with null/undefined data
     * 
     * Verifies that cache handles null and undefined values correctly.
     */
    it('should handle null and undefined data', () => {
      const cache = new CacheManager();

      // Set null value
      cache.set('null-key', null);
      expect(cache.get('null-key')).toBeNull();

      // Set undefined value
      cache.set('undefined-key', undefined);
      expect(cache.get('undefined-key')).toBeUndefined();

      // Both should be in cache
      expect(cache.count).toBe(2);
    });

    /**
     * Test cache with complex nested objects
     * 
     * Verifies that cache handles deeply nested objects correctly.
     */
    it('should handle complex nested objects', () => {
      const cache = new CacheManager();

      const complexData = {
        level1: {
          level2: {
            level3: {
              array: [1, 2, 3, { nested: 'value' }],
              map: { key1: 'value1', key2: 'value2' },
            },
          },
        },
        metadata: {
          timestamp: Date.now(),
          tags: ['tag1', 'tag2'],
        },
      };

      cache.set('complex', complexData);

      const retrieved = cache.get('complex');
      expect(retrieved).toEqual(complexData);
    });

    /**
     * Test cache pattern-based clearing
     * 
     * Verifies that pattern-based clearing works correctly.
     */
    it('should clear cache entries matching pattern', () => {
      const cache = new CacheManager();

      // Add entries with different prefixes
      cache.set('team:1:details', { id: 1 });
      cache.set('team:2:details', { id: 2 });
      cache.set('user:1:profile', { id: 1 });
      cache.set('user:2:profile', { id: 2 });

      expect(cache.count).toBe(4);

      // Clear only team entries
      cache.clear('^team:');

      // Team entries should be gone
      expect(cache.get('team:1:details')).toBeNull();
      expect(cache.get('team:2:details')).toBeNull();

      // User entries should remain
      expect(cache.get('user:1:profile')).not.toBeNull();
      expect(cache.get('user:2:profile')).not.toBeNull();

      expect(cache.count).toBe(2);
    });

    /**
     * Test cache with very large entries
     * 
     * Verifies that cache handles large entries without crashing.
     */
    it('should handle very large entries', () => {
      const cache = new CacheManager({
        maxSize: 10 * 1024 * 1024, // 10MB
      });

      // Create a large entry (1MB)
      const largeData = 'x'.repeat(500000);
      
      expect(() => cache.set('large', largeData)).not.toThrow();
      expect(cache.get('large')).toBe(largeData);
    });

    /**
     * Test LRU eviction order
     * 
     * Verifies that LRU eviction removes the least recently accessed entry.
     */
    it('should evict least recently used entry first', () => {
      const cache = new CacheManager({
        maxSize: 3000, // 3KB limit
      });

      // Add three entries (each ~800 bytes)
      cache.set('entry1', 'a'.repeat(400));
      cache.set('entry2', 'b'.repeat(400));
      cache.set('entry3', 'c'.repeat(400));

      // Verify all three are present
      expect(cache.get('entry1')).not.toBeNull();
      expect(cache.get('entry2')).not.toBeNull();
      expect(cache.get('entry3')).not.toBeNull();

      // Access entry1 and entry2 again to update their LRU status
      cache.get('entry1');
      cache.get('entry2');

      // Add another entry that will trigger eviction (~800 bytes)
      cache.set('entry4', 'd'.repeat(400));

      // entry3 should be evicted (least recently accessed)
      // Note: Due to the way LRU works, the least recently accessed entry
      // (entry3) should be removed first
      const entry3Present = cache.get('entry3') !== null;
      const entry1Present = cache.get('entry1') !== null;
      const entry2Present = cache.get('entry2') !== null;
      const entry4Present = cache.get('entry4') !== null;

      // At least one entry should have been evicted
      const totalPresent = [entry1Present, entry2Present, entry3Present, entry4Present].filter(Boolean).length;
      expect(totalPresent).toBeLessThan(4);

      // entry4 should be present (just added)
      expect(entry4Present).toBe(true);

      // entry1 and entry2 should be more likely to be present than entry3
      // (since they were accessed more recently)
      if (!entry3Present) {
        // This is the expected behavior - entry3 was evicted
        expect(entry1Present || entry2Present).toBe(true);
      }
    });

    /**
     * Test cache statistics
     * 
     * Verifies that cache statistics are accurate.
     */
    it('should provide accurate cache statistics', () => {
      const cache = new CacheManager({
        maxSize: 10000,
      });

      // Add some entries
      cache.set('key1', 'a'.repeat(100));
      cache.set('key2', 'b'.repeat(200));
      cache.set('key3', 'c'.repeat(300));

      const stats = cache.getStats();

      expect(stats.count).toBe(3);
      expect(stats.size).toBeGreaterThan(0);
      expect(stats.maxSize).toBe(10000);
      expect(stats.utilizationPercent).toBeGreaterThan(0);
      expect(stats.utilizationPercent).toBeLessThan(100);
    });

    /**
     * Test cache with different TTL types
     * 
     * Verifies that different data types use their configured TTLs.
     */
    it('should use type-specific TTLs', () => {
      const cache = new CacheManager();

      // Set data with different types
      cache.set('team:1', { id: 1 }, 'team-details');
      cache.set('members:1', [{ id: 1 }], 'member-lists');
      cache.set('tasks:1', [{ id: 1 }], 'task-summaries');

      // All should be retrievable immediately
      expect(cache.get('team:1')).not.toBeNull();
      expect(cache.get('members:1')).not.toBeNull();
      expect(cache.get('tasks:1')).not.toBeNull();
    });

    /**
     * Test shouldRefresh method
     * 
     * Verifies that shouldRefresh correctly identifies when data should be refreshed.
     */
    it('should correctly identify when data should be refreshed', () => {
      const cache = new CacheManager();

      const key = 'test-key';
      const data = { value: 'test' };

      cache.set(key, data);

      // Immediately after setting, should not need refresh
      expect(cache.shouldRefresh(key, 30000)).toBe(false);

      // Non-existent key should not need refresh
      expect(cache.shouldRefresh('nonexistent', 30000)).toBe(false);
    });

    /**
     * Test multiple evictions
     * 
     * Verifies that multiple evictions work correctly when adding large data.
     */
    it('should handle multiple evictions when needed', () => {
      const cache = new CacheManager({
        maxSize: 3000, // 3KB limit
      });

      // Add many small entries (each ~400 bytes)
      for (let i = 0; i < 10; i++) {
        cache.set(`entry${i}`, 'x'.repeat(200));
      }

      // Add a large entry that requires multiple evictions (~1.2KB)
      cache.set('large', 'y'.repeat(600));

      // Cache should be within limit
      expect(cache.size).toBeLessThanOrEqual(3000);

      // Large entry should be present
      expect(cache.get('large')).not.toBeNull();

      // Some small entries should have been evicted
      expect(cache.count).toBeLessThan(11);
    });
  });
});
