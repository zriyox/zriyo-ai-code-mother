---
name: nprogress
description: "NProgress 页面加载进度 Skill 文档"
metadata:
  short-description: "NProgress 页面加载进度 Skill 文档"
---

# NProgress 页面加载进度 Skill 文档

## 概述

NProgress 是一个适用于单页应用的纳米级进度条，灵感来自 YouTube、Medium 等网站。

## 安装

```bash
```

## 基础用法

### 简单使用

```typescript
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 开始进度条
NProgress.start()

// 结束进度条
NProgress.done()
```

### 手动控制进度

```typescript
import NProgress from 'nprogress'

// 设置进度百分比（0-1）
NProgress.set(0.5)

// 增加进度
NProgress.inc()
NProgress.inc(0.2)  // 增加 20%
```

## Vue Router 集成

### 路由守卫集成

```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [/* ... */],
})

// 路由前置守卫
router.beforeEach((to, from, next) => {
  NProgress.start()
  next()
})

// 路由后置守卫
router.afterEach(() => {
  NProgress.done()
})

export default router
```

### 带标题的路由进度

```typescript
router.beforeEach((to, from, next) => {
  NProgress.start()

  // 根据路由元信息设置标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 应用名称`
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})
```

## 配置选项

### 基础配置

```typescript
import NProgress from 'nprogress'

// 配置
NProgress.configure({
  minimum: 0.08,        // 最小百分比
  easing: 'ease',       // 缓动函数
  speed: 200,           // 动画速度（ms）
  trickle: true,        // 自动增加进度
  trickleSpeed: 200,    // 自动增加速度
  showSpinner: true,    // 显示加载图标
  barSelector: '[role="bar"]',  // 进度条选择器
  spinnerSelector: '[role="spinner"]',  // 图标选择器
  parent: 'body',       // 父容器
  template: `           // 自定义模板
    <div class="bar" role="bar">
      <div class="peg"></div>
    </div>
    <div class="spinner" role="spinner">
      <div class="spinner-icon"></div>
    </div>
  `,
})
```

### 关闭加载图标

```typescript
NProgress.configure({ showSpinner: false })
```

### 调整速度

```typescript
NProgress.configure({
  speed: 500,      // 进度条动画速度
  trickleSpeed: 500,  // 自动增加速度
})
```

## 自定义样式

### 覆盖默认样式

```scss
// styles/nprogress.scss
@import 'nprogress/nprogress.css';

// 修改进度条颜色
#nprogress .bar {
  background: #3b82f6 !important; // 蓝色
  height: 3px !important;
}

// 修改加载图标颜色
#nprogress .spinner {
  display: none; // 隐藏图标
}

// 修改加载图标
#nprogress .spinner-icon {
  border-top-color: #3b82f6 !important;
  border-left-color: #3b82f6 !important;
}

// 添加阴影效果
#nprogress .peg {
  box-shadow: 0 0 10px #3b82f6, 0 0 5px #3b82f6 !important;
}

// 修改 z-index
#nprogress {
  pointer-events: none;
  z-index: 9999;
}
```

### 多种主题样式

```scss
// GitHub 风格
.nprogress-github .bar {
  background: #ffffff !important;
}

// Element Plus 风格
.nprogress-element .bar {
  background: #409eff !important;
}

// Ant Design 风格
.nprogress-antd .bar {
  background: #1890ff !important;
}

// 渐变风格
.nprogress-gradient .bar {
  background: linear-gradient(
    to right,
    #3b82f6,
    #8b5cf6,
    #ec4899
  ) !important;
}
```

## 请求拦截器集成

### Axios 拦截器

```typescript
// utils/request.ts
import axios from 'axios'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 配置
NProgress.configure({ showSpinner: false })

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    NProgress.start()
    return config
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    NProgress.done()
    return response.data
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

export default request
```

### 带并发控制的拦截器

```typescript
let requestCount = 0

function showProgress() {
  if (requestCount === 0) {
    NProgress.start()
  }
  requestCount++
}

function hideProgress() {
  requestCount--
  if (requestCount === 0) {
    NProgress.done()
  }
}

request.interceptors.request.use((config) => {
  showProgress()
  return config
})

request.interceptors.response.use(
  (response) => {
    hideProgress()
    return response.data
  },
  (error) => {
    hideProgress()
    return Promise.reject(error)
  }
)
```

## Vue 3 组合式封装

### useNProgress Composable

```typescript
// composables/useNProgress.ts
import { ref, watch } from 'vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

interface ProgressOptions {
  minimum?: number
  speed?: number
  showSpinner?: boolean
}

export function useNProgress(options?: ProgressOptions) {
  const isLoading = ref(false)
  const progress = ref(0)

  // 配置
  if (options) {
    NProgress.configure(options)
  }

  // 开始
  const start = () => {
    isLoading.value = true
    NProgress.start()
  }

  // 完成
  const done = () => {
    isLoading.value = false
    progress.value = 0
    NProgress.done()
  }

  // 设置进度
  const set = (n: number) => {
    progress.value = n
    NProgress.set(n)
  }

  // 增加进度
  const inc = (amount?: number) => {
    const current = progress.value
    progress.value = Math.min(1, current + (amount || 0.1))
    NProgress.inc(amount)
  }

  return {
    isLoading,
    progress,
    start,
    done,
    set,
    inc,
  }
}
```

### 使用示例

```vue
<script setup lang="ts">
import { useNProgress } from '@/composables/useNProgress'

const { isLoading, start, done } = useNProgress({
  minimum: 0.1,
  speed: 300,
  showSpinner: false,
})

async function fetchData() {
  start()
  try {
    const data = await api.getData()
    return data
  } finally {
    done()
  }
}
</script>

<template>
  <div v-if="isLoading" class="loading-overlay">
    <p>加载中...</p>
  </div>
</template>
```

## 自定义位置

### 顶部固定（默认）

```scss
#nprogress .bar {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 9999;
}
```

### 底部固定

```scss
#nprogress .bar {
  top: auto !important;
  bottom: 0 !important;
}
```

### 容器内

```vue
<template>
  <div ref="containerRef" class="content-container">
    <!-- 内容 -->
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import NProgress from 'nprogress'

const containerRef = ref<HTMLElement>()

onMounted(() => {
  NProgress.configure({
    parent: containerRef.value,
  })
})
</script>
```

## 常用场景

### 页面加载进度

```typescript
// main.ts
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 显示加载进度
window.addEventListener('load', () => {
  NProgress.done()
})

// 开始加载
NProgress.start()
```

### 文件上传进度

```vue
<script setup lang="ts">
import { ref } from 'vue'
import NProgress from 'nprogress'

const uploading = ref(false)

async function uploadFile(file: File) {
  uploading.value = true
  NProgress.start()

  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await fetch('/api/upload', {
      method: 'POST',
      body: formData,
    })

    if (response.ok) {
      NProgress.done()
    }
  } catch (error) {
    NProgress.done()
  } finally {
    uploading.value = false
  }
}
</script>
```

### 图片懒加载进度

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import NProgress from 'nprogress'

const images = ref<string[]>([
  '/image1.jpg',
  '/image2.jpg',
  '/image3.jpg',
])

onMounted(async () => {
  NProgress.start()

  const promises = images.value.map((src) => {
    return new Promise((resolve) => {
      const img = new Image()
      img.onload = resolve
      img.src = src
    })
  })

  await Promise.all(promises)
  NProgress.done()
})
</script>
```

## API 参考

### 方法

| 方法 | 参数 | 描述 |
|------|------|------|
| `start()` | - | 开始进度条 |
| `done(force)` | force?: boolean | 完成进度条 |
| `set(amount)` | amount: number | 设置进度（0-1） |
| `inc(amount)` | amount?: number | 增加少量进度 |
| `configure(options)` | options: object | 配置选项 |

### 配置选项

| 选项 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| minimum | number | 0.08 | 最小百分比 |
| easing | string | 'ease' | 缓动函数 |
| speed | number | 200 | 动画速度（ms） |
| trickle | boolean | true | 是否自动增加 |
| trickleSpeed | number | 200 | 自动增加速度（ms） |
| showSpinner | boolean | true | 显示加载图标 |
| parent | string | 'body' | 父容器选择器 |

## 最佳实践

1. **路由集成**：在路由守卫中自动处理进度
2. **请求拦截**：配合 Axios 拦截器处理请求进度
3. **关闭图标**：生产环境建议关闭 spinner
4. **样式定制**：根据网站风格定制进度条颜色
5. **并发处理**：多个请求时使用计数器控制
