/**
 * 图片懒加载 Composable
 * 使用 Intersection Observer API 实现高性能的图片懒加载
 */

import { onMounted, onUnmounted } from 'vue'

interface LazyLoadOptions {
  root?: Element | null
  rootMargin?: string
  threshold?: number | number[]
}

/**
 * 图片懒加载 Hook
 * @param selector 图片选择器
 * @param options Intersection Observer 选项
 */
export function useLazyLoad(
  selector: string = 'img[data-src]',
  options: LazyLoadOptions = {}
) {
  let observer: IntersectionObserver | null = null

  const defaultOptions: IntersectionObserverInit = {
    root: options.root || null,
    rootMargin: options.rootMargin || '50px',
    threshold: options.threshold || 0.01
  }

  const loadImage = (img: HTMLImageElement) => {
    const src = img.getAttribute('data-src')
    if (!src) return

    img.src = src
    img.removeAttribute('data-src')
    img.classList.add('loaded')
  }

  const observeImages = () => {
    const images = document.querySelectorAll(selector)
    
    if (!observer) {
      observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const img = entry.target as HTMLImageElement
            loadImage(img)
            observer?.unobserve(img)
          }
        })
      }, defaultOptions)
    }

    images.forEach((img) => {
      observer?.observe(img)
    })
  }

  onMounted(() => {
    observeImages()
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  return {
    observeImages
  }
}

/**
 * 防抖函数
 * @param fn 要防抖的函数
 * @param delay 延迟时间（毫秒）
 */
export function useDebounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number = 300
): (...args: Parameters<T>) => void {
  let timeoutId: number | null = null

  return function (this: any, ...args: Parameters<T>) {
    if (timeoutId !== null) {
      clearTimeout(timeoutId)
    }

    timeoutId = window.setTimeout(() => {
      fn.apply(this, args)
      timeoutId = null
    }, delay)
  }
}

/**
 * 节流函数
 * @param fn 要节流的函数
 * @param delay 延迟时间（毫秒）
 */
export function useThrottle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number = 300
): (...args: Parameters<T>) => void {
  let lastCall = 0

  return function (this: any, ...args: Parameters<T>) {
    const now = Date.now()

    if (now - lastCall >= delay) {
      lastCall = now
      fn.apply(this, args)
    }
  }
}
