/**
 * ParallelDataLoader - Utility for executing multiple API requests concurrently
 * 
 * This utility enables parallel data loading with priority-based execution,
 * individual failure handling, and timing information for performance monitoring.
 * 
 * Features:
 * - Concurrent request execution using Promise.allSettled
 * - Priority-based loading (critical, high, normal, low)
 * - Individual request failure handling without blocking others
 * - Timing information for each request
 * - Fallback data support for failed requests
 */

export interface LoadRequest<T> {
  key: string;
  loader: () => Promise<T>;
  priority: 'critical' | 'high' | 'normal' | 'low';
  fallback?: T;
}

export interface LoadResult<T> {
  key: string;
  data?: T;
  error?: Error;
  duration: number;
}

export class ParallelDataLoader {
  /**
   * Load all requests concurrently without priority ordering
   * 
   * Executes all requests in parallel using Promise.allSettled, ensuring that
   * individual failures don't block other requests from completing.
   * 
   * @param requests - Array of load requests to execute
   * @returns Map of results keyed by request key
   */
  async loadAll<T>(requests: LoadRequest<T>[]): Promise<Map<string, LoadResult<T>>> {
    const results = new Map<string, LoadResult<T>>();

    // Execute all requests concurrently
    const promises = requests.map(async (request) => {
      const startTime = performance.now();
      
      try {
        const data = await request.loader();
        const duration = performance.now() - startTime;
        
        results.set(request.key, {
          key: request.key,
          data,
          duration
        });
      } catch (error) {
        const duration = performance.now() - startTime;
        
        results.set(request.key, {
          key: request.key,
          error: error instanceof Error ? error : new Error(String(error)),
          data: request.fallback,
          duration
        });
      }
    });

    // Wait for all requests to complete (success or failure)
    await Promise.allSettled(promises);

    return results;
  }

  /**
   * Load requests with priority-based ordering
   * 
   * Executes requests in priority order (critical → high → normal → low),
   * starting each priority group as soon as the previous group begins execution.
   * This ensures critical data starts loading first while still maintaining
   * parallel execution within each priority level.
   * 
   * @param requests - Array of load requests to execute
   * @returns Map of results keyed by request key
   */
  async loadWithPriority<T>(requests: LoadRequest<T>[]): Promise<Map<string, LoadResult<T>>> {
    const results = new Map<string, LoadResult<T>>();

    // Group requests by priority
    const priorityGroups = this.groupByPriority(requests);
    
    // Priority order
    const priorityOrder: Array<'critical' | 'high' | 'normal' | 'low'> = [
      'critical',
      'high',
      'normal',
      'low'
    ];

    // Execute each priority group
    const allPromises: Promise<void>[] = [];
    
    for (const priority of priorityOrder) {
      const group = priorityGroups.get(priority);
      if (!group || group.length === 0) continue;

      // Start all requests in this priority group concurrently
      const groupPromises = group.map(async (request) => {
        const startTime = performance.now();
        
        try {
          const data = await request.loader();
          const duration = performance.now() - startTime;
          
          results.set(request.key, {
            key: request.key,
            data,
            duration
          });
        } catch (error) {
          const duration = performance.now() - startTime;
          
          results.set(request.key, {
            key: request.key,
            error: error instanceof Error ? error : new Error(String(error)),
            data: request.fallback,
            duration
          });
        }
      });

      // Add this group's promises to the overall list
      allPromises.push(...groupPromises);
      
      // Small delay to ensure priority ordering (critical starts first)
      // but don't wait for completion before starting next priority
      await new Promise(resolve => setTimeout(resolve, 0));
    }

    // Wait for all requests across all priorities to complete
    await Promise.allSettled(allPromises);

    return results;
  }

  /**
   * Group requests by priority level
   * 
   * @param requests - Array of load requests
   * @returns Map of requests grouped by priority
   */
  private groupByPriority<T>(
    requests: LoadRequest<T>[]
  ): Map<'critical' | 'high' | 'normal' | 'low', LoadRequest<T>[]> {
    const groups = new Map<'critical' | 'high' | 'normal' | 'low', LoadRequest<T>[]>();
    
    // Initialize all priority groups
    groups.set('critical', []);
    groups.set('high', []);
    groups.set('normal', []);
    groups.set('low', []);

    // Group requests by priority
    for (const request of requests) {
      const group = groups.get(request.priority);
      if (group) {
        group.push(request);
      }
    }

    return groups;
  }
}
