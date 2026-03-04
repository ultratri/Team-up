/**
 * 路由预加�?Composable
 * 在用户鼠标悬停在导航链接上时预加载对应的路由组件
 */

import { onMounted, onUnmounted } from 'vue'
import type { Router } from 'vue-router'

// 已预加载的路由缓�?
const preloadedRoutes = new Set<string>()

/**
 * 预加载指定路由的组件
 */
export function preloadRoute(router: Router, routeName: string) {
  // 如果已经预加载过，直接返�?
  if (preloadedRoutes.has(routeName)) {
    return Promise.resolve()
  }

  const route = router.getRoutes().find(r => r.name === routeName)
  if (!route) {
    console.warn(`路由 ${routeName} 不存在`)
    return Promise.resolve()
  }

  // 获取路由组件
  const component = route.components?.default
  if (!component) {
    return Promise.resolve()
  }

  // 如果是懒加载组件（函数），执行预加载
  if (typeof component === 'function') {
    preloadedRoutes.add(routeName)
    return (component as () => Promise<any>)()
      .then(() => {
        
      })
      .catch((err: Error) => {
        console.error(`�?预加载路由失�? ${routeName}`, err)
        preloadedRoutes.delete(routeName)
      })
  }

  return Promise.resolve()
}

/**
 * 批量预加载路�?
 */
export function preloadRoutes(router: Router, routeNames: string[]) {
  return Promise.all(routeNames.map(name => preloadRoute(router, name)))
}

/**
 * 自动预加载路�?Hook
 * 监听导航链接的鼠标悬停事件，自动预加载对应路�?
 */
export function useRoutePreload(router: Router) {
  let hoverTimer: number | null = null

  const handleMouseEnter = (event: MouseEvent) => {
    const target = event.target
    
    // 确保target是HTMLElement
    if (!(target instanceof HTMLElement)) return
    
    const link = target.closest('a[href]') as HTMLAnchorElement
    
    if (!link) return

    // 清除之前的定时器
    if (hoverTimer) {
      clearTimeout(hoverTimer)
    }

    // 延迟预加载，避免用户快速划过时触发
    hoverTimer = window.setTimeout(() => {
      const href = link.getAttribute('href')
      if (!href || href.startsWith('http')) return

      try {
        const route = router.resolve(href)
        if (route.name) {
          preloadRoute(router, route.name as string)
        }
      } catch (err) {
        console.error('解析路由失败:', err)
      }
    }, 100) // 100ms 延迟
  }

  const handleMouseLeave = () => {
    if (hoverTimer) {
      clearTimeout(hoverTimer)
      hoverTimer = null
    }
  }

  onMounted(() => {
    document.addEventListener('mouseenter', handleMouseEnter, true)
    document.addEventListener('mouseleave', handleMouseLeave, true)
  })

  onUnmounted(() => {
    document.removeEventListener('mouseenter', handleMouseEnter, true)
    document.removeEventListener('mouseleave', handleMouseLeave, true)
    if (hoverTimer) {
      clearTimeout(hoverTimer)
    }
  })

  return {
    preloadRoute: (routeName: string) => preloadRoute(router, routeName),
    preloadRoutes: (routeNames: string[]) => preloadRoutes(router, routeNames)
  }
}

/**
 * 预加载常用路�?
 * 在应用启动后空闲时预加载常用页面
 */
export function preloadCommonRoutes(router: Router) {
  // ?? requestIdleCallback ??????????
  const idleCallback = (window as any).requestIdleCallback || ((cb: Function) => setTimeout(cb, 1))
  
  idleCallback(() => {
    const commonRoutes = [
      'Project',
      'CompetitionList',
      'EcosystemHub',
      'Profile',
      'Notifications',
      // 'Messages', // ?????????
    ]
    
    preloadRoutes(router, commonRoutes)
  })
}
