import { describe, it, expect, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import TeamCard from '@/views/team/components/TeamCard.vue'
import type { Team } from '@/types/team'
import { ElAvatar, ElTag, ElIcon } from 'element-plus'

// Helper to generate valid date strings
const dateArb = (minDate: string, maxDate: string) => 
  fc.integer({ 
    min: new Date(minDate).getTime(), 
    max: new Date(maxDate).getTime() 
  }).map(timestamp => new Date(timestamp).toISOString())

describe('Property Tests - TeamCard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  /**
   * ?? 2: ?????????
   * ????: 2.1.3
   * 
   * ???????????????????????????????????
   */
  it('Property 2: Team card information completeness - displays all required information', () => {
    // Feature: team-management, Property 2: Team card information completeness
    
    // ????????
    const teamArb = fc.record({
      id: fc.integer({ min: 1, max: 10000 }),
      name: fc.string({ minLength: 2, maxLength: 50 }),
      description: fc.option(
        fc.string({ minLength: 1, maxLength: 500 }),
        { nil: undefined }
      ),
      creatorId: fc.integer({ min: 1, max: 1000 }),
      status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
      createdAt: dateArb('2021-01-01', '2025-12-31'),
      updatedAt: dateArb('2021-01-01', '2025-12-31'),
      memberCount: fc.option(
        fc.integer({ min: 0, max: 1000 }),
        { nil: undefined }
      ),
      avatar: fc.option(
        fc.webUrl(),
        { nil: undefined }
      )
    })

    fc.assert(
      fc.property(teamArb, (team: Team) => {
        // ?? TeamCard ??
        const wrapper = mount(TeamCard, {
          props: { team },
          global: {
            components: { ElAvatar, ElTag, ElIcon }
          }
        })

        const text = wrapper.text()

        // ?? 1: ???????????trim?????
        const displayedName = team.name.trim() || team.name
        expect(text).toContain(displayedName)
        expect(wrapper.find('.team-name').text()).toBe(displayedName)

        // ?? 2: ?????????????????????"????"?
        const trimmedDescription = team.description?.trim()
        if (trimmedDescription) {
          expect(text).toContain(trimmedDescription)
          expect(wrapper.find('.team-description').text()).toBe(trimmedDescription)
        } else {
          expect(text).toContain('????')
          expect(wrapper.find('.team-description').text()).toBe('????')
        }

        // ?? 3: ????????
        const expectedMemberCount = team.memberCount ?? 0
        expect(text).toContain(`${expectedMemberCount} ?`)
        
        // ?? 4: ???????????????
        const metaItems = wrapper.findAll('.meta-item')
        expect(metaItems.length).toBeGreaterThanOrEqual(2) // ???????????
        
        // ??????????????
        const dateText = metaItems[1].text() // ??? meta-item ???
        expect(dateText.length).toBeGreaterThan(0)
        
        // ???????????????
        const hasTimeUnit = 
          dateText.includes('??') ||
          dateText.includes('??') ||
          dateText.includes('??') ||
          dateText.includes('??') ||
          dateText.includes('???') ||
          dateText.includes('??')
        expect(hasTimeUnit).toBe(true)

        return true
      }),
      { numRuns: 100 } // ?? 100 ???
    )
  })

  /**
   * ?? 2 ??: ????????? - ????
   * ????: 2.1.3
   * 
   * ??????????
   */
  it('Property 2 (extended): Team card information completeness - handles edge cases', () => {
    // Feature: team-management, Property 2: Team card information completeness
    
    // ?????????????
    const edgeCaseTeamArb = fc.record({
      id: fc.integer({ min: 1, max: 10000 }),
      name: fc.oneof(
        // ????
        fc.string({ minLength: 2, maxLength: 2 }),
        // ????
        fc.string({ minLength: 50, maxLength: 50 }),
        // ?????????
        fc.string({ minLength: 5, maxLength: 20 }).map(s => s.replace(/[^a-zA-Z0-9\u4e00-\u9fa5_]/g, '_'))
      ),
      description: fc.oneof(
        // ???
        fc.constant(undefined),
        fc.constant(''),
        // ????
        fc.string({ minLength: 500, maxLength: 500 }),
        // ???????
        fc.constant('???\n???\n???')
      ),
      creatorId: fc.integer({ min: 1, max: 1000 }),
      status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
      createdAt: fc.oneof(
        // ??
        fc.constant(new Date().toISOString()),
        // ??
        fc.constant(new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString()),
        // ????
        fc.constant(new Date('2021-01-01').toISOString()),
        // ????????????????
        fc.constant(new Date('2027-01-01').toISOString())
      ),
      updatedAt: dateArb('2021-01-01', '2025-12-31'),
      memberCount: fc.oneof(
        // ???
        fc.constant(0),
        // ???
        fc.constant(undefined),
        // ????
        fc.constant(9999),
        // ????
        fc.integer({ min: 1, max: 100 })
      )
    })

    fc.assert(
      fc.property(edgeCaseTeamArb, (team: Team) => {
        const wrapper = mount(TeamCard, {
          props: { team },
          global: {
            components: { ElAvatar, ElTag, ElIcon }
          }
        })

        const text = wrapper.text()

        // ??????????????????????
        const displayedName = team.name.trim() || team.name
        expect(text).toContain(displayedName)
        
        const trimmedDescription = team.description?.trim()
        if (trimmedDescription) {
          expect(text).toContain(trimmedDescription)
        } else {
          expect(text).toContain('????')
        }

        const expectedMemberCount = team.memberCount ?? 0
        expect(text).toContain(`${expectedMemberCount} ?`)

        // ?????????
        expect(wrapper.exists()).toBe(true)
        expect(wrapper.find('.team-card').exists()).toBe(true)

        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * ????: ????????
   * ??????????????
   */
  it('Property: Team card clickability - all cards are clickable', () => {
    const teamArb = fc.record({
      id: fc.integer({ min: 1, max: 10000 }),
      name: fc.string({ minLength: 2, maxLength: 50 }),
      description: fc.option(fc.string({ maxLength: 500 })),
      creatorId: fc.integer({ min: 1, max: 1000 }),
      status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
      createdAt: dateArb('2021-01-01', '2025-12-31'),
      updatedAt: dateArb('2021-01-01', '2025-12-31'),
      memberCount: fc.option(fc.integer({ min: 0, max: 1000 }))
    })

    fc.assert(
      fc.property(teamArb, (team: Team) => {
        const wrapper = mount(TeamCard, {
          props: { team },
          global: {
            components: { ElAvatar, ElTag, ElIcon }
          }
        })

        const card = wrapper.find('.team-card')

        // ???????
        expect(card.exists()).toBe(true)

        // ???????????????
        expect(card.attributes('role')).toBe('button')
        expect(card.attributes('tabindex')).toBe('0')

        // ????????????
        card.trigger('click')
        expect(wrapper.emitted('click')).toBeTruthy()
        expect(wrapper.emitted('click')).toHaveLength(1)

        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * ????: ????????
   * ???????????????
   */
  it('Property: Team card keyboard navigation - supports Enter and Space keys', () => {
    const teamArb = fc.record({
      id: fc.integer({ min: 1, max: 10000 }),
      name: fc.string({ minLength: 2, maxLength: 50 }),
      creatorId: fc.integer({ min: 1, max: 1000 }),
      status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
      createdAt: dateArb('2021-01-01', '2025-12-31'),
      updatedAt: dateArb('2021-01-01', '2025-12-31')
    })

    fc.assert(
      fc.property(teamArb, (team: Team) => {
        const wrapper = mount(TeamCard, {
          props: { team },
          global: {
            components: { ElAvatar, ElTag, ElIcon }
          }
        })

        const card = wrapper.find('.team-card')

        // ???Enter ?????
        card.trigger('keydown.enter')
        expect(wrapper.emitted('click')).toBeTruthy()
        expect(wrapper.emitted('click')).toHaveLength(1)

        // ???Space ?????
        card.trigger('keydown.space')
        expect(wrapper.emitted('click')).toHaveLength(2)

        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * ????: ????????
   * ??????????????????????
   */
  it('Property: Date formatting consistency - formats all valid dates', () => {
    const teamArb = fc.record({
      id: fc.integer({ min: 1, max: 10000 }),
      name: fc.string({ minLength: 2, maxLength: 50 }),
      creatorId: fc.integer({ min: 1, max: 1000 }),
      status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
      createdAt: fc.integer({ 
        min: new Date('2021-01-01').getTime(), 
        max: new Date('2025-12-31').getTime() 
      }).map(timestamp => new Date(timestamp).toISOString()),
      updatedAt: fc.integer({ 
        min: new Date('2021-01-01').getTime(), 
        max: new Date('2025-12-31').getTime() 
      }).map(timestamp => new Date(timestamp).toISOString())
    })

    fc.assert(
      fc.property(teamArb, (team: Team) => {
        const wrapper = mount(TeamCard, {
          props: { team },
          global: {
            components: { ElAvatar, ElTag, ElIcon }
          }
        })

        const metaItems = wrapper.findAll('.meta-item')
        expect(metaItems.length).toBeGreaterThanOrEqual(2)

        const dateText = metaItems[1].text()
        
        // ??????????
        expect(dateText.length).toBeGreaterThan(0)
        
        // ????????????????
        const validTimeUnits = ['??', '??', '??', '??', '???', '??']
        const hasValidTimeUnit = validTimeUnits.some(unit => dateText.includes(unit))
        expect(hasValidTimeUnit).toBe(true)

        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * ????: ?????????
   * ????????????
   */
  it('Property: Member count display consistency - always shows correct count', () => {
    const teamArb = fc.record({
      id: fc.integer({ min: 1, max: 10000 }),
      name: fc.string({ minLength: 2, maxLength: 50 }),
      creatorId: fc.integer({ min: 1, max: 1000 }),
      status: fc.constantFrom('ACTIVE', 'ARCHIVED', 'DISBANDED'),
      createdAt: dateArb('2021-01-01', '2025-12-31'),
      updatedAt: dateArb('2021-01-01', '2025-12-31'),
      memberCount: fc.option(
        fc.integer({ min: 0, max: 10000 }),
        { nil: undefined }
      )
    })

    fc.assert(
      fc.property(teamArb, (team: Team) => {
        const wrapper = mount(TeamCard, {
          props: { team },
          global: {
            components: { ElAvatar, ElTag, ElIcon }
          }
        })

        const expectedCount = team.memberCount ?? 0
        const text = wrapper.text()

        // ???????????
        expect(text).toContain(`${expectedCount} ?`)

        // ???????? meta-item ?
        const metaItems = wrapper.findAll('.meta-item')
        const memberCountItem = metaItems[0] // ??? meta-item ????
        expect(memberCountItem.text()).toContain(`${expectedCount} ?`)

        return true
      }),
      { numRuns: 100 }
    )
  })
})
