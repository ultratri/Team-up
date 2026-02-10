/**
 * CacheManager - Intelligent caching utility with TTL and LRU eviction
 * 
 * This utility provides a caching layer with the following features:
 * - Time-to-live (TTL) based cache expiration
 * - Independent TTL values by data type
 * - Least Recently Used (LRU) eviction when size exceeds limit
 * - Staleness checking for background refresh
 * - Size-based cache management
 * 
 * Requirements: 4.1, 4.4, 4.5
 */

export interface CacheEntry<T> {
  data: T;
  timestamp: number;
  ttl: number;
  size: number;
  lastAccessed: number;
}

export interface CacheConfig {
  maxSize: number; // bytes (default: 50MB)
  defaultTTL: number; // milliseconds (default: 60000 = 1 minute)
  ttlByType: Map<string, number>;
}

export class CacheManager {
  private cache: Map<string, CacheEntry<any>>;
  private config: CacheConfig;
  private currentSize: number;

  constructor(config?: Partial<CacheConfig>) {
    this.cache = new Map();
    this.currentSize = 0;
    
    // Default configuration
    this.config = {
      maxSize: config?.maxSize ?? 50 * 1024 * 1024, // 50MB default
      defaultTTL: config?.defaultTTL ?? 60000, // 1 minute default
      ttlByType: config?.ttlByType ?? new Map([
        ['team-details', 60000], // 60 seconds
        ['member-lists', 120000], // 120 seconds
        ['task-summaries', 30000], // 30 seconds
      ]),
    };
  }

  /**
   * Get data from cache
   * 
   * Returns cached data if it exists and is not expired.
   * Updates last accessed timestamp for LRU tracking.
   * 
   * @param key - Cache key
   * @returns Cached data or null if not found or expired
   */
  get<T>(key: string): T | null {
    const entry = this.cache.get(key);
    
    if (!entry) {
      return null;
    }

    // Check if entry has expired
    const now = Date.now();
    const age = now - entry.timestamp;
    
    if (age > entry.ttl) {
      // Entry has expired, remove it
      this.remove(key);
      return null;
    }

    // Update last accessed time for LRU
    entry.lastAccessed = now;
    
    return entry.data as T;
  }

  /**
   * Set data in cache
   * 
   * Stores data with timestamp and TTL. If adding this entry would exceed
   * the size limit, evicts least recently used entries first.
   * 
   * @param key - Cache key
   * @param data - Data to cache
   * @param type - Optional data type for type-specific TTL
   */
  set<T>(key: string, data: T, type?: string): void {
    const now = Date.now();
    
    // Determine TTL based on type or use default
    const ttl = type && this.config.ttlByType.has(type)
      ? this.config.ttlByType.get(type)!
      : this.config.defaultTTL;

    // Calculate size of the data
    const size = this.calculateSize(data);

    // Remove existing entry if present
    if (this.cache.has(key)) {
      this.remove(key);
    }

    // If the new entry is larger than max size, don't cache it
    if (size > this.config.maxSize) {
      return;
    }

    // Evict entries if necessary to make room
    while (this.currentSize + size > this.config.maxSize && this.cache.size > 0) {
      this.evictLRU();
    }

    // Create cache entry
    const entry: CacheEntry<T> = {
      data,
      timestamp: now,
      ttl,
      size,
      lastAccessed: now,
    };

    // Store in cache
    this.cache.set(key, entry);
    this.currentSize += size;
  }

  /**
   * Check if cached data is stale
   * 
   * Returns true if the data exists but is older than the specified threshold.
   * This is useful for triggering background refreshes.
   * 
   * @param key - Cache key
   * @param threshold - Age threshold in milliseconds (default: 30000 = 30 seconds)
   * @returns True if data is stale but not expired
   */
  isStale(key: string, threshold: number = 30000): boolean {
    const entry = this.cache.get(key);
    
    if (!entry) {
      return false;
    }

    const now = Date.now();
    const age = now - entry.timestamp;

    // Check if stale (older than threshold) but not expired
    return age > threshold && age <= entry.ttl;
  }

  /**
   * Check if data should be refreshed
   * 
   * Similar to isStale but allows custom threshold.
   * 
   * @param key - Cache key
   * @param threshold - Age threshold in milliseconds
   * @returns True if data should be refreshed
   */
  shouldRefresh(key: string, threshold: number): boolean {
    return this.isStale(key, threshold);
  }

  /**
   * Evict least recently used entry
   * 
   * Removes the entry with the oldest lastAccessed timestamp.
   * This is called automatically when cache size exceeds the limit.
   */
  evictLRU(): void {
    if (this.cache.size === 0) {
      return;
    }

    let oldestKey: string | null = null;
    let oldestTime = Infinity;

    // Find the least recently used entry
    for (const [key, entry] of this.cache.entries()) {
      if (entry.lastAccessed < oldestTime) {
        oldestTime = entry.lastAccessed;
        oldestKey = key;
      }
    }

    // Remove the oldest entry
    if (oldestKey) {
      this.remove(oldestKey);
    }
  }

  /**
   * Remove an entry from cache
   * 
   * @param key - Cache key to remove
   */
  private remove(key: string): void {
    const entry = this.cache.get(key);
    
    if (entry) {
      this.currentSize -= entry.size;
      this.cache.delete(key);
    }
  }

  /**
   * Clear cache entries matching a pattern
   * 
   * @param pattern - Optional regex pattern to match keys. If not provided, clears all.
   */
  clear(pattern?: string): void {
    if (!pattern) {
      this.cache.clear();
      this.currentSize = 0;
      return;
    }

    const regex = new RegExp(pattern);
    const keysToDelete: string[] = [];

    for (const key of this.cache.keys()) {
      if (regex.test(key)) {
        keysToDelete.push(key);
      }
    }

    for (const key of keysToDelete) {
      this.remove(key);
    }
  }

  /**
   * Calculate approximate size of data in bytes
   * 
   * This is a rough estimation based on JSON serialization.
   * 
   * @param data - Data to measure
   * @returns Approximate size in bytes
   */
  private calculateSize(data: any): number {
    try {
      const jsonString = JSON.stringify(data);
      // Each character is approximately 2 bytes in UTF-16
      return jsonString.length * 2;
    } catch (error) {
      // If serialization fails, return a default size
      return 1024; // 1KB default
    }
  }

  /**
   * Get current cache size in bytes
   * 
   * @returns Current cache size
   */
  get size(): number {
    return this.currentSize;
  }

  /**
   * Get number of entries in cache
   * 
   * @returns Number of cached entries
   */
  get count(): number {
    return this.cache.size;
  }

  /**
   * Get cache statistics
   * 
   * @returns Object with cache statistics
   */
  getStats(): {
    size: number;
    count: number;
    maxSize: number;
    utilizationPercent: number;
  } {
    return {
      size: this.currentSize,
      count: this.cache.size,
      maxSize: this.config.maxSize,
      utilizationPercent: (this.currentSize / this.config.maxSize) * 100,
    };
  }
}
