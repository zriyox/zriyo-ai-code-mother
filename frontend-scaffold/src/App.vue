<!--
根组件
-->
<template>
  <el-config-provider :locale="locale" :namespace="namespace">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import en from 'element-plus/dist/locale/en.mjs'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const appStore = useAppStore()

// 语言设置
const locale = computed(() => (appStore.lang === 'zh' ? zhCn : en))

// 命名空间
const namespace = computed(() => (appStore.dark ? 'ep-dark' : ''))

// 监听路由变化
watch(
  () => route.path,
  () => {
    // 更新页面标题
    const title = route.meta?.title
      ? `${route.meta.title} - ${import.meta.env.VITE_APP_TITLE || 'AI Generated App'}`
      : import.meta.env.VITE_APP_TITLE || 'AI Generated App'
    document.title = title
  },
  { immediate: true }
)

// 初始化
onMounted(() => {
  // 初始化主题
  appStore.initTheme()

  // 添加全局错误处理
  window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled promise rejection:', event.reason)
  })

  // 添加全局错误监听
  window.addEventListener('error', (event) => {
    console.error('Global error:', event.error)
  })
})
</script>

<style>
/* 全局样式已由 main.ts 导入 */
</style>
