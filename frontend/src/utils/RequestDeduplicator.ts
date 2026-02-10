/**
 * RequestDeduplicator - Utility for preventing duplicate API requests
 * 
 * This utility ensures that identical API requests (same endpoint, method, and parameters)
 * are only executed once, with all callers sharing the same promise and receiving the
 * same result or error.
 * 
 * Features:
 * - Generates unique keys from request parameters
 * - Tracks in-flight requests in a Map
 * - Shares promises for identical requests
 * - Automatically clears completed requests from map
 * - Propagates errors to all waiting callers
 * 
 * Requirements: 6.1, 6.4
 */

export interface RequestKey {
  endpoint: string;
  method: string;
  params: Record<string, any>;
}

export class RequestDeduplicator {
  private inFlightRequests: Map<string, Promise<any>>;

  constructor() {
    this.inFlightRequests = new Map();
  }

  /**
   * Deduplicate a request by sharing the promise for identical requests
   * 
   * If a request with the same key is already in flight, returns the existing promise.
   * Otherwise, executes the fetcher and stores the promise until it completes.
   * 
   * @param key - Request identifier (endpoint, method, params)
   * @param fetcher - Function that executes the actual request
   * @returns Promise that resolves with the request result
   */
  async dedupe<T>(key: RequestKey, fetcher: () => Promise<T>): Promise<T> {
    // Generate unique key for this request
    const requestKey = this.generateKey(key);

    // Check if this request is already in flight
    const existingRequest = this.inFlightRequests.get(requestKey);
    if (existingRequest) {
      // Return the existing promise
      return existingRequest;
    }

    // Create new request promise
    const requestPromise = fetcher()
      .then((result) => {
        // Clear from map on success
        this.inFlightRequests.delete(requestKey);
        return result;
      })
      .catch((error) => {
        // Clear from map on error
        this.inFlightRequests.delete(requestKey);
        // Re-throw to propagate error to all callers
        throw error;
      });

    // Store the promise
    this.inFlightRequests.set(requestKey, requestPromise);

    return requestPromise;
  }

  /**
   * Generate a unique key from request parameters
   * 
   * Creates a deterministic string key by combining endpoint, method, and
   * a sorted JSON representation of parameters to ensure identical requests
   * produce the same key regardless of parameter order.
   * 
   * @param key - Request identifier
   * @returns Unique string key
   */
  private generateKey(key: RequestKey): string {
    // Sort params to ensure consistent key generation
    const sortedParams = this.sortObject(key.params);
    const paramsString = JSON.stringify(sortedParams);
    
    return `${key.method}:${key.endpoint}:${paramsString}`;
  }

  /**
   * Sort object keys recursively for consistent serialization
   * 
   * @param obj - Object to sort
   * @returns New object with sorted keys
   */
  private sortObject(obj: Record<string, any>): Record<string, any> {
    if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) {
      return obj;
    }

    const sorted: Record<string, any> = {};
    const keys = Object.keys(obj).sort();

    for (const key of keys) {
      const value = obj[key];
      if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
        sorted[key] = this.sortObject(value);
      } else {
        sorted[key] = value;
      }
    }

    return sorted;
  }

  /**
   * Clear a specific request or all requests from the map
   * 
   * @param key - Optional request key to clear. If not provided, clears all requests.
   */
  clear(key?: RequestKey): void {
    if (key) {
      const requestKey = this.generateKey(key);
      this.inFlightRequests.delete(requestKey);
    } else {
      this.inFlightRequests.clear();
    }
  }

  /**
   * Get the number of in-flight requests
   * 
   * @returns Number of requests currently being tracked
   */
  get size(): number {
    return this.inFlightRequests.size;
  }
}
