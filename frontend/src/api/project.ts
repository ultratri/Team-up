import { request } from '@utils/request'
import { normalizeProjectFiles } from '@utils/fieldNormalizer'
import type { Project, ProjectApplication, ProjectComment, ProjectFile, ProjectMilestone } from '../types/project'
import type { TeamMember } from '../types/team'

interface PageResult<T> {
  records: T[]
  total: number
  // 其他分页字段不强约束
  [key: string]: any
}

// 获取项目列表（项目大厅）
export async function getProjects(params?: {
  page?: number
  size?: number
  type?: string
  status?: string
  keyword?: string
}) {
  // request.ts 已经将后端 Result<T> 解包成 T，这里拿到的就是 Page<Project>
  const page = await request.get<PageResult<Project>>('/projects', { params })

  if (!page) {
    return {
      list: [],
      total: 0,
    }
  }

  return {
    list: page.records || [],
    total: page.total ?? 0,
  }
}

// 获取我的项目列表
export async function getMyProjects(params?: {
  page?: number
  size?: number
  type?: string
  status?: string
  keyword?: string
}) {
  const page = await request.get<PageResult<Project>>('/projects/my', { params })

  if (!page) {
    return {
      list: [],
      total: 0,
    }
  }

  return {
    list: page.records || [],
    total: page.total ?? 0,
  }
}

// 获取项目详情
export function getProjectDetail(id: number) {
  return request.get<Project>(`/projects/${id}`)
}

// 获取项目成员（通过项目关联的团队）
export function getProjectMembers(projectId: number) {
  return request.get<TeamMember[]>(`/projects/${projectId}/members`)
}

// 创建项目
export function createProject(data: Partial<Project>) {
  return request.post<Project>('/projects', data)
}

// 发布项目
export function publishProject(projectId: number) {
  return request.post(`/projects/${projectId}/publish`)
}

// 更新项目
export function updateProject(id: number, data: Partial<Project>) {
  return request.put<Project>(`/projects/${id}`, data)
}

// 删除项目
export function deleteProject(id: number) {
  return request.delete(`/projects/${id}`)
}

// 申请加入项目
export function applyProject(projectId: number, reason?: string) {
  return request.post<ProjectApplication>(`/projects/${projectId}/apply`, { reason })
}

// 审核申请
export function reviewApplication(applicationId: number, approved: boolean, comment?: string) {
  return request.post(`/projects/applications/${applicationId}/review`, null, { 
    params: { approved, comment }
  })
}

// 获取我创建的项目的所有申请
export async function getMyProjectApplications(page: number = 1, size: number = 10, status?: string) {
  const res = await request.get<ApiResult<PageResult<ProjectApplication>>>('/projects/applications/my-projects', {
    params: { page, size, status }
  })
  return res
}

// 获取我的申请历史
export async function getMyApplications(page: number = 1, size: number = 10) {
  const res = await request.get<ApiResult<PageResult<ProjectApplication>>>('/projects/applications/my-applications', {
    params: { page, size }
  })
  return res
}

// 撤回申请
export function withdrawApplication(applicationId: number) {
  return request.post(`/projects/applications/${applicationId}/withdraw`)
}

// 批量审核申请
export function batchReviewApplications(applicationIds: number[], approved: boolean, comment?: string) {
  return request.post('/projects/applications/batch-review', {
    applicationIds,
    approved,
    comment
  })
}

// 获取项目推荐结果
export function getProjectRecommendations(projectId: number) {
  return request.get<ApiResult<any[]>>(`/projects/${projectId}/recommendations`)
}

// 获取项目评论（分页）
export function getProjectComments(projectId: number, page = 1, size = 10) {
  return request.get<ApiResult<{ records: ProjectComment[]; total: number }>>(
    `/projects/${projectId}/comments`,
    { params: { page, size } },
  )
}

// 新增评论或回复
export function addProjectComment(data: {
  projectId: number
  content: string
  parentId?: number
  replyToUserId?: number
}) {
  const { projectId, ...payload } = data
  return request.post<ApiResult<ProjectComment>>(`/projects/${projectId}/comments`, payload)
}

// 获取项目文件列表（分页）
export function getProjectFiles(projectId: number, page = 1, size = 10, category?: string) {
  return request.get<ApiResult<{ records: ProjectFile[]; total: number }>>(
    `/projects/${projectId}/files`,
    { params: { page, size, category } }
  ).then(res => {
    if (res.data && res.data.records) {
      res.data.records = normalizeProjectFiles(res.data.records)
    }
    return res
  })
}

// 上传项目文件
export function uploadProjectFile(
  projectId: number,
  file: File,
  category?: string,
  description?: string
) {
  const formData = new FormData()
  formData.append('file', file)
  if (category) formData.append('category', category)
  if (description) formData.append('description', description)
  
  return request.post<ApiResult<ProjectFile>>(
    `/projects/${projectId}/files`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }
  )
}

// 删除项目文件
export function deleteProjectFile(fileId: number) {
  return request.delete(`/projects/files/${fileId}`)
}

// 获取项目文件分类列表
export function getProjectFileCategories(projectId: number) {
  return request.get<ApiResult<string[]>>(`/projects/${projectId}/files/categories`)
}

// 里程碑列表
export function getProjectMilestones(projectId: number) {
  return request.get<ApiResult<ProjectMilestone[]>>(`/projects/${projectId}/milestones`)
}

// 新增里程碑
export function createProjectMilestone(projectId: number, data: Partial<ProjectMilestone>) {
  return request.post<ApiResult<ProjectMilestone>>(`/projects/${projectId}/milestones`, data)
}

// 更新里程碑
export function updateProjectMilestone(milestoneId: number, data: Partial<ProjectMilestone>) {
  return request.put<ApiResult<ProjectMilestone>>(`/projects/milestones/${milestoneId}`, data)
}

// 删除里程碑
export function deleteProjectMilestone(milestoneId: number) {
  return request.delete(`/projects/milestones/${milestoneId}`)
}

// 完成项目（并处理团队）
export function completeProject(projectId: number, teamAction: 'KEEP' | 'DISSOLVE', summary?: string) {
  return request.post(`/projects/${projectId}/complete`, {
    teamAction,
    summary
  })
}

// 为项目关联团队
export function associateTeamWithProject(projectId: number, teamId: number) {
  return request.post(`/projects/${projectId}/associate-team`, {
    teamId
  })
}
