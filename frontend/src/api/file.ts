import service, { request } from '@utils/request'

/**
 * 文件实体接口
 */
export interface FileEntity {
  id: number
  fileName: string
  isFolder: boolean
  fileSize?: number
  mimeType?: string
  fileType?: string
  uploaderId: number
  uploaderName: string
  parentFolderId?: number
  createdAt: string
}

/**
 * 团队文件活动（类似 git log 的最小实现：上传/下载/删除等）
 */
export interface ActivityVO {
  id: number
  userId: number
  username: string
  avatarUrl?: string
  activityType: string
  action: string
  detail?: string
  createdAt: string
}

/**
 * 获取团队文件列表
 * 
 * 返回指定团队和文件夹下的文件列表，按创建时间倒序排列。
 * 
 * @param teamId 团队 ID
 * @param folderId 文件夹 ID（可选，null 表示根目录）
 * @returns Promise<FileEntity[]> 文件列表
 * @throws {Error} 当团队不存在或用户无权限访问时抛出错误
 * 
 * @example
 * ```typescript
 * // 获取根目录文件
 * const files = await getTeamFiles(123);
 * 
 * // 获取指定文件夹下的文件
 * const folderFiles = await getTeamFiles(123, 456);
 * ```
 */
export function getTeamFiles(
  teamId: number,
  folderId?: number | null
): Promise<FileEntity[]> {
  const params = folderId ? { folderId } : {}
  return request.get<FileEntity[]>(`/api/teams/${teamId}/files`, { params })
}

/**
 * 创建团队文件夹
 * @param teamId 团队 ID
 * @param folderName 文件夹名称
 * @param parentFolderId 父文件夹 ID（可选）
 */
export function createTeamFolder(
  teamId: number,
  folderName: string,
  parentFolderId?: number | null
): Promise<FileEntity> {
  const params: Record<string, any> = { folderName }
  if (parentFolderId) {
    params.parentFolderId = parentFolderId
  }
  return request.post<FileEntity>(`/teams/${teamId}/folders`, null, { params })
}

/**
 * 下载文件
 * 
 * 下载指定的文件。对于云端存储的文件，返回预签名 URL；
 * 对于本地存储的文件，直接下载文件内容。
 * 
 * @param fileId 文件 ID
 * @returns Promise<void> 下载操作完成
 * @throws {Error} 当文件不存在或用户无权限下载时抛出错误
 * 
 * @example
 * ```typescript
 * await downloadFile(789);
 * ```
 */
export function downloadFile(fileId: number): Promise<void> {
  // 使用原生 fetch + Authorization 头，避免 axios 拦截器对 blob 响应的干扰
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const token = window.localStorage.getItem('token') || ''

  return fetch(`${baseUrl}/api/files/${fileId}/download`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    credentials: 'include',
  })
    .then(async (res) => {
      if (!res.ok) {
        const text = await res.text().catch(() => '')
        throw new Error(text || '文件下载失败')
      }
      return res.blob()
    })
    .then((blob) => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = ''
      a.style.display = 'none'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    })
}

/**
 * 删除文件
 * 
 * 删除指定的文件或文件夹。如果是文件夹，会递归删除其中的所有文件和子文件夹。
 * 只有文件上传者或团队管理者才能删除文件。
 * 
 * @param fileId 文件 ID
 * @returns Promise<void> 删除操作完成
 * @throws {Error} 当文件不存在或用户无权限删除时抛出错误
 * 
 * @example
 * ```typescript
 * await deleteFile(789);
 * ```
 */
export function deleteFile(fileId: number): Promise<void> {
  return request.delete<void>(`/api/files/${fileId}`)
}

/**
 * 获取某个文件的活动历史
 */
export function getFileActivities(fileId: number, limit = 50): Promise<ActivityVO[]> {
  return request.get<ActivityVO[]>(`/api/files/${fileId}/activities`, { params: { limit } })
}

/**
 * 预览文件（inline）
 */
export async function previewFileBlob(fileId: number): Promise<Blob> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const token = window.localStorage.getItem('token') || ''

  const res = await fetch(`${baseUrl}/api/files/${fileId}/preview`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    credentials: 'include',
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(text || '文件预览失败')
  }

  return res.blob()
}
