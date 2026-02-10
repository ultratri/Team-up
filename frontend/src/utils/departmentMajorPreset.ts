import { getDepartmentMajorTree } from '@/api/system'

export interface DepartmentMajorDict {
  department: string
  majors: string[]
}

// 默认预设数据（作为降级方案）
const DEFAULT_PRESET: DepartmentMajorDict[] = [
  {
    department: '计算机学院',
    majors: ['计算机科学与技术', '软件工程', '网络工程', '信息安全', '人工智能', '数据科学与大数据技术']
  },
  {
    department: '信息工程学院',
    majors: ['电子信息工程', '通信工程', '自动化', '微电子科学与工程', '物联网工程']
  },
  {
    department: '数学与统计学院',
    majors: ['数学与应用数学', '信息与计算科学', '统计学']
  },
  {
    department: '管理学院',
    majors: ['信息管理与信息系统', '工商管理', '市场营销', '会计学', '财务管理']
  },
  {
    department: '经济学院',
    majors: ['经济学', '国际经济与贸易', '金融学']
  },
  {
    department: '外国语学院',
    majors: ['英语', '日语', '翻译']
  },
  {
    department: '设计与艺术学院',
    majors: ['视觉传达设计', '环境设计', '产品设计', '数字媒体艺术']
  },
  {
    department: '机械与电气工程学院',
    majors: ['机械工程', '机械设计制造及其自动化', '电气工程及其自动化']
  }
]

// 缓存的院系专业数据
let cachedData: DepartmentMajorDict[] | null = null
let loadingPromise: Promise<DepartmentMajorDict[]> | null = null

/**
 * 获取院系专业预设数据（从 API 获取，失败时使用默认数据）
 */
export async function getDepartmentMajorPreset(): Promise<DepartmentMajorDict[]> {
  // 如果已有缓存，直接返回
  if (cachedData) {
    return cachedData
  }
  
  // 如果正在加载，返回加载中的 Promise
  if (loadingPromise) {
    return loadingPromise
  }
  
  // 开始加载
  loadingPromise = (async () => {
    try {
      const tree = await getDepartmentMajorTree()
      const result: DepartmentMajorDict[] = []
      
      for (const [department, majors] of Object.entries(tree)) {
        result.push({ department, majors })
      }
      
      // 如果 API 返回空数据，使用默认数据
      if (result.length === 0) {
        console.warn('⚠️ API 返回空数据，使用默认预设')
        cachedData = DEFAULT_PRESET
        return DEFAULT_PRESET
      }
      
      cachedData = result
      return result
    } catch (error) {
      console.error('❌ 加载院系专业数据失败，使用默认预设:', error)
      cachedData = DEFAULT_PRESET
      return DEFAULT_PRESET
    } finally {
      loadingPromise = null
    }
  })()
  
  return loadingPromise
}

/**
 * 清除缓存（用于管理员修改数据后刷新）
 */
export function clearDepartmentMajorCache() {
  cachedData = null
}

/**
 * 同步获取院系专业预设数据（如果未加载，返回默认数据）
 * 注意：建议使用异步的 getDepartmentMajorPreset()
 */
export function getDepartmentMajorPresetSync(): DepartmentMajorDict[] {
  return cachedData || DEFAULT_PRESET
}

// 导出默认预设（向后兼容）
export const DEPARTMENT_MAJOR_PRESET = DEFAULT_PRESET
