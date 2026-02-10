import { request } from '@utils/request'

/**
 * 导出数据
 * @param type 数据类型: users, projects, teams, competitions, notifications, audit-logs
 * @param format 格式: excel, csv
 */
export async function exportData(type: string, format: string = 'excel') {
  try {
    const response = await request.get(`/admin/export/${type}`, {
      params: { format },
      responseType: 'blob'
    })

    // 创建下载链接
    const blob = new Blob([response as any], {
      type: format === 'excel'
        ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        : 'text/csv'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const extension = format === 'excel' ? 'xlsx' : 'csv'
    link.download = `${type}_${new Date().toISOString().split('T')[0]}.${extension}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error: any) {
    console.error('导出失败:', error)
    throw error
  }
}

/**
 * 获取数据统计（用于显示记录数）
 */
export async function getDataCounts() {
  try {
    const response = await request.get('/admin/export/counts')
    return response as any
  } catch (error: any) {
    console.error('获取数据统计失败:', error)
    return {
      users: 0,
      projects: 0,
      teams: 0,
      competitions: 0,
      notifications: 0,
      'audit-logs': 0
    }
  }
}

/**
 * 导入数据
 * @param type 数据类型: users, projects, teams, competitions
 * @param file 文件对象
 */
export async function importData(type: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const response = await request.post(`/admin/import/${type}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response as { successCount: number; failCount: number; message?: string }
  } catch (error: any) {
    console.error('导入失败:', error)
    throw error
  }
}
