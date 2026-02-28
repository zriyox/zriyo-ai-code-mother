---
name: iconify
description: "@iconify/vue Iconify 图标库 Skill 文档"
metadata:
  short-description: "@iconify/vue Iconify 图标库 Skill 文档"
---

# @iconify/vue Iconify 图标库 Skill 文档

## 概述

Iconify 是一个统一的图标框架，支持超过 200,000 个来自 150+ 图标集的图标。`@iconify/vue` 是 Vue 3 的官方组件。

## 安装

```bash
```

## 基础用法

### 使用 Icon 组件

```vue
<script setup lang="ts">
import { Icon } from '@iconify/vue'
</script>

<template>
  <!-- 基础用法 -->
  <Icon icon="mdi:home" />

  <!-- 自定义大小 -->
  <Icon icon="mdi:home" width="24" height="24" />

  <!-- 自定义颜色 -->
  <Icon icon="mdi:home" color="#3b82f6" />

  <!-- 旋转 -->
  <Icon icon="mdi:home" :rotate="1" />
</template>
```

### 使用离线图标

```vue
<script setup lang="ts">
import Icon from '@iconify/vue/offline'
import mdiHome from '@iconify-icons/mdi/home'
</script>

<template>
  <Icon :icon="mdiHome" />
</template>
```

## 常用图标集

### Material Design Icons (mdi)

```vue
<Icon icon="mdi:home" />
<Icon icon="mdi:account" />
<Icon icon="mdi:settings" />
<Icon icon="mdi:delete" />
<Icon icon="mdi:check" />
<Icon icon="mdi:close" />
```

### Heroicons

```vue
<Icon icon="heroicons:home" />
<Icon icon="heroicons:user" />
<Icon icon="heroicons:cog-6-tooth" />
<Icon icon="heroicons:x-mark" />
```

### Tabler Icons

```vue
<Icon icon="tabler:home" />
<Icon icon="tabler:settings" />
<Icon icon="tabler:trash" />
```

### Font Awesome

```vue
<Icon icon="fa:home" />
<Icon icon="fa-solid:user" />
<Icon icon="fa-brands:github" />
```

### Carbon Design System

```vue
<Icon icon="carbon:home" />
<Icon icon="carbon:settings" />
<Icon icon="carbon:dashboard" />
```

## 组件属性

### 尺寸相关

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| width | string \| number | null | 图标宽度 |
| height | string \| number | null | 图标高度 |
| size | string \| number | null | 同时设置宽高 |
| minWidth | string \| number | null | 最小宽度 |
| minHeight | string \| number | null | 最小高度 |
| maxWidth | string \| number | null | 最大宽度 |
| maxHeight | string \| number | null | 最大高度 |

### 样式相关

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| color | string | null | 图标颜色 |
| horizontalFlip | boolean | false | 水平翻转 |
| verticalFlip | boolean | false | 垂直翻转 |
| rotate | number | 0 | 旋转角度（1 = 90度） |
| inline | boolean | false | 行内模式 |

### 动画相关

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| spin | boolean | false | 旋转动画 |
| pulse | boolean | false | 脉冲动画 |

### 加载相关

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| onLoad | () => void | null | 加载完成回调 |
| onError | (e: Event) => void | null | 加载失败回调 |

## 使用场景

### 按钮图标

```vue
<script setup lang="ts">
import { Icon } from '@iconify/vue'
</script>

<template>
  <el-button>
    <Icon icon="mdi:plus" />
    新建
  </el-button>

  <el-button type="danger">
    <Icon icon="mdi:delete" />
    删除
  </el-button>

  <el-button circle>
    <Icon icon="mdi:settings" />
  </el-button>
</template>
```

### 菜单图标

```vue
<script setup lang="ts">
import { Icon } from '@iconify/vue'

interface MenuItem {
  icon: string
  label: string
  path: string
}

const menuItems: MenuItem[] = [
  { icon: 'mdi:home', label: '首页', path: '/' },
  { icon: 'mdi:account', label: '用户', path: '/users' },
  { icon: 'mdi:settings', label: '设置', path: '/settings' },
]
</script>

<template>
  <el-menu>
    <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
      <Icon :icon="item.icon" />
      <span>{{ item.label }}</span>
    </el-menu-item>
  </el-menu>
</template>
```

### 状态图标

```vue
<script setup lang="ts">
import { Icon } from '@iconify/vue'

interface StatusIconProps {
  status: 'success' | 'warning' | 'error' | 'info'
}

const props = defineProps<StatusIconProps>()

const statusConfig = {
  success: { icon: 'mdi:check-circle', color: '#67c23a' },
  warning: { icon: 'mdi:alert', color: '#e6a23c' },
  error: { icon: 'mdi:close-circle', color: '#f56c6c' },
  info: { icon: 'mdi:information', color: '#909399' },
}
</script>

<template>
  <Icon
    :icon="statusConfig[status].icon"
    :color="statusConfig[status].color"
  />
</template>
```

### 动态图标

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'

const loading = ref(false)
const icon = computed(() => loading.value ? 'mdi:loading' : 'mdi:check')
</script>

<template>
  <Icon :icon="icon" :spin="loading" />
</template>
```

### 加载动画

```vue
<template>
  <!-- 旋转动画 -->
  <Icon icon="mdi:loading" :spin="true" />

  <!-- 脉冲动画 -->
  <Icon icon="mdi:loading" :pulse="true" />
</template>
```

## 自动导入配置

### unplugin-icons 集成

```typescript
// vite.config.ts
import Icons from 'unplugin-icons/vite'
import IconsResolver from 'unplugin-icons/resolver'
import Components from 'unplugin-vue-components/vite'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [
    Icons({
      autoInstall: true,
    }),
    Components({
      resolvers: [
        IconsResolver({
          componentPrefix: '',
        }),
      ],
    }),
  ],
})
```

### 自动导入使用

```vue
<template>
  <!-- 无需导入，直接使用 -->
  <i-mdi-home />
  <i-carbon-settings />
  <i-heroicons-user />
</template>
```

## 图标搜索

### 官网搜索

访问 https://icon-sets.iconify.design/ 搜索需要的图标。

### API 搜索

```typescript
import { searchIcons } from '@iconify/vue'

// 搜索图标
const results = await searchIcons('home')

// 获取图标集信息
const collection = await getIconCollection('mdi')
```

## 最佳实践

1. **统一使用 Iconify**：避免混用多个图标库
2. **保持图标一致**：同一功能使用相同图标集
3. **合理设置尺寸**：优先使用 `size` 属性
4. **离线使用**：生产环境建议使用离线图标
5. **按需加载**：使用离线图标减少网络请求

## 常用图标速查

| 功能 | Material Design | Heroicons | Tabler |
|------|-----------------|-----------|--------|
| 首页 | mdi:home | heroicons:home | tabler:home |
| 设置 | mdi:settings | heroicons:cog-6-tooth | tabler:settings |
| 用户 | mdi:account | heroicons:user | tabler:user |
| 搜索 | mdi:magnify | heroicons:magnifying-glass | tabler:search |
| 关闭 | mdi:close | heroicons:x-mark | tabler:x |
| 添加 | mdi:plus | heroicons:plus | tabler:plus |
| 删除 | mdi:delete | heroicons:trash | tabler:trash |
| 编辑 | mdi:pencil | heroicons:pencil | tabler:edit |
| 保存 | mdi:content-save | heroicons:document-check | tabler:device-floppy |
| 刷新 | mdi:refresh | heroicons:arrow-path | tabler:refresh |
| 下载 | mdi:download | heroicons:arrow-down-tray | tabler:download |
| 上传 | mdi:upload | heroicons:arrow-up-tray | tabler:upload |
| 邮件 | mdi:email | heroicons:envelope | tabler:mail |
| 通知 | mdi:bell | heroicons:bell | tabler:bell |
| 收藏 | mdi:star | heroicons:star | tabler:star |
| 日历 | mdi:calendar | heroicons:calendar | tabler:calendar |
| 时间 | mdi:clock | heroicons:clock | tabler:clock |
| 文件 | mdi:file | heroicons:document | tabler:file |
| 文件夹 | mdi:folder | heroicons:folder | tabler:folder |
| 链接 | mdi:link | heroicons:link | tabler:link |
| 复制 | mdi:content-copy | heroicons:clipboard-document | tabler:copy |
| 剪切 | mdi:content-cut | heroicons:scissors | tabler:cut |
| 粘贴 | mdi:content-paste | heroicons:clipboard | tabler:clipboard |
| 撤销 | mdi:undo | heroicons:arrow-uturn-left | tabler:undo |
| 重做 | mdi:redo | heroicons:arrow-uturn-right | tabler:redo |
| 更多 | mdi:dots-vertical | heroicons:ellipsis-vertical | tabler:dots-vertical |
| 菜单 | mdi:menu | heroicons:bars-3 | tabler:menu-2 |
| 箭头左 | mdi:arrow-left | heroicons:arrow-left | tabler:arrow-left |
| 箭头右 | mdi:arrow-right | heroicons:arrow-right | tabler:arrow-right |
| 箭头上 | mdi:arrow-up | heroicons:arrow-up | tabler:arrow-up |
| 箭头下 | mdi:arrow-down | heroicons:arrow-down | tabler:arrow-down |
| 外部链接 | mdi:open-in-new | heroicons:arrow-top-right-on-square | tabler:external-link |
