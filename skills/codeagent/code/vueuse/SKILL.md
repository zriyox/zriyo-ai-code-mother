---
name: vueuse
description: "@vueuse/core Vue 组合式工具库 Skill 文档"
metadata:
  short-description: "@vueuse/core Vue 组合式工具库 Skill 文档"
---

# @vueuse/core Vue 组合式工具库 Skill 文档

## 概述

VueUse 是基于 Vue 3 Composition API 的实用工具函数集合，提供了超过 200 个开箱即用的组合式 API。

## 安装

```bash
```

## 自动导入

```typescript
// vite.config.ts
import AutoImport from 'unplugin-auto-import/vite'

export default defineConfig({
  plugins: [
    AutoImport({
      imports: [
        'vue',
        'vue-router',
        'pinia',
        '@vueuse/core',
      ],
      dts: 'types/auto-imports.d.ts',
    }),
  ],
})
```

## 常用 API

### 浏览器

```typescript
import {
  useWindowSize,
  useElementSize,
  useScroll,
  useBreakpoints,
  useMediaQuery,
} from '@vueuse/core'

// 窗口大小
const { width, height } = useWindowSize()

// 元素尺寸
const { width, height } = useElementSize(targetRef)

// 滚动位置
const { x, y, isScrolling, arrivedState, directions } = useScroll()

// 断点
const breakpoints = useBreakpoints({
  tablet: 768,
  laptop: 1024,
  desktop: 1280,
})
const isTablet = breakpoints.greater('tablet')

// 媒体查询
const isDark = useMediaQuery('(prefers-color-scheme: dark)')
```

### 传感器

```typescript
import {
  useMouse,
  usePointer,
  useElementHover,
  useIntersectionObserver,
  useResizeObserver,
} from '@vueuse/core'

// 鼠标位置
const { x, y, sourceType } = useMouse()

// 指针状态
const { pointerType, pressure } = usePointer()

// 元素悬停
const isHovered = useElementHover(targetRef)

// 可见性检测
const { stop, start } = useIntersectionObserver(targetCallback)

// 元素大小监听
const { width, height } = useResizeObserver(targetRef)
```

### 网络状态

```typescript
import { useOnline, useNetwork } from '@vueuse/core'

// 在线状态
const isOnline = useOnline()

// 网络信息
const {
  isOnline: onlineStatus,
  offlineAt,
  downlink,
  effectiveType,
  rtt,
  saveData,
} = useNetwork()
```

### 状态管理

```typescript
import {
  useLocalStorage,
  useSessionStorage,
  useStorage,
} from '@vueuse/core'

// LocalStorage
const { value: token } = useLocalStorage('token', '')

// SessionStorage
const { value: sessionId } = useSessionStorage('sessionId', '')

// 存储任意类型（自动序列化）
const { value: user, isReadonly, write: set } = useStorage('user', { name: 'User' })

// 修改值
set({ name: 'Admin' })
```

### 时间工具

```typescript
import {
  useNow,
  useDateFormat,
  useTimeAgo,
} from '@vueuse/core'

// 当前时间
const now = useNow()

// 日期格式化
const formatted = useDateFormat(new Date(), 'YYYY-MM-DD HH:mm:ss')

// 相对时间
const timeAgo = useTimeAgo(new Date('2024-01-01'))
```

### DOM 操作

```typescript
import {
  useElementSize,
  useElementBoundingRect,
  useScroll,
  useToggle,
  useCssVar,
} from '@vueuse/core'

// 切换布尔值
const [value, toggle] = useToggle()

// CSS 变量
const { setVar } = useCssVar('--color-primary', '#3b82f6')

// 滚动到指定位置
const { x, y } = useScroll()
const scrollTo = (x: number, y: number) => {
  x.value = x
  y.value = y
}
```

### 剪贴板

```typescript
import { useClipboard } from '@vueuse/core'

const { text, isSupported, copy, copied } = useClipboard()

// 复制到剪贴板
async function copyText(text: string) {
  const { copy, copied } = useClipboard()

  await copy(text)
  if (copied.value) {
    console.log('复制成功')
  }
}
```

### 请求状态

```typescript
import {
  useAsyncState,
  useFetch,
} from '@vue/app'

// 异步状态
const { state, isLoading, isReady, error } = useAsyncState(
  () => fetch('/api/data').then(r => r.json()),
  { immediate: true }
)

// Fetch 请求
const { data, isFetching, error, execute } = useFetch('/api/data').get()
```

### 生命周期

```typescript
import {
  onMounted,
  onUnmounted,
  onBeforeUnmount,
  onceMounted,
} from '@vueuse/core'

onMounted(() => {
  console.log('组件已挂载')
})

onUnmounted(() => {
  console.log('组件将卸载')
})
```

## 常用组合式函数

### 防抖函数

```typescript
import { useDebounceFn } from '@vueuse/core'

const debouncedSearch = useDebounceFn(
  (value: string) => {
    console.log('搜索:', value)
  },
  300
)
```

### 节流函数

```typescript
import { useThrottleFn } from '@vueuse/core'

const throttledScroll = useThrottleFn(
  () => {
    console.log('滚动中...')
  },
  100
)
```

### RAF 节流

```typescript
import { useRafFn } from '@vueuse/core'

const { raf, cancel, isActive } = useRafFn(() => {
  console.log('每帧更新')
})

// 停止 RAF
cancel()
```

### 点击外部检测

```typescript
import { onClickOutside } from '@vueuse/core'

onClickOutside(targetRef, () => {
  console.log('点击了外部')
})
```

## 最佳实践

1. **自动导入**：配置自动导入简化使用
2. **按需引入**：从 '@vueuse/core' 按需引入函数
3. **响应式**：所有返回的值都是响应式的
4. **SSR 兼容**：使用支持 SSR 的函数（如 useNow）
