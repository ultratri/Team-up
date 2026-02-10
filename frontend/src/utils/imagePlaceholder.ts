/**
 * 图片占位符工具
 * 用于生成 SVG 占位图和处理图片加载
 */

/**
 * 生成 SVG 占位图
 * @param width 宽度
 * @param height 高度
 * @param text 显示文本
 * @param bgColor 背景颜色
 * @param textColor 文字颜色
 * @returns Data URL 格式的 SVG 图片
 */
export function generatePlaceholder(
  width: number = 200,
  height: number = 200,
  text: string = '加载中...',
  bgColor: string = '#f0f0f0',
  textColor: string = '#999'
): string {
  const svg = `
    <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
      <rect width="100%" height="100%" fill="${bgColor}"/>
      <text 
        x="50%" 
        y="50%" 
        dominant-baseline="middle" 
        text-anchor="middle" 
        font-family="Arial, sans-serif" 
        font-size="14" 
        fill="${textColor}"
      >
        ${text}
      </text>
    </svg>
  `
  return `data:image/svg+xml;base64,${btoa(svg)}`
}

/**
 * 生成头像占位图
 * @param name 用户名或团队名
 * @param size 尺寸
 * @returns Data URL 格式的 SVG 图片
 */
export function generateAvatarPlaceholder(name: string, size: number = 60): string {
  const initial = name.charAt(0).toUpperCase()
  const colors = [
    { bg: '#667eea', text: '#fff' },
    { bg: '#764ba2', text: '#fff' },
    { bg: '#f093fb', text: '#fff' },
    { bg: '#4facfe', text: '#fff' },
    { bg: '#43e97b', text: '#fff' },
    { bg: '#fa709a', text: '#fff' },
  ]
  
  // 根据名字生成一致的颜色
  const colorIndex = name.charCodeAt(0) % colors.length
  const color = colors[colorIndex]
  
  const svg = `
    <svg width="${size}" height="${size}" xmlns="http://www.w3.org/2000/svg">
      <circle cx="${size / 2}" cy="${size / 2}" r="${size / 2}" fill="${color.bg}"/>
      <text 
        x="50%" 
        y="50%" 
        dominant-baseline="middle" 
        text-anchor="middle" 
        font-family="Arial, sans-serif" 
        font-size="${size / 2}" 
        font-weight="600"
        fill="${color.text}"
      >
        ${initial}
      </text>
    </svg>
  `
  return `data:image/svg+xml;base64,${btoa(svg)}`
}

/**
 * 获取图片加载错误时的默认图片
 */
export const DEFAULT_AVATAR = generatePlaceholder(60, 60, '?', '#e0e0e0', '#999')
export const DEFAULT_TEAM_AVATAR = generatePlaceholder(60, 60, '团队', '#e0e0e0', '#999')
