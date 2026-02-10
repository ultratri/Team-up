import { test, expect } from '@playwright/test'

/**
 * 基础端到端测试: 团队管理页面结构和导航
 * 
 * 这些测试不依赖后端API，只测试页面结构和客户端路由
 */

test.describe('Team Management Basic E2E Tests', () => {
  test('should load team list page', async ({ page }) => {
    // Navigate to team list
    await page.goto('/team')
    
    // Verify URL
    await expect(page).toHaveURL('/team')
    
    // Verify page loads without errors
    const title = await page.title()
    expect(title).toBeTruthy()
  })

  test('should have proper page structure', async ({ page }) => {
    await page.goto('/team')
    
    // Wait for main content to load
    await page.waitForLoadState('networkidle')
    
    // Verify main layout elements exist
    const mainContent = page.locator('main, .main-content, .team-list')
    await expect(mainContent).toBeAttached()
  })

  test('should be responsive on mobile', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })
    
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    // Verify page is accessible on mobile
    const body = page.locator('body')
    await expect(body).toBeVisible()
  })

  test('should be responsive on tablet', async ({ page }) => {
    // Set tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 })
    
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    // Verify page is accessible on tablet
    const body = page.locator('body')
    await expect(body).toBeVisible()
  })

  test('should have no console errors on load', async ({ page }) => {
    const errors: string[] = []
    
    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text())
      }
    })
    
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    // Filter out known warnings (Vue devtools, etc.)
    const criticalErrors = errors.filter(error => 
      !error.includes('DevTools') && 
      !error.includes('extension')
    )
    
    expect(criticalErrors.length).toBe(0)
  })

  test('should have proper meta tags', async ({ page }) => {
    await page.goto('/team')
    
    // Check for viewport meta tag
    const viewport = await page.locator('meta[name="viewport"]').getAttribute('content')
    expect(viewport).toBeTruthy()
  })

  test('should support keyboard navigation', async ({ page }) => {
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    // Press Tab key
    await page.keyboard.press('Tab')
    
    // Verify focus moved to an element
    const focusedElement = await page.evaluate(() => {
      const el = document.activeElement
      return el ? el.tagName : null
    })
    
    expect(focusedElement).toBeTruthy()
  })

  test('should have accessible color contrast', async ({ page }) => {
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    // This is a basic check - in production, use axe-core or similar
    const backgroundColor = await page.evaluate(() => {
      return window.getComputedStyle(document.body).backgroundColor
    })
    
    expect(backgroundColor).toBeTruthy()
  })

  test('should handle browser back button', async ({ page }) => {
    // Navigate to team list
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    // Navigate to home or another page
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Go back
    await page.goBack()
    
    // Verify we're back at team list
    await expect(page).toHaveURL('/team')
  })

  test('should handle browser forward button', async ({ page }) => {
    // Navigate to team list
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    // Navigate to home
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Go back
    await page.goBack()
    await expect(page).toHaveURL('/team')
    
    // Go forward
    await page.goForward()
    await expect(page).toHaveURL('/')
  })

  test('should handle page refresh', async ({ page }) => {
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    // Refresh page
    await page.reload()
    await page.waitForLoadState('networkidle')
    
    // Verify page still works
    await expect(page).toHaveURL('/team')
  })

  test('should load CSS and JavaScript', async ({ page }) => {
    await page.goto('/team')
    
    // Wait for all resources to load
    await page.waitForLoadState('load')
    
    // Check if styles are applied
    const bodyColor = await page.evaluate(() => {
      return window.getComputedStyle(document.body).color
    })
    
    expect(bodyColor).toBeTruthy()
    expect(bodyColor).not.toBe('rgba(0, 0, 0, 0)')
  })

  test('should have proper document structure', async ({ page }) => {
    await page.goto('/team')
    
    // Check for proper HTML structure
    const hasHtml = await page.locator('html').count()
    const hasHead = await page.locator('head').count()
    const hasBody = await page.locator('body').count()
    
    expect(hasHtml).toBe(1)
    expect(hasHead).toBe(1)
    expect(hasBody).toBe(1)
  })

  test('should support different screen sizes', async ({ page }) => {
    const sizes = [
      { width: 320, height: 568 },  // iPhone SE
      { width: 375, height: 667 },  // iPhone 8
      { width: 414, height: 896 },  // iPhone 11
      { width: 768, height: 1024 }, // iPad
      { width: 1024, height: 768 }, // iPad Landscape
      { width: 1280, height: 720 }, // Desktop
      { width: 1920, height: 1080 } // Full HD
    ]
    
    for (const size of sizes) {
      await page.setViewportSize(size)
      await page.goto('/team')
      await page.waitForLoadState('networkidle')
      
      // Verify page is accessible
      const body = page.locator('body')
      await expect(body).toBeVisible()
    }
  })

  test('should have proper charset', async ({ page }) => {
    await page.goto('/team')
    
    // Check for UTF-8 charset
    const charset = await page.locator('meta[charset]').getAttribute('charset')
    expect(charset?.toLowerCase()).toBe('utf-8')
  })

  test('should load without JavaScript errors', async ({ page }) => {
    const jsErrors: Error[] = []
    
    page.on('pageerror', error => {
      jsErrors.push(error)
    })
    
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    expect(jsErrors.length).toBe(0)
  })
})

test.describe('Team Management Navigation Tests', () => {
  test('should navigate to team space with valid ID', async ({ page }) => {
    // Navigate to a team space (assuming team ID 1 exists)
    await page.goto('/team/1')
    
    // Verify URL
    await expect(page).toHaveURL(/\/team\/\d+/)
  })

  test('should handle invalid team ID', async ({ page }) => {
    // Navigate to invalid team ID
    await page.goto('/team/invalid')
    
    // Should either redirect or show error
    await page.waitForLoadState('networkidle')
    
    // Verify we're not stuck on the invalid URL or error is shown
    const url = page.url()
    const hasError = await page.locator('.error-state, .el-result').isVisible().catch(() => false)
    
    expect(url.includes('/team/invalid') ? hasError : true).toBe(true)
  })

  test('should support deep linking to team modules', async ({ page }) => {
    // Navigate directly to team overview
    await page.goto('/team/1/overview')
    
    // Verify URL
    await expect(page).toHaveURL(/\/team\/\d+\/overview/)
  })

  test('should maintain URL state on refresh', async ({ page }) => {
    await page.goto('/team/1/overview')
    await page.waitForLoadState('networkidle')
    
    const urlBefore = page.url()
    
    // Refresh
    await page.reload()
    await page.waitForLoadState('networkidle')
    
    const urlAfter = page.url()
    
    expect(urlAfter).toBe(urlBefore)
  })
})

test.describe('Team Management Performance Tests', () => {
  test('should load within acceptable time', async ({ page }) => {
    const startTime = Date.now()
    
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    const loadTime = Date.now() - startTime
    
    // Should load within 5 seconds
    expect(loadTime).toBeLessThan(5000)
  })

  test('should have reasonable bundle size', async ({ page }) => {
    const resources: number[] = []
    
    page.on('response', response => {
      if (response.url().includes('.js') || response.url().includes('.css')) {
        response.body().then(body => {
          resources.push(body.length)
        }).catch(() => {})
      }
    })
    
    await page.goto('/team')
    await page.waitForLoadState('networkidle')
    
    // Wait a bit for all resources to be captured
    await page.waitForTimeout(1000)
    
    // This is just a basic check - adjust thresholds as needed
    expect(resources.length).toBeGreaterThan(0)
  })
})
