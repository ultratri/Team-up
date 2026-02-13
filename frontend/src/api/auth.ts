import { request } from '@utils/request'
import type { User } from '../types/user'

export interface LoginRequest {
  userCode: string
  password: string
}

export interface LoginResponse {
  token: string
  user: User
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface RegisterRequest {
  userCode: string
  username: string
  password: string
  email: string
  phone?: string
  role?: 'STUDENT' | 'MENTOR'
}

// 登录
export async function login(data: LoginRequest) {
  const res = await request.post<ApiResult<LoginResponse>>('/auth/login', data)
  // 提取 data 字段
  return (res as any).data || res
}

// 注册
export async function register(data: RegisterRequest) {
  const res = await request.post<ApiResult<LoginResponse>>('/auth/register', data)
  return (res as any).data || res
}

// 获取当前用户信息
export async function getCurrentUser() {
  const res = await request.get<ApiResult<User>>('/auth/current')
  return (res as any).data || res
}

// 登出
export function logout() {
  return request.post('/auth/logout')
}

