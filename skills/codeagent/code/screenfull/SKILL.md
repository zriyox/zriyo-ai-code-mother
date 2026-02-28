---
name: screenfull
description: "Screenfull 全屏 API Skill 文档"
metadata:
  short-description: "Screenfull 全屏 API Skill 文档"
---

# Screenfull 全屏 API Skill 文档

## 概述

Screenfull 是一个跨浏览器的全屏 API 封装库，简化了全屏功能的实现。

## 安装

```bash
```

## 基础用法

### 简单全屏切换

```vue
<script setup lang="ts">
import screenfull from 'screenfull'
import { ref, onMounted } from 'vue'

const isFullscreen = ref(false)

onMounted(() => {
  // 检查是否支持全屏
  if (screenfull.isEnabled) {
    isFullscreen.value = screenfull.isFullscreen

    // 监听全屏变化
    screenfull.on('change', () => {
      isFullscreen.value = screenfull.isFullscreen
    })
  }
})

function toggleFullscreen() {
  if (screenfull.isEnabled) {
    screenfull.toggle()
  }
}
</script>

<template>
  <button @click="toggleFullscreen">
    {{ isFullscreen ? '退出全屏' : '全屏' }}
  </button>
</template>
```

### 元素全屏

```vue
<script setup lang="ts">
import screenfull from 'screenfull'
import { ref } from 'vue'

const containerRef = ref<HTMLElement>()

function enterFullscreen() {
  if (screenfull.isEnabled && containerRef.value) {
    screenfull.request(containerRef.value)
  }
}

function exitFullscreen() {
  if (screenfull.isEnabled) {
    screenfull.exit()
  }
}
</script>

<template>
  <div>
    <button @click="enterFullscreen">进入全屏</button>
    <button @click="exitFullscreen">退出全屏</button>

    <div ref="containerRef" class="content">
      <!-- 这个区域可以全屏 -->
      <h1>这是可以全屏的内容</h1>
    </div>
  </div>
</template>
```

## API 参考

### 方法

| 方法 | 参数 | 返回值 | 描述 |
|------|------|--------|------|
| `request(element?)` | Element? | Promise | 请求全屏 |
| `exit()` | - | Promise | 退出全屏 |
| `toggle(element?)` | Element? | Promise | 切换全屏 |
| `isFullscreen` | - | boolean | 是否全屏 |
| `isEnabled` | - | boolean | 是否支持全屏 |

### 事件

| 事件 | 描述 |
|------|------|
| `change` | 全屏状态变化 |
| `error` | 全屏错误 |

```typescript
// 监听事件
screenfull.on('change', () => {
  console.log('全屏状态:', screenfull.isFullscreen)
})

screenfull.on('error', (event) => {
  console.error('全屏错误:', event)
})

// 移除监听
screenfull.off('change', handler)
```

## Vue 3 组合式封装

### useScreenfull Composable

```typescript
// composables/useScreenfull.ts
import { ref, onMounted, onUnmounted } from 'vue'
import screenfull from 'screenfull'

export function useScreenfull() {
  const isFullscreen = ref(false)
  const isEnabled = screenfull.isEnabled

  const toggle = () => {
    if (!isEnabled) return
    screenfull.toggle()
  }

  const request = (element?: HTMLElement) => {
    if (!isEnabled) return
    screenfull.request(element)
  }

  const exit = () => {
    if (!isEnabled) return
    screenfull.exit()
  }

  const onChange = () => {
    isFullscreen.value = screenfull.isFullscreen
  }

  onMounted(() => {
    if (isEnabled) {
      screenfull.on('change', onChange)
      isFullscreen.value = screenfull.isFullscreen
    }
  })

  onUnmounted(() => {
    if (isEnabled) {
      screenfull.off('change', onChange)
    }
  })

  return {
    isFullscreen,
    isEnabled,
    toggle,
    request,
    exit,
  }
}
```

### 使用示例

```vue
<script setup lang="ts">
import { useScreenfull } from '@/composables/useScreenfull'

const { isFullscreen, isEnabled, toggle } = useScreenfull()
</script>

<template>
  <el-button
    v-if="isEnabled"
    :icon="isFullscreen ? 'mdi:fullscreen-exit' : 'mdi:fullscreen'"
    @click="toggle"
  >
    {{ isFullscreen ? '退出全屏' : '全屏' }}
  </el-button>
</template>
```

## 常用场景

### 页面全屏

```vue
<script setup lang="ts">
import { ref } from 'vue'
import screenfull from 'screenfull'

const isFullscreen = ref(false)

function toggle() {
  if (screenfull.isEnabled) {
    screenfull.toggle()
    isFullscreen.value = !isFullscreen.value
  }
}
</script>

<template>
  <div class="page">
    <div class="header">
      <el-button @click="toggle" circle>
        <Icon :icon="isFullscreen ? 'mdi:fullscreen-exit' : 'mdi:fullscreen'" />
      </el-button>
    </div>
    <div class="content">
      <!-- 页面内容 -->
    </div>
  </div>
</template>
```

### 视频播放器全屏

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import screenfull from 'screenfull'

const videoRef = ref<HTMLVideoElement>()
const isFullscreen = ref(false)

function toggleVideoFullscreen() {
  if (!screenfull.isEnabled) return

  if (screenfull.isFullscreen) {
    screenfull.exit()
  } else if (videoRef.value) {
    screenfull.request(videoRef.value)
  }
}

screenfull.on('change', () => {
  isFullscreen.value = screenfull.isFullscreen
})
</script>

<template>
  <div class="video-player">
    <video ref="videoRef" src="video.mp4"></video>

    <div class="controls">
      <el-button @click="toggleVideoFullscreen">
        {{ isFullscreen ? '退出全屏' : '全屏' }}
      </el-button>
    </div>
  </div>
</template>
```

### 图片预览全屏

```vue
<script setup lang="ts">
import { ref } from 'vue'
import screenfull from 'screenfull'

interface Props {
  src: string
  alt?: string
}

const props = defineProps<Props>()

const imageRef = ref<HTMLImageElement>()

function enterFullscreen() {
  if (screenfull.isEnabled && imageRef.value) {
    screenfull.request(imageRef.value)
  }
}
</script>

<template>
  <div class="image-preview">
    <img
      ref="imageRef"
      :src="src"
      :alt="alt"
      @click="enterFullscreen"
    />
  </div>
</template>

<style scoped>
.image-preview img {
  max-width: 100%;
  cursor: pointer;
}
</style>
```

### 数据大屏全屏

```vue
<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import screenfull from 'screenfull'

const isFullscreen = ref(false)

function toggle() {
  if (screenfull.isEnabled) {
    screenfull.toggle()
  }
}

function handleResize() {
  // 全屏变化时重新调整图表尺寸
  window.dispatchEvent(new Event('resize'))
}

onMounted(() => {
  if (screenfull.isEnabled) {
    screenfull.on('change', () => {
      isFullscreen.value = screenfull.isFullscreen
      handleResize()
    })
  }
})

onUnmounted(() => {
  if (screenfull.isEnabled) {
    screenfull.off('change', handleResize)
  }
})
</script>

<template>
  <div :class="['dashboard', { fullscreen: isFullscreen }]">
    <div class="dashboard-header">
      <h1>数据大屏</h1>
      <el-button @click="toggle">
        {{ isFullscreen ? '退出全屏' : '全屏显示' }}
      </el-button>
    </div>

    <div class="dashboard-content">
      <!-- 图表内容 -->
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 20px;
  background: #f5f5f5;
}

.dashboard.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  padding: 40px;
  background: #000;
}

.dashboard.fullscreen :deep(.chart) {
  height: calc(100vh - 200px) !important;
}
</style>
```

### 快捷键全屏

```vue
<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import screenfull from 'screenfull'

const isFullscreen = ref(false)

function handleKeydown(event: KeyboardEvent) {
  // F11 或 ESC
  if (event.key === 'F11' || event.key === 'Escape') {
    event.preventDefault()
    if (screenfull.isEnabled) {
      screenfull.toggle()
    }
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)

  if (screenfull.isEnabled) {
    screenfull.on('change', () => {
      isFullscreen.value = screenfull.isFullscreen
    })
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div>
    <p>按 F11 切换全屏</p>
    <p>当前状态: {{ isFullscreen ? '全屏' : '窗口' }}</p>
  </div>
</template>
```

## TypeScript 类型

```typescript
// types/screenfull.d.ts
declare module 'screenfull' {
  interface Screenfull {
    readonly isFullscreen: boolean
    readonly isEnabled: boolean
    request(element?: HTMLElement): Promise<void>
    exit(): Promise<void>
    toggle(element?: HTMLElement): Promise<void>
    on(event: string, handler: () => void): void
    off(event: string, handler: () => void): void
  }

  const screenfull: Screenfull
  export default screenfull
}
```

## 浏览器兼容性

| 浏览器 | 支持情况 |
|--------|----------|
| Chrome | ✅ |
| Firefox | ✅ |
| Safari | ✅ |
| Edge | ✅ |
| Opera | ✅ |
| IE 11 | ❌ |

## 最佳实践

1. **兼容性检查**：使用 `isEnabled` 检查浏览器支持
2. **状态同步**：监听 `change` 事件同步状态
3. **事件清理**：组件卸载时移除事件监听
4. **错误处理**：处理 `error` 事件提供友好提示
5. **快捷键**：提供快捷键支持提升体验
