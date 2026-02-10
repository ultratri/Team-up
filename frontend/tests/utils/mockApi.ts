/**
 * Mock API Utilities
 * 
 * Provides utilities for mocking API calls in tests.
 * Used for testing components and stores that interact with the backend.
 */

import { vi } from 'vitest'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

/**
 * Creates a mock Axios instance with configurable responses
 */
export function createMockAxios(): AxiosInstance {
  const mockAxios = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
    request: vi.fn(),
    interceptors: {
      request: {
        use: vi.fn(),
        eject: vi.fn(),
        clear: vi.fn()
      },
      response: {
        use: vi.fn(),
        eject: vi.fn(),
        clear: vi.fn()
      }
    },
    defaults: {
      headers: {
        common: {},
        delete: {},
        get: {},
        head: {},
        post: {},
        put: {},
        patch: {}
      }
    }
  } as unknown as AxiosInstance

  return mockAxios
}

/**
 * Creates a mock successful Axios response
 */
export function createMockResponse<T>(data: T, status = 200): AxiosResponse<T> {
  return {
    data,
    status,
    statusText: 'OK',
    headers: {},
    config: {} as InternalAxiosRequestConfig
  }
}

/**
 * Creates a mock error response
 */
export function createMockError(message: string, status = 500) {
  const error = new Error(message) as any
  error.response = {
    data: { message },
    status,
    statusText: status === 404 ? 'Not Found' : 'Internal Server Error',
    headers: {},
    config: {} as InternalAxiosRequestConfig
  }
  error.isAxiosError = true
  return error
}

/**
 * Mock API delay utility for simulating network latency
 */
export function mockDelay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * Creates a mock API client with predefined responses
 */
export class MockApiClient {
  private responses: Map<string, any> = new Map()
  private errors: Map<string, Error> = new Map()
  private delays: Map<string, number> = new Map()

  /**
   * Set a mock response for a specific endpoint
   */
  setResponse(endpoint: string, data: any, delay = 0): void {
    this.responses.set(endpoint, data)
    if (delay > 0) {
      this.delays.set(endpoint, delay)
    }
  }

  /**
   * Set a mock error for a specific endpoint
   */
  setError(endpoint: string, error: Error, delay = 0): void {
    this.errors.set(endpoint, error)
    if (delay > 0) {
      this.delays.set(endpoint, delay)
    }
  }

  /**
   * Simulate an API call
   */
  async call<T>(endpoint: string): Promise<T> {
    const delay = this.delays.get(endpoint) || 0
    if (delay > 0) {
      await mockDelay(delay)
    }

    if (this.errors.has(endpoint)) {
      throw this.errors.get(endpoint)
    }

    if (this.responses.has(endpoint)) {
      return this.responses.get(endpoint)
    }

    throw new Error(`No mock response configured for endpoint: ${endpoint}`)
  }

  /**
   * Clear all mock responses and errors
   */
  clear(): void {
    this.responses.clear()
    this.errors.clear()
    this.delays.clear()
  }
}

/**
 * Mock team data generator
 */
export function createMockTeam(overrides: Partial<any> = {}) {
  return {
    id: 'team-1',
    name: 'Test Team',
    description: 'A test team',
    avatar: 'https://example.com/avatar.png',
    memberCount: 5,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    ...overrides
  }
}

/**
 * Mock task data generator
 */
export function createMockTask(overrides: Partial<any> = {}) {
  return {
    id: 'task-1',
    title: 'Test Task',
    description: 'A test task',
    status: 'todo',
    priority: 'medium',
    assigneeId: 'user-1',
    assignee: null,
    tags: [],
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    dueDate: null,
    ...overrides
  }
}

/**
 * Mock user data generator
 */
export function createMockUser(overrides: Partial<any> = {}) {
  return {
    id: 'user-1',
    username: 'testuser',
    email: 'test@example.com',
    avatar: 'https://example.com/avatar.png',
    ...overrides
  }
}
