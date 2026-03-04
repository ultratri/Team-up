import request from '@/utils/request'

/**
 * 举报目标类型
 */
export enum TargetType {
  PROJECT = 'PROJECT',
  TEAM = 'TEAM',
  USER = 'USER',
  COMMENT = 'COMMENT'
}

/**
 * 举报原因
 */
export enum ReportReason {
  SPAM = 'SPAM',           // 垃圾信息
  FRAUD = 'FRAUD',         // 诈骗
  INAPPROPRIATE = 'INAPPROPRIATE',  // 不当内容
  HARASSMENT = 'HARASSMENT',     // 骚扰
  OTHER = 'OTHER'          // 其他
}

/**
 * 举报状态
 */
export enum ReportStatus {
  PENDING = 'PENDING',     // 待处理
  REVIEWING = 'REVIEWING', // 审核中
  RESOLVED = 'RESOLVED',   // 已处理
  REJECTED = 'REJECTED'    // 已驳回
}

/**
 * 提交举报DTO
 */
export interface SubmitReportDTO {
  targetType: TargetType
  targetId: number
  reason: ReportReason
  description: string
  evidenceUrls?: string[]
}

/**
 * 处理举报DTO
 */
export interface HandleReportDTO {
  reportId: number
  status: ReportStatus
  handleResult: string
  punishTarget?: boolean
  punishmentType?: string
  punishmentDays?: number
}

/**
 * 举报详情VO
 */
export interface ReportDetailVO {
  id: number
  reporterId: number
  reporterName: string
  targetType: TargetType
  targetId: number
  targetName: string
  reason: ReportReason
  description: string
  evidenceUrls: string[]
  status: ReportStatus
  handlerId?: number
  handlerName?: string
  handleResult?: string
  handledAt?: string
  createdAt: string
  updatedAt: string
}

/**
 * 举报统计VO
 */
export interface ReportStatisticsVO {
  targetType: TargetType
  status: ReportStatus
  count: number
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 提交举报
 */
export async function submitReport(data: SubmitReportDTO): Promise<void> {
  await request.post('/reports', data)
}

/**
 * 查询举报列表（管理员）
 */
export async function getReportList(params: {
  page: number
  size: number
  status?: ReportStatus
  targetType?: TargetType
}): Promise<PageResult<ReportDetailVO>> {
  const res = await request.get<any>('/admin/reports', { params })
  if (res && typeof res === 'object') {
    if ('records' in res) {
      return res as PageResult<ReportDetailVO>
    }
    if ('data' in res && res.data && 'records' in res.data) {
      return res.data as PageResult<ReportDetailVO>
    }
  }
  return {
    records: [],
    total: 0,
    size: params.size,
    current: params.page,
    pages: 0
  }
}

/**
 * 查询举报详情
 */
export async function getReportDetail(reportId: number): Promise<ReportDetailVO> {
  const res = await request.get<any>(`/admin/reports/${reportId}`)
  if (res && typeof res === 'object') {
    if ('data' in res) {
      return res.data as ReportDetailVO
    }
    return res as ReportDetailVO
  }
  throw new Error('获取举报详情失败')
}

/**
 * 处理举报
 */
export async function handleReport(data: HandleReportDTO): Promise<void> {
  await request.post('/admin/reports/handle', data)
}

/**
 * 获取举报统计
 */
export async function getReportStatistics(): Promise<ReportStatisticsVO[]> {
  const res = await request.get<any>('/admin/reports/statistics')
  if (res && typeof res === 'object') {
    if ('data' in res) {
      return res.data as ReportStatisticsVO[]
    }
    if (Array.isArray(res)) {
      return res as ReportStatisticsVO[]
    }
  }
  return []
}
