/**
 * 应用状态管理
 */

import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { ThemeMode, ThemeConfig } from '@/types'

const STORAGE_KEY = 'app-settings'

export const useAppStore = defineStore('app', () => {
  // 状态
  const sidebarCollapsed = ref(false)
  const device = ref<'desktop' | 'mobile'>('desktop')
  const lang = ref<'zh' | 'en'>('zh')
  const themeMode = ref<ThemeMode>('light')
  const primaryColor = ref('#3b82f6')

  // 计算属性
  const isMobile = computed(() => device.value === 'mobile')
  const dark = computed(() => themeMode.value === 'dark')

  // 主题配置
  const themeConfig = computed<ThemeConfig>(() => ({
    mode: themeMode.value,
    primaryColor: primaryColor.value,
    borderRadius: 4,
    fontSize: 14,
  }))

  // 初始化主题
  function initTheme() {
    // 从 localStorage 读取设置
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      try {
        const data = JSON.parse(saved)
        if (data.themeMode) themeMode.value = data.themeMode
        if (data.primaryColor) primaryColor.value = data.primaryColor
        if (data.lang) lang.value = data.lang
        if (data.sidebarCollapsed !== undefined) {
          sidebarCollapsed.value = data.sidebarCollapsed
        }
      } catch (e) {
        console.error('Failed to parse saved settings:', e)
      }
    }

    // 应用主题
    applyTheme()
  }

  // 应用主题
  function applyTheme() {
    const root = document.documentElement

    // 应用主题模式
    if (themeMode.value === 'dark') {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }

    // 应用主色调
    root.style.setProperty('--primary-color', primaryColor.value)
  }

  // 切换侧边栏
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    saveSettings()
  }

  // 切换主题模式
  function toggleTheme() {
    const modes: ThemeMode[] = ['light', 'dark', 'auto']
    const currentIndex = modes.indexOf(themeMode.value)
    themeMode.value = modes[(currentIndex + 1) % modes.length]
    applyTheme()
    saveSettings()
  }

  // 设置主题
  function setTheme(mode: ThemeMode) {
    themeMode.value = mode
    applyTheme()
    saveSettings()
  }

  // 设置主色调
  function setPrimaryColor(color: string) {
    primaryColor.value = color
    applyTheme()
    saveSettings()
  }

  // 切换语言
  function toggleLanguage() {
    lang.value = lang.value === 'zh' ? 'en' : 'zh'
    saveSettings()
  }

  // 设置设备类型
  function setDevice(value: 'desktop' | 'mobile') {
    device.value = value
    if (value === 'mobile') {
      sidebarCollapsed.value = true
    }
  }

  // 保存设置到 localStorage
  function saveSettings() {
    const data = {
      themeMode: themeMode.value,
      primaryColor: primaryColor.value,
      lang: lang.value,
      sidebarCollapsed: sidebarCollapsed.value,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  }

  // 监听主题变化
  watch(themeMode, applyTheme)
  watch(primaryColor, () => {
    document.documentElement.style.setProperty('--primary-color', primaryColor.value)
  })

  return {
    // 状态
    sidebarCollapsed,
    device,
    lang,
    themeMode,
    primaryColor,
    // 计算属性
    isMobile,
    dark,
    themeConfig,
    // 方法
    initTheme,
    applyTheme,
    toggleSidebar,
    toggleTheme,
    setTheme,
    setPrimaryColor,
    toggleLanguage,
    setDevice,
    saveSettings,
  }
})
