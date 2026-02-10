import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getSettingsByGroup } from '@/api/system'

export const useSystemStore = defineStore('system', () => {
  const siteName = ref('Team Up')
  const siteDescription = ref('团队协作平台')
  const loaded = ref(false)

  /**
   * 加载系统设置
   */
  const loadSettings = async () => {
    try {
      const basicSettings = await getSettingsByGroup('basic')
      
      if (basicSettings.siteName) {
        siteName.value = basicSettings.siteName
      }
      
      if (basicSettings.siteDescription) {
        siteDescription.value = basicSettings.siteDescription
      }
      
      // 更新页面标题
      document.title = siteName.value
      
      loaded.value = true
      console.log('✅ 系统设置加载成功:', { siteName: siteName.value, siteDescription: siteDescription.value })
    } catch (error) {
      console.error('❌ 加载系统设置失败:', error)
      // 使用默认值
      loaded.value = true
    }
  }

  return {
    siteName,
    siteDescription,
    loaded,
    loadSettings
  }
})
