import gsap from 'gsap'

/**
 * 页面转场动画
 */
export function usePageTransition() {
  const fadeIn = (element: HTMLElement, duration = 0.5) => {
    gsap.from(element, {
      opacity: 0,
      y: 20,
      duration,
      ease: 'power2.out',
    })
  }

  const slideIn = (element: HTMLElement, direction: 'left' | 'right' | 'top' | 'bottom' = 'left') => {
    const offset: Record<string, any> = {
      left: { x: -50, y: 0 },
      right: { x: 50, y: 0 },
      top: { x: 0, y: -50 },
      bottom: { x: 0, y: 50 },
    }

    gsap.from(element, {
      opacity: 0,
      ...offset[direction],
      duration: 0.5,
      ease: 'power2.out',
    })
  }

  const scaleIn = (element: HTMLElement) => {
    gsap.from(element, {
      opacity: 0,
      scale: 0.9,
      duration: 0.4,
      ease: 'back.out(1.7)',
    })
  }

  return {
    fadeIn,
    slideIn,
    scaleIn,
  }
}

/**
 * 列表交错动画
 */
export function useStaggerAnimation() {
  const staggerFadeIn = (elements: NodeListOf<Element> | Element[], delay = 0.1) => {
    gsap.from(elements, {
      opacity: 0,
      y: 20,
      duration: 0.5,
      stagger: delay,
      ease: 'power2.out',
    })
  }

  return {
    staggerFadeIn,
  }
}

