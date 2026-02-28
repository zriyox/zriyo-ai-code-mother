---
name: vue-router
description: "Vue Router 路由管理 Skill 文档"
metadata:
  short-description: "Vue Router 路由管理 Skill 文档"
---

# Vue Router 路由管理 Skill 文档

> 官方文档: https://router.vuejs.org/zh/guide/

## 概述

Vue Router 是 Vue.js 的官方路由管理器。它与 Vue.js 核心深度集成，让用 Vue.js 构建单页应用（SPA）变得轻而易举。

## 安装

```bash
```

## 基础配置

### 创建路由器实例

```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/About.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
```

### 注册路由器

```typescript
// main.ts
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(router)
app.mount('#app')
```

## 核心组件

### RouterView

```vue
<template>
  <!-- 渲染当前 URL 对应的组件 -->
  <RouterView />
</template>
```

### RouterLink

```vue
<template>
  <!-- 导航链接 -->
  <RouterLink to="/">Home</RouterLink>
  <RouterLink to="/about">About</RouterLink>

  <!-- 命名路由 -->
  <RouterLink :to="{ name: 'User', params: { id: 1 } }">
    User
  </RouterLink>

  <!-- 带查询参数 -->
  <RouterLink to="/search?q=vue">Search</RouterLink>

  <!-- 使用 a 标签的等价写法 -->
  <a href="/">Home</a>
</template>
```

## 路由配置

### 动态路由

```typescript
const routes: RouteRecordRaw[] = [
  // 参数路由
  {
    path: '/user/:id',
    name: 'User',
    component: () => import('@/views/User.vue'),
  },

  // 多个参数
  {
    path: '/post/:postId/comment/:commentId',
    name: 'Comment',
    component: () => import('@/views/Comment.vue'),
  },

  // 可选参数
  {
    path: '/category/:name?',
    name: 'Category',
    component: () => import('@/views/Category.vue'),
  },

  // 匹配所有
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
  },
]
```

### 嵌套路由

```typescript
const routes: RouteRecordRaw[] = [
  {
    path: '/user/:id',
    component: () => import('@/views/User.vue'),
    children: [
      {
        path: 'profile', // 完整路径 /user/:id/profile
        component: () => import('@/views/User/Profile.vue'),
      },
      {
        path: 'posts', // 完整路径 /user/:id/posts
        component: () => import('@/views/User/Posts.vue'),
      },
    ],
  },
]
```

### 命名路由

```typescript
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home', // 命名路由
    component: () => import('@/views/Home.vue'),
  },
]
```

## 编程式导航

### useRouter

```vue
<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

// 导航到指定路径
function navigateToPath() {
  router.push('/about')
}

// 命名路由导航
function navigateByName() {
  router.push({ name: 'User', params: { id: 1 } })
}

// 带查询参数
function navigateWithQuery() {
  router.push({ path: '/search', query: { q: 'vue' } })
}

// 替换当前路由
function replaceRoute() {
  router.replace('/home')
}

// 前进/后退
function goBack() {
  router.go(-1)
}
</script>
```

## 路由信息

### useRoute

```vue
<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

// 路径参数
console.log(route.params.id)

// 查询参数
console.log(route.query.q)

// 路由元信息
console.log(route.meta.title)

// 当前路径
console.log route.path
console.log route.fullPath
</script>
```

## 路由守卫

### 全局前置守卫

```typescript
router.beforeEach((to, from, next) => {
  // 可以通过返回 false 取消导航
  if (to.meta.requiresAuth && !isAuthenticated()) {
    next('/login')
  } else {
    next()
  }
})
```

### 全局后置钩子

```typescript
router.afterEach((to, from) => {
  // 修改页面标题
  document.title = to.meta.title || 'App'
})
```

### 路由独享守卫

```typescript
const routes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: () => import('@/views/Admin.vue'),
    beforeEnter: (to, from, next) => {
      if (isAdmin()) {
        next()
      } else {
        next(false)
      }
    },
  },
]
```

### 组件内守卫

```vue
<script setup>
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'

// 离开守卫
onBeforeRouteLeave((to, from, next) => {
  const answer = window.confirm('确定要离开吗？未保存的更改将会丢失！')
  if (answer) {
    next()
  } else {
    next(false)
  }
})

// 更新守卫
onBeforeRouteUpdate((to, from, next) => {
  // 可以访问组件实例
  next()
})
</script>
```

## 路由元信息

### 定义元信息

```typescript
interface RouteMeta {
  title?: string
  requiresAuth?: boolean
  roles?: string[]
  icon?: string
  hidden?: boolean
  keepAlive?: boolean
}

const routes: RouteRecordRaw[] = [
  {
    path: '/admin',
    meta: {
      title: '管理后台',
      requiresAuth: true,
      roles: ['admin'],
      icon: 'Setting',
    } as RouteMeta,
    component: () => import('@/views/Admin.vue'),
  },
]
```

### 使用元信息

```vue
<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

// 在模板中
// {{ route.meta.title }}
</script>
```

## 懒加载

### 路由级别代码分割

```typescript
const routes: RouteRecordRaw[] = [
  {
    path: '/dashboard',
    component: () => import('@/views/Dashboard.vue'),
    // 分组打包（webpack）
    component: () => import(/* webpackChunkName: "dashboard" */ '@/views/Dashboard.vue'),
  },
]
```

### 懒加载状态组件

```vue
<template>
  <RouterView v-slot="{ Component }">
    <template v-if="Component">
      <Suspense>
        <component :is="Component" />
        <template #fallback>
          <LoadingSpinner />
        </template>
      </Suspense>
    </template>
    </RouterView>
</template>
```

## 过渡动效

### 路由过渡

```vue
<template>
  <RouterView v-slot="{ Component, route }">
    <transition :name="route.meta.transition || 'fade'" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </RouterView>
</template>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
```

## 滚动行为

```typescript
const router = createRouter({
  scrollBehavior(to, from, savedPosition) {
    // 返回 savedPosition 当通过后退/前进按钮触发时
    if (savedPosition) {
      return savedPosition
    }
    // 返回 top 针对所有路由导航
    return { top: 0 }
    // 也可以返回选择器来滚动到指定元素
    // return { el: '#main' }
  },
})
```

## 组合式 API

### 在 setup 中使用

```vue
<script setup>
import { useRouter, useRoute, onBeforeRouteUpdate, useLink } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 导航到不同位置
function navigate() {
  router.push({ name: 'Home' })
}

// 获取当前路由信息
console.log(route.path)

// RouterLink 的等价写法
const homeLink = useLink({ to: '/' })
const isActive = homeLink.isActive.value
</script>

<template>
  <a :href="homeLink.href" :class="{ active: isActive }">
    Home
  </a>
</template>
```

## 最佳实践

1. **懒加载路由**：使用动态导入实现代码分割
2. **命名路由**：使用命名路由而非硬编码路径
3. **路由守卫**：合理使用全局、路由独享和组件内守卫
4. **元信息**：利用路由元信息存储页面相关信息
5. **错误处理**：设置 404 和错误处理路由
