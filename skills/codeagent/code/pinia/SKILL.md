---
name: pinia
description: "Pinia 状态管理 Skill 文档"
metadata:
  short-description: "Pinia 状态管理 Skill 文档"
---

# Pinia 状态管理 Skill 文档

> 官方文档: https://pinia.vuejs.org/zh/getting-started.html

## 概述

Pinia 是 Vue 的官方状态管理库，是 Vuex 的继任者。它提供了更简单的 API、更好的 TypeScript 支持和更小的打包体积。

## 安装

```bash
```

## 基础用法

### 创建 Pinia 实例

```typescript
// main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.mount('#app')
```

### 定义 Store

```typescript
// stores/counter.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCounterStore = defineStore('counter', () => {
  // state
  const count = ref(0)

  // getters
  const doubleCount = computed(() => count.value * 2)

  // actions
  function increment() {
    count.value++
  }

  function decrement() {
    count.value--
  }

  function reset() {
    count.value = 0
  }

  return {
    count,
    doubleCount,
    increment,
    decrement,
    reset,
  }
})
```

### 在组件中使用

```vue
<script setup>
import { useCounterStore } from '@/stores/counter'

const counterStore = useCounterStore()
</script>

<template>
  <div>
    <p>Count: {{ counterStore.count }}</p>
    <p>Double: {{ counterStore.doubleCount }}</p>
    <button @click="counterStore.increment">+1</button>
    <button @click="counterStore.decrement">-1</button>
    <button @click="counterStore.reset">Reset</button>
  </div>
</template>
```

## Store 选项

### State (状态)

```typescript
export const useUserStore = defineStore('user', () => {
  // 响应式状态
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  return {
    token,
    userInfo,
  }
})
```

### Getters (计算属性)

```typescript
export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)

  // Getter
  const isLogin = computed(() => !!userInfo.value)
  const username = computed(() => userInfo.value?.username || '')
  const displayName = computed(() => {
    return userInfo.value?.nickname || userInfo.value?.username || '游客'
  })

  return {
    userInfo,
    isLogin,
    username,
    displayName,
  }
})
```

### Actions (方法)

```typescript
export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  // Action
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  async function login(credentials: LoginRequest) {
    const data = await loginApi(credentials)
    setToken(data.token)
    setUserInfo(data.user)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    login,
    logout,
  }
})
```

## Store 持久化

### 使用 localStorage

```typescript
export const useAppStore = defineStore('app', () => {
  const theme = ref<ThemeMode>('light')
  const sidebarCollapsed = ref(false)

  // 初始化
  function init() {
    const savedTheme = localStorage.getItem('theme') as ThemeMode
    if (savedTheme) {
      theme.value = savedTheme
    }
  }

  // 切换主题
  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    localStorage.setItem('theme', theme.value)
  }

  return {
    theme,
    sidebarCollapsed,
    init,
    toggleTheme,
  }
})
```

### 使用 pinia-plugin-persistedstate

```bash
```

```typescript
// main.ts
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// stores/user.ts
export const useUserStore = defineStore('user', () => {
  // ...
  return {
    token,
    userInfo,
  }
}, {
  persist: {
    key: 'user',
    storage: localStorage,
    paths: ['token', 'userInfo'],
  },
})
```

## Store 之间调用

```typescript
// stores/app.ts
export const useAppStore = defineStore('app', () => {
  const loading = ref(false)

  function setLoading(value: boolean) {
    loading.value = value
  }

  return {
    loading,
    setLoading,
  }
})

// stores/user.ts
export const useUserStore = defineStore('user', () => {
  const appStore = useAppStore() // 调用其他 store

  async function login(credentials: LoginRequest) {
    appStore.setLoading(true)
    try {
      const data = await loginApi(credentials)
      // ...
    } finally {
      appStore.setLoading(false)
    }
  }

  return {
    login,
  }
})
```

## TypeScript 支持

### 定义 State 类型

```typescript
interface UserInfo {
  id: number | string
  username: string
  nickname?: string
  avatar?: string
  email?: string
  roles?: string[]
  permissions?: string[]
}

interface UserState {
  token: string
  userInfo: UserInfo | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: '',
    userInfo: null,
  }),
  getters: {
    isLogin: (state) => !!state.token,
    username: (state) => state.userInfo?.username || '',
  },
  actions: {
    setToken(token: string) {
      this.token = token
    },
  },
})
```

## 最佳实践

1. **命名约定**：Store 函数以 `use` 开头，如 `useUserStore`
2. **组合式 API**：使用 setup 语法获得更好的 TypeScript 支持
3. **状态分层**：合理划分 Store，按功能模块组织
4. **持久化**：使用插件自动持久化关键状态
5. **类型定义**：使用 TypeScript 接口定义状态类型

## Store 结构建议

```
stores/
├── index.ts           # 统一导出
├── app.ts            # 应用状态（主题、侧边栏、语言）
├── user.ts           # 用户状态（登录、用户信息、权限）
├── permission.ts     # 权限状态
└── [feature].ts      # 功能状态（按需添加）
```
