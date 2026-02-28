<!--
默认布局组件
包含侧边栏、顶部栏、主内容区
-->
<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="sidebarWidth" class="layout-aside">
      <div class="logo-container">
        <h1 v-if="!appStore.sidebarCollapsed" class="logo-title">
          {{ appTitle }}
        </h1>
        <span v-else class="logo-icon">🤖</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :unique-opened="true"
        router
        class="layout-menu"
      >
        <template v-for="item in menuRoutes" :key="item.path">
          <el-menu-item
            v-if="!item.meta?.hidden"
            :index="item.path"
          >
            <el-icon v-if="item.meta?.icon">
              <component :is="item.meta.icon" />
            </el-icon>
            <template #title>{{ item.meta?.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container class="layout-main">
      <!-- 顶部栏 -->
      <el-header height="60px" class="layout-header">
        <div class="header-left">
          <el-icon
            class="collapse-trigger"
            :size="20"
            @click="appStore.toggleSidebar"
          >
            <Expand v-if="appStore.sidebarCollapsed" />
            <Fold v-else />
          </el-icon>

          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="item in breadcrumbs"
              :key="item.path"
              :to="item.path"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 搜索 -->
          <el-input
            v-model="searchText"
            placeholder="搜索..."
            prefix-icon="Search"
            class="header-search"
            clearable
          />

          <!-- 主题切换 -->
          <el-tooltip :content="appStore.dark ? '切换亮色' : '切换暗色'">
            <el-button :icon="appStore.dark ? Sunny : Moon" circle @click="appStore.toggleTheme" />
          </el-tooltip>

          <!-- 语言切换 -->
          <el-dropdown>
            <el-button circle>
              <span>文</span>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>中文</el-dropdown-item>
                <el-dropdown-item>English</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 用户菜单 -->
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.avatar">
                {{ userStore.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.nickname || userStore.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToSettings">
                  <el-icon><Setting /></el-icon>
                  设置
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="layout-content">
        <router-view v-slot="{ Component, route }">
          <transition :name="transitionName" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component" :key="route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Expand,
  Fold,
  Sunny,
  Moon,
  Setting,
  SwitchButton,
} from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

// 菜单项类型
interface MenuItem {
  path: string
  meta: {
    title: string
    icon: string
    hidden?: boolean
  }
}

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

// 应用标题
const appTitle = import.meta.env.VITE_APP_TITLE || 'AI Generated App'

// 侧边栏宽度
const sidebarWidth = computed(() =>
  appStore.sidebarCollapsed ? '64px' : '240px'
)

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 菜单路由
const menuRoutes = computed<MenuItem[]>(() => {
  // TODO: 从动态菜单获取
  return [
    { path: '/dashboard', meta: { title: '工作台', icon: 'Dashboard' } },
    { path: '/projects', meta: { title: '项目管理', icon: 'FolderOpened' } },
    { path: '/settings', meta: { title: '设置', icon: 'Setting' } },
  ]
})

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter((r) => r.meta?.title)
  return matched.map((r) => ({
    path: r.path,
    title: r.meta?.title as string,
  }))
})

// 缓存的视图
const cachedViews = computed(() => {
  // TODO: 根据配置决定缓存哪些页面
  return ['Dashboard']
})

// 过渡动画
const transitionName = computed(() => {
  return appStore.sidebarCollapsed ? 'fade' : 'slide-fade'
})

// 搜索文本
const searchText = ref('')

// 方法
function goToSettings() {
  router.push('/settings')
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await userStore.logout()
    router.push('/login')
  })
}

// 监听设备变化
const checkDevice = () => {
  const width = document.body.clientWidth
  appStore.setDevice(width < 768 ? 'mobile' : 'desktop')
}

watch(
  () => route.path,
  () => {
    checkDevice()
  },
  { immediate: true }
)

// 监听窗口大小变化
window.addEventListener('resize', checkDevice)
</script>

<style scoped>
.layout-container {
  width: 100%;
  height: 100vh;
}

.layout-aside {
  background-color: #001529;
  transition: width 0.3s;
  overflow: hidden;
}

.dark .layout-aside {
  background-color: #000;
}

.logo-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 60px;
  color: #fff;
}

.logo-title {
  font-size: 18px;
  font-weight: 600;
}

.logo-icon {
  font-size: 24px;
}

.layout-menu {
  border-right: none;
  background-color: transparent;
}

.layout-menu:not(.el-menu--collapse) {
  width: 240px;
}

.layout-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 20px;
}

.dark .layout-header {
  background-color: #1f2937;
  border-bottom-color: #374151;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.collapse-trigger {
  cursor: pointer;
  color: #6b7280;
  transition: color 0.3s;
}

.collapse-trigger:hover {
  color: #374151;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-search {
  width: 200px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.username {
  font-size: 14px;
  color: #374151;
}

.dark .username {
  color: #e5e7eb;
}

.layout-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f3f4f6;
}

.dark .layout-content {
  background-color: #111827;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter-from {
  transform: translateX(10px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(-10px);
  opacity: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .header-search {
    display: none;
  }

  .layout-content {
    padding: 10px;
  }
}
</style>
