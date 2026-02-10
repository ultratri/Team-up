import { request } from '@utils/request'
import type { UserProfile, UserSkill, UserCredit } from '../types/user'

// 获取用户档案
export function getProfile(userId: number) {
  return request.get<UserProfile>(`/profile/${userId}`)
}

// 更新用户档案
export function updateProfile(userId: number, data: Partial<UserProfile>) {
  return request.put(`/profile/${userId}`, data)
}

// 获取用户技能列表
export function getUserSkills(userId: number) {
  return request.get<UserSkill[]>(`/profile/${userId}/skills`)
}

// 添加用户技能
export function addUserSkill(userId: number, skill: Partial<UserSkill>) {
  return request.post(`/profile/${userId}/skills`, skill)
}

// 删除用户技能
export function removeUserSkill(skillId: number) {
  return request.delete(`/profile/skills/${skillId}`)
}

// 获取用户信誉信息
export function getUserCredit(userId: number) {
  return request.get<UserCredit>(`/profile/${userId}/credit`)
}

