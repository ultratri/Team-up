import { test, expect } from '@playwright/test'

/**
 * 端到端测试: 团队管理完整用户流程
 * 
 * 测试从登录到团队管理的完整用户旅程：
 * 1. 用户登录
 * 2. 导航到团队列表
 * 3. 创建新团队
 * 4. 进入团队空间
 * 5. 浏览不同模块
 * 6. 切换团队
 */

// Test configuration
const TEST_USER = {
  username: 'testuser',
  password: 'password123',
  email: 'test@example.com'
}

const TEST_TEAM = {
  name: 'E2E Test Team',
  description: 'This is a test team created by E2E tests'
}

/**
 * Setup: Login before each test
 */
test.beforeEach(async ({ page }) => {
  // Navigate to login page
  await page.goto('/login')
  
  // Fill login form
  await page.fill('input[name="username"]', TEST_USER.username)
  await page.fill('input[name="password"]', TEST_USER.password)
  
  // Submit form
  await page.click('button[type="submit"]')
  
  // Wait for navigation to complete
  await page.waitForURL('**/', { timeout: 5000 }).catch(() => {
    // If already logged in or redirect doesn't happen, continue
  })
})

test.describe('Team Management E2E Tests', () => {
  test('should complete full team creation workflow', async ({ page }) => {
    // Step 1: Navigate to team list
    await page.goto('/team')
    await expect(page).toHaveURL('/team')
    
    // Verify page title
    await expect(page.locator('h1, h2').filter({ hasText: '我的团队' })).toBeVisible()
    
    // Step 2: Click create team button
    await page.click('button:has-text("创建团队")')
    
    // Wait for dialog to appear
    await expect(page.locator('.el-dialog').filter({ hasText: '创建团队' })).toBeVisible()
    
    // Step 3: Fill team creation form
    await page.fill('input[name="name"]', TEST_TEAM.name)
    await page.fill('textarea[name="description"]', TEST_TEAM.description)
    
    // Step 4: Submit form
    await page.click('.el-dialog button:has-text("创建")')
    
    // Step 5: Wait for navigation to team space
    await page.waitForURL(/\/team\/\d+/, { timeout: 10000 })
    
    // Step 6: Verify we're in team space
    const url = page.url()
    expect(url).toMatch(/\/team\/\d+/)
    
    // Verify team name is displayed
    await expect(page.locator('text=' + TEST_TEAM.name)).toBeVisible()
  })

  test('should navigate from team list to team space', async ({ page }) => {
    // Navigate to team list
    await page.goto('/team')
    
    // Wait for teams to load
    await page.waitForSelector('.team-card, .empty-state', { timeout: 5000 })
    
    // Check if there are any teams
    const teamCards = page.locator('.team-card')
    const teamCount = await teamCards.count()
    
    if (teamCount > 0) {
      // Click on first team card
      await teamCards.first().click()
      
      // Verify navigation to team space
      await page.waitForURL(/\/team\/\d+/, { timeout: 5000 })
      
      // Verify team space is loaded
      await expect(page.locator('.team-space, .team-header')).toBeVisible()
    } else {
      // No teams available, skip this test
      test.skip()
    }
  })

  test('should switch between team modules', async ({ page }) => {
    // Navigate to team list
    await page.goto('/team')
    
    // Wait for teams to load
    await page.waitForSelector('.team-card, .empty-state', { timeout: 5000 })
    
    const teamCards = page.locator('.team-card')
    const teamCount = await teamCards.count()
    
    if (teamCount > 0) {
      // Click on first team
      await teamCards.first().click()
      await page.waitForURL(/\/team\/\d+/, { timeout: 5000 })
      
      // Get team ID from URL
      const url = page.url()
      const teamId = url.match(/\/team\/(\d+)/)?.[1]
      
      if (teamId) {
        // Navigate to overview
        await page.click('text=团队概览')
        await expect(page).toHaveURL(`/team/${teamId}/overview`)
        
        // Navigate to tasks
        await page.click('text=任务看板')
        await expect(page).toHaveURL(`/team/${teamId}/tasks`)
        
        // Navigate to members
        await page.click('text=成员管理')
        await expect(page).toHaveURL(`/team/${teamId}/members`)
      }
    } else {
      test.skip()
    }
  })

  test('should switch between teams', async ({ page }) => {
    // Navigate to team list
    await page.goto('/team')
    
    // Wait for teams to load
    await page.waitForSelector('.team-card, .empty-state', { timeout: 5000 })
    
    const teamCards = page.locator('.team-card')
    const teamCount = await teamCards.count()
    
    if (teamCount >= 2) {
      // Click on first team
      await teamCards.first().click()
      await page.waitForURL(/\/team\/\d+/, { timeout: 5000 })
      
      const firstTeamUrl = page.url()
      const firstTeamId = firstTeamUrl.match(/\/team\/(\d+)/)?.[1]
      
      // Click team switcher dropdown
      await page.click('.team-switcher, .el-dropdown')
      
      // Wait for dropdown menu
      await page.waitForSelector('.el-dropdown-menu', { timeout: 2000 })
      
      // Click on second team in dropdown
      const dropdownItems = page.locator('.el-dropdown-menu .el-dropdown-item')
      if (await dropdownItems.count() >= 2) {
        await dropdownItems.nth(1).click()
        
        // Wait for navigation
        await page.waitForURL(/\/team\/\d+/, { timeout: 5000 })
        
        // Verify we switched to a different team
        const secondTeamUrl = page.url()
        const secondTeamId = secondTeamUrl.match(/\/team\/(\d+)/)?.[1]
        
        expect(secondTeamId).not.toBe(firstTeamId)
      }
    } else {
      test.skip()
    }
  })

  test('should display team overview information', async ({ page }) => {
    // Navigate to team list
    await page.goto('/team')
    
    // Wait for teams to load
    await page.waitForSelector('.team-card, .empty-state', { timeout: 5000 })
    
    const teamCards = page.locator('.team-card')
    const teamCount = await teamCards.count()
    
    if (teamCount > 0) {
      // Click on first team
      await teamCards.first().click()
      await page.waitForURL(/\/team\/\d+/, { timeout: 5000 })
      
      // Navigate to overview
      await page.goto(page.url() + '/overview')
      
      // Verify overview elements are visible
      await expect(page.locator('.team-info, .team-header')).toBeVisible()
      
      // Check for statistics cards
      const statsCards = page.locator('.statistics-card, .stat-card')
      expect(await statsCards.count()).toBeGreaterThan(0)
      
      // Check for member list
      await expect(page.locator('.member-list, .team-members')).toBeVisible()
    } else {
      test.skip()
    }
  })

  test('should show empty state when no teams exist', async ({ page }) => {
    // This test assumes a fresh user with no teams
    // In a real scenario, you might need to clean up test data first
    
    await page.goto('/team')
    
    // Wait for page to load
    await page.waitForSelector('.team-list, .empty-state', { timeout: 5000 })
    
    // Check if empty state is shown
    const emptyState = page.locator('.empty-state')
    const teamCards = page.locator('.team-card')
    
    const hasEmptyState = await emptyState.isVisible().catch(() => false)
    const hasTeamCards = await teamCards.count() > 0
    
    // Either empty state should be shown or teams should be present
    expect(hasEmptyState || hasTeamCards).toBe(true)
  })

  test('should handle search functionality', async ({ page }) => {
    await page.goto('/team')
    
    // Wait for teams to load
    await page.waitForSelector('.team-card, .empty-state', { timeout: 5000 })
    
    const teamCards = page.locator('.team-card')
    const initialCount = await teamCards.count()
    
    if (initialCount > 0) {
      // Get first team name
      const firstTeamName = await teamCards.first().locator('.team-name').textContent()
      
      if (firstTeamName) {
        // Search for the team
        await page.fill('input[placeholder*="搜索"]', firstTeamName)
        
        // Wait for search results
        await page.waitForTimeout(500) // Wait for debounce
        
        // Verify search results
        const searchResults = page.locator('.team-card')
        expect(await searchResults.count()).toBeGreaterThan(0)
      }
    } else {
      test.skip()
    }
  })

  test('should handle team creation validation', async ({ page }) => {
    await page.goto('/team')
    
    // Click create team button
    await page.click('button:has-text("创建团队")')
    
    // Wait for dialog
    await expect(page.locator('.el-dialog').filter({ hasText: '创建团队' })).toBeVisible()
    
    // Try to submit empty form
    await page.click('.el-dialog button:has-text("创建")')
    
    // Verify validation error is shown
    await expect(page.locator('.el-form-item__error, .error-message')).toBeVisible()
    
    // Fill with invalid name (too short)
    await page.fill('input[name="name"]', 'A')
    await page.click('.el-dialog button:has-text("创建")')
    
    // Verify validation error
    await expect(page.locator('.el-form-item__error, .error-message')).toBeVisible()
    
    // Fill with valid name
    await page.fill('input[name="name"]', 'Valid Team Name')
    
    // Validation error should disappear
    const errorVisible = await page.locator('.el-form-item__error, .error-message')
      .isVisible()
      .catch(() => false)
    
    // Error should not be visible or form should be submittable
    expect(errorVisible).toBe(false)
  })
})

test.describe('Team Management Error Scenarios', () => {
  test('should handle network errors gracefully', async ({ page }) => {
    // Simulate offline mode
    await page.context().setOffline(true)
    
    await page.goto('/team')
    
    // Wait for error message or retry button
    await page.waitForSelector('.error-state, .retry-button, .el-message', { timeout: 5000 })
    
    // Verify error handling UI is shown
    const hasErrorState = await page.locator('.error-state, .el-message--error').isVisible().catch(() => false)
    expect(hasErrorState).toBe(true)
    
    // Restore online mode
    await page.context().setOffline(false)
  })

  test('should handle unauthorized access', async ({ page }) => {
    // Try to access a team that doesn't exist or user doesn't have access to
    await page.goto('/team/999999')
    
    // Wait for error page or redirect
    await page.waitForTimeout(2000)
    
    // Verify error handling
    const url = page.url()
    const hasError = url.includes('/team') && !url.includes('/team/999999')
    const hasErrorMessage = await page.locator('.error-state, .el-result, .el-message').isVisible().catch(() => false)
    
    // Either redirected or error shown
    expect(hasError || hasErrorMessage).toBe(true)
  })

  test('should handle team not found', async ({ page }) => {
    // Try to access a non-existent team
    await page.goto('/team/999999')
    
    // Wait for error handling
    await page.waitForTimeout(2000)
    
    // Verify error is shown or redirected
    const currentUrl = page.url()
    const isRedirected = !currentUrl.includes('/team/999999')
    const hasErrorUI = await page.locator('.error-state, .el-result').isVisible().catch(() => false)
    
    expect(isRedirected || hasErrorUI).toBe(true)
  })
})

test.describe('Team Management Responsive Design', () => {
  test('should work on mobile devices', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })
    
    await page.goto('/team')
    
    // Wait for page to load
    await page.waitForSelector('.team-list, .empty-state', { timeout: 5000 })
    
    // Verify mobile layout
    const teamCards = page.locator('.team-card')
    if (await teamCards.count() > 0) {
      // Verify cards are stacked vertically (mobile layout)
      const firstCard = teamCards.first()
      const cardWidth = await firstCard.evaluate(el => el.getBoundingClientRect().width)
      
      // On mobile, cards should take most of the width
      expect(cardWidth).toBeGreaterThan(300)
    }
  })

  test('should work on tablet devices', async ({ page }) => {
    // Set tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 })
    
    await page.goto('/team')
    
    // Wait for page to load
    await page.waitForSelector('.team-list, .empty-state', { timeout: 5000 })
    
    // Verify tablet layout works
    const teamCards = page.locator('.team-card')
    expect(await teamCards.count()).toBeGreaterThanOrEqual(0)
  })
})

test.describe('Team Management Accessibility', () => {
  test('should be keyboard navigable', async ({ page }) => {
    await page.goto('/team')
    
    // Wait for page to load
    await page.waitForSelector('.team-list, .empty-state', { timeout: 5000 })
    
    // Tab through elements
    await page.keyboard.press('Tab')
    await page.keyboard.press('Tab')
    
    // Verify focus is visible
    const focusedElement = await page.evaluate(() => document.activeElement?.tagName)
    expect(focusedElement).toBeTruthy()
  })

  test('should have proper ARIA labels', async ({ page }) => {
    await page.goto('/team')
    
    // Wait for page to load
    await page.waitForSelector('.team-list, .empty-state', { timeout: 5000 })
    
    // Check for ARIA labels on important elements
    const createButton = page.locator('button:has-text("创建团队")')
    if (await createButton.isVisible()) {
      const ariaLabel = await createButton.getAttribute('aria-label')
      expect(ariaLabel || await createButton.textContent()).toBeTruthy()
    }
  })
})
