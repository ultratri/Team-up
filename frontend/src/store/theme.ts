import { defineStore } from 'pinia'

export type ThemeMode = 'system' | 'light' | 'dark'

interface ThemeState {
  mode: ThemeMode
  primaryColor: string
}

const STORAGE_KEY = 'teamup-theme'

export const useThemeStore = defineStore('theme', {
  state: (): ThemeState => ({
    mode: 'light',
    primaryColor: '#3b82f6',
  }),
  getters: {
    effectiveMode(state): 'light' | 'dark' {
      if (state.mode === 'system') {
        if (
          typeof window !== 'undefined' &&
          typeof window.matchMedia === 'function' &&
          window.matchMedia('(prefers-color-scheme: dark)').matches
        ) {
          return 'dark'
        }
        return 'light'
      }
      return state.mode
    },
  },
  actions: {
    initTheme() {
      if (typeof window === 'undefined') return

      const saved = window.localStorage.getItem(STORAGE_KEY)
      if (saved) {
        try {
          const parsed = JSON.parse(saved) as Partial<ThemeState>
          if (parsed.mode) {
            this.mode = parsed.mode as ThemeMode
          }
          if (parsed.primaryColor) {
            this.primaryColor = parsed.primaryColor
          }
        } catch (error) {
          console.error(error)
        }
      }

      this.applyTheme()

      if (typeof window.matchMedia === 'function') {
        const media = window.matchMedia('(prefers-color-scheme: dark)')
        const handler = (event: MediaQueryListEvent) => {
          if (this.mode === 'system') {
            this.applyTheme(event.matches ? 'dark' : 'light')
          }
        }

        if (typeof media.addEventListener === 'function') {
          media.addEventListener('change', handler)
        } else if (typeof media.addListener === 'function') {
          media.addListener(handler)
        }
      }
    },
    setMode(mode: ThemeMode) {
      this.mode = mode
      this.persist()
      this.applyTheme()
    },
    setPrimaryColor(color: string) {
      this.primaryColor = color
      this.persist()
      this.applyTheme()
    },
    resetToDefault() {
      this.mode = 'light'
      this.primaryColor = '#3b82f6'
      this.persist()
      this.applyTheme()
    },
    persist() {
      if (typeof window === 'undefined') return

      const data: ThemeState = {
        mode: this.mode,
        primaryColor: this.primaryColor,
      }

      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    },
    applyTheme(forcedMode?: 'light' | 'dark') {
      if (typeof document === 'undefined') return

      const theme = forcedMode ?? this.effectiveMode
      const root = document.documentElement

      root.setAttribute('data-theme', theme)
      root.style.setProperty('--accent-color', this.primaryColor)
      root.style.setProperty('color-scheme', theme === 'dark' ? 'dark' : 'light')

      // Calculate and set derived variables
      const rgb = this.hexToRgb(this.primaryColor)
      if (rgb) {
        root.style.setProperty('--accent-color-rgb', `${rgb.r}, ${rgb.g}, ${rgb.b}`)
      }
      
      const darkVariant = this.adjustBrightness(this.primaryColor, -20)
      root.style.setProperty('--accent-color-dark', darkVariant)

      // Sync Element Plus primary colors so components (Button/Table/Tag...) follow theme color globally
      root.style.setProperty('--el-color-primary', this.primaryColor)
      root.style.setProperty('--el-color-primary-dark-2', this.adjustBrightness(this.primaryColor, -12))
      root.style.setProperty('--el-color-primary-light-3', this.adjustBrightness(this.primaryColor, 18))
      root.style.setProperty('--el-color-primary-light-5', this.adjustBrightness(this.primaryColor, 30))
      root.style.setProperty('--el-color-primary-light-7', this.adjustBrightness(this.primaryColor, 42))
      root.style.setProperty('--el-color-primary-light-8', this.adjustBrightness(this.primaryColor, 52))
      root.style.setProperty('--el-color-primary-light-9', this.adjustBrightness(this.primaryColor, 62))

      // Sync key Element Plus surface/text/border vars (Select/Dropdown/Form/Table...) to match our theme
      root.style.setProperty('--el-text-color-primary', 'var(--text-color)')
      root.style.setProperty('--el-text-color-regular', 'var(--text-color-muted)')
      root.style.setProperty('--el-text-color-secondary', 'var(--text-color-secondary)')
      root.style.setProperty('--el-border-color', 'var(--border-subtle)')
      root.style.setProperty('--el-border-color-light', 'var(--border-card)')
      root.style.setProperty('--el-bg-color', 'var(--bg-body)')
      root.style.setProperty('--el-bg-color-overlay', 'var(--bg-elevated)')
      root.style.setProperty('--el-fill-color-blank', 'var(--bg-card)')
      root.style.setProperty('--el-fill-color-light', 'var(--bg-elevated-soft)')
    },
    hexToRgb(hex: string) {
      const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
      return result ? {
        r: parseInt(result[1]!, 16),
        g: parseInt(result[2]!, 16),
        b: parseInt(result[3]!, 16)
      } : null
    },
    adjustBrightness(hex: string, percent: number) {
      let num = parseInt(hex.replace('#', ''), 16),
          amt = Math.round(2.55 * percent),
          R = (num >> 16) + amt,
          B = ((num >> 8) & 0x00FF) + amt,
          G = (num & 0x0000FF) + amt
      return '#' + (0x1000000 + (R<255?R<1?0:R:255)*0x10000 + (B<255?B<1?0:B:255)*0x100 + (G<255?G<1?0:G:255)).toString(16).slice(1)
    },
  },
})
