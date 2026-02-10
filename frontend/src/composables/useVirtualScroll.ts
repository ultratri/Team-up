/**
 * Virtual Scroll Composable
 * Performance optimization for rendering large lists
 * Only renders visible items in the viewport
 */

import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

interface VirtualScrollOptions {
  itemHeight: number
  containerHeight: number
  buffer?: number
}

export function useVirtualScroll<T>(
  items: T[],
  options: VirtualScrollOptions
) {
  const { itemHeight, containerHeight, buffer = 5 } = options
  
  const scrollTop = ref(0)
  const containerRef = ref<HTMLElement | null>(null)
  
  // Calculate visible range
  const visibleRange = computed(() => {
    const start = Math.floor(scrollTop.value / itemHeight)
    const visibleCount = Math.ceil(containerHeight / itemHeight)
    const end = start + visibleCount
    
    return {
      start: Math.max(0, start - buffer),
      end: Math.min(items.length, end + buffer)
    }
  })
  
  // Get visible items
  const visibleItems = computed(() => {
    const { start, end } = visibleRange.value
    return items.slice(start, end).map((item, index) => ({
      item,
      index: start + index
    }))
  })
  
  // Calculate total height
  const totalHeight = computed(() => items.length * itemHeight)
  
  // Calculate offset for positioning
  const offsetY = computed(() => visibleRange.value.start * itemHeight)
  
  // Handle scroll event
  const handleScroll = (event: Event) => {
    const target = event.target as HTMLElement
    scrollTop.value = target.scrollTop
  }
  
  // Setup scroll listener
  onMounted(() => {
    if (containerRef.value) {
      containerRef.value.addEventListener('scroll', handleScroll)
    }
  })
  
  onUnmounted(() => {
    if (containerRef.value) {
      containerRef.value.removeEventListener('scroll', handleScroll)
    }
  })
  
  return {
    containerRef,
    visibleItems,
    totalHeight,
    offsetY,
    scrollTop
  }
}
