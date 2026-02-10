import { describe, it, expect } from 'vitest'
import {
  validateTeamName,
  validateTeamDescription,
  teamNameRules,
  teamDescriptionRules
} from '@/utils/validation'

describe('Validation Utils', () => {
  describe('validateTeamName', () => {
    it('should accept valid team names', () => {
      const validNames = [
        '前端开发团队',
        'Frontend_Team',
        'Team123',
        '团队_01',
        'AB',
        '中文English123_'
      ]

      validNames.forEach(name => {
        const result = validateTeamName(name)
        expect(result.valid).toBe(true)
        expect(result.message).toBeUndefined()
      })
    })

    it('should reject empty team name', () => {
      const result = validateTeamName('')
      expect(result.valid).toBe(false)
      expect(result.message).toBe('请输入团队名称')
    })

    it('should reject whitespace-only team name', () => {
      const result = validateTeamName('   ')
      expect(result.valid).toBe(false)
      expect(result.message).toBe('请输入团队名称')
    })

    it('should reject team name shorter than 2 characters', () => {
      const result = validateTeamName('A')
      expect(result.valid).toBe(false)
      expect(result.message).toBe('团队名称至少需要 2 个字符')
    })

    it('should reject team name longer than 50 characters', () => {
      const longName = 'A'.repeat(51)
      const result = validateTeamName(longName)
      expect(result.valid).toBe(false)
      expect(result.message).toBe('团队名称不能超过 50 个字符')
    })

    it('should accept team name with exactly 2 characters', () => {
      const result = validateTeamName('AB')
      expect(result.valid).toBe(true)
    })

    it('should accept team name with exactly 50 characters', () => {
      const name = 'A'.repeat(50)
      const result = validateTeamName(name)
      expect(result.valid).toBe(true)
    })

    it('should reject team name with special characters', () => {
      const invalidNames = [
        'Team@123',
        'Team#Name',
        'Team Name',
        'Team-01',
        'Team.Name',
        'Team!',
        'Team$',
        'Team%',
        'Team&',
        'Team*',
        'Team+',
        'Team=',
        'Team[',
        'Team]',
        'Team{',
        'Team}',
        'Team|',
        'Team\\',
        'Team/',
        'Team?',
        'Team<',
        'Team>',
        'Team,',
        'Team;',
        'Team:',
        'Team"',
        "Team'"
      ]

      invalidNames.forEach(name => {
        const result = validateTeamName(name)
        expect(result.valid).toBe(false)
        expect(result.message).toBe('团队名称只能包含中文、英文、数字和下划线')
      })
    })

    it('should accept team name with Chinese characters', () => {
      const result = validateTeamName('前端开发团队')
      expect(result.valid).toBe(true)
    })

    it('should accept team name with English characters', () => {
      const result = validateTeamName('FrontendTeam')
      expect(result.valid).toBe(true)
    })

    it('should accept team name with numbers', () => {
      const result = validateTeamName('Team123')
      expect(result.valid).toBe(true)
    })

    it('should accept team name with underscores', () => {
      const result = validateTeamName('Team_Name_01')
      expect(result.valid).toBe(true)
    })

    it('should accept team name with mixed characters', () => {
      const result = validateTeamName('前端Team_123')
      expect(result.valid).toBe(true)
    })

    it('should trim whitespace before validation', () => {
      const result = validateTeamName('  ValidTeam  ')
      expect(result.valid).toBe(true)
    })
  })

  describe('validateTeamDescription', () => {
    it('should accept valid descriptions', () => {
      const validDescriptions = [
        '这是一个团队描述',
        'This is a team description',
        '包含特殊字符的描述：@#$%^&*()',
        '多行描述\n第二行\n第三行',
        ''
      ]

      validDescriptions.forEach(desc => {
        const result = validateTeamDescription(desc)
        expect(result.valid).toBe(true)
        expect(result.message).toBeUndefined()
      })
    })

    it('should accept undefined description', () => {
      const result = validateTeamDescription(undefined)
      expect(result.valid).toBe(true)
    })

    it('should accept empty description', () => {
      const result = validateTeamDescription('')
      expect(result.valid).toBe(true)
    })

    it('should reject description longer than 500 characters', () => {
      const longDesc = 'A'.repeat(501)
      const result = validateTeamDescription(longDesc)
      expect(result.valid).toBe(false)
      expect(result.message).toBe('团队描述不能超过 500 个字符')
    })

    it('should accept description with exactly 500 characters', () => {
      const desc = 'A'.repeat(500)
      const result = validateTeamDescription(desc)
      expect(result.valid).toBe(true)
    })

    it('should accept description with special characters', () => {
      const desc = '特殊字符：!@#$%^&*()_+-=[]{}|;:\'",.<>?/~`'
      const result = validateTeamDescription(desc)
      expect(result.valid).toBe(true)
    })

    it('should accept description with emojis', () => {
      const desc = '团队描述 😀 🎉 🚀'
      const result = validateTeamDescription(desc)
      expect(result.valid).toBe(true)
    })
  })

  describe('teamNameRules', () => {
    it('should have required rule', () => {
      const requiredRule = teamNameRules.find(rule => rule.required)
      expect(requiredRule).toBeDefined()
      expect(requiredRule?.message).toBe('请输入团队名称')
      expect(requiredRule?.trigger).toBe('blur')
    })

    it('should have length rule', () => {
      const lengthRule = teamNameRules.find(rule => 'min' in rule && 'max' in rule)
      expect(lengthRule).toBeDefined()
      expect(lengthRule?.min).toBe(2)
      expect(lengthRule?.max).toBe(50)
      expect(lengthRule?.message).toBe('长度在 2 到 50 个字符')
      expect(lengthRule?.trigger).toBe('blur')
    })

    it('should have pattern rule', () => {
      const patternRule = teamNameRules.find(rule => 'pattern' in rule)
      expect(patternRule).toBeDefined()
      expect(patternRule?.pattern).toBeInstanceOf(RegExp)
      expect(patternRule?.message).toBe('只能包含中文、英文、数字和下划线')
      expect(patternRule?.trigger).toBe('blur')
    })

    it('should have exactly 3 rules', () => {
      expect(teamNameRules).toHaveLength(3)
    })
  })

  describe('teamDescriptionRules', () => {
    it('should have max length rule', () => {
      const maxRule = teamDescriptionRules.find(rule => 'max' in rule)
      expect(maxRule).toBeDefined()
      expect(maxRule?.max).toBe(500)
      expect(maxRule?.message).toBe('描述不能超过 500 个字符')
      expect(maxRule?.trigger).toBe('blur')
    })

    it('should have exactly 1 rule', () => {
      expect(teamDescriptionRules).toHaveLength(1)
    })

    it('should not have required rule', () => {
      const requiredRule = teamDescriptionRules.find(rule => 'required' in rule)
      expect(requiredRule).toBeUndefined()
    })
  })

  describe('Edge cases', () => {
    it('should handle null input for team name', () => {
      const result = validateTeamName(null as any)
      expect(result.valid).toBe(false)
    })

    it('should handle undefined input for team name', () => {
      const result = validateTeamName(undefined as any)
      expect(result.valid).toBe(false)
    })

    it('should handle numeric input for team name', () => {
      const result = validateTeamName(123 as any)
      expect(result.valid).toBe(false)
    })

    it('should handle object input for team name', () => {
      const result = validateTeamName({} as any)
      expect(result.valid).toBe(false)
    })
  })
})
