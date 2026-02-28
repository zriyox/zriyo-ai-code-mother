---
name: system
description: "前端脚手架开发规范"
metadata:
  short-description: "前端脚手架开发规范"
---

# 前端脚手架开发规范

## 技术栈

Vue 3.5.13 + TypeScript 5.8.0 + Vite 6.3.5
Vue Router 4.5.0 + Pinia 2.2.8
Element Plus 2.10.4 + Tailwind CSS 3.4.17
ECharts 5.6.0 + Three.js 0.173.0
@vueuse/core 11.3.0 + Day.js + Lodash-es

## 代码规范

### 组件格式
```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

interface Props {
  title: string
}
const props = defineProps<Props>()
const emit = defineEmits<{
  change: [value: string]
}>()

const count = ref(0)
const double = computed(() => count.value * 2)

function increment() {
  count.value++
  emit('change', count.value.toString())
}
</script>

<template>
  <div class="p-4">
    <h1>{{ title }}</h1>
    <p>{{ count }}</p>
    <button @click="increment">+1</button>
  </div>
</template>

<style scoped lang="scss">
</style>
```

### 命名规范
- 组件文件：PascalCase（UserProfile.vue）
- 页面文件：PascalCase（Dashboard.vue）
- 工具文件：kebab-case（format-date.ts）
- 变量/函数：camelCase（getUserInfo）

### 样式优先级
1. 优先使用 Tailwind CSS 工具类（class="p-4 m-2 flex"）
2. 复杂样式使用 scoped SCSS
3. 禁止内联样式

### 类型定义
- Props 必须定义 interface
- 禁止使用 any
- 使用 defineProps<Props>() 泛型语法

## 禁止事项

- ❌ 禁止 jQuery，使用 Vue 原生 API
- ❌ 禁止 Moment.js，使用 Day.js
- ❌ 禁止混用其他 UI 框架，只使用 Element Plus
- ❌ 禁止 this，使用 Composition API
- ❌ 禁止 Options API，使用 setup 语法糖

## 自动导入

无需手动引入，可直接使用：
- Vue: ref, computed, onMounted, useRouter, useRoute
- Pinia: defineStore, storeToRefs
- VueUse: useWindowSize, useLocalStorage, useClipboard
- Element Plus: 所有组件自动注册

## 图标使用

```vue
<Icon icon="mdi:home" />
<Icon icon="heroicons:user" />
```

常用前缀：mdi, heroicons, tabler, carbon, fa-solid, fa-brands

## 目录结构

```
src/
├── api/           # API 接口
├── assets/        # 静态资源
├── components/    # 公共组件
│   ├── common/    # 通用组件
│   ├── business/  # 业务组件
│   ├── 3d/        # 3D 组件
│   └── charts/    # 图表组件
├── composables/   # 组合式函数
├── layouts/       # 布局组件
├── router/        # 路由配置
├── stores/        # Pinia 状态
├── types/         # TypeScript 类型
├── utils/         # 工具函数
└── views/         # 页面组件
```

## API 请求

```typescript
import request from '@/utils/request'

request.get('/api/data')
request.post('/api/data', { foo: 'bar' })
```

## 路由

```typescript
import { useRouter } from 'vue-router'

const router = useRouter()
router.push('/path')
```

## Store

```typescript
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
userStore.updateInfo(data)
```

## 日期处理

```typescript
import dayjs from 'dayjs'

dayjs().format('YYYY-MM-DD')
dayjs().add(1, 'day')
```

## 参考文档

详细 API 和示例请查看 skills/codeagent/ 目录下对应文档：
- 图表 → code/echarts/SKILL.md
- 3D → code/threejs/SKILL.md
- 拖拽 → code/vuedraggable/SKILL.md
- 代码编辑器 → code/codemirror/SKILL.md
- 国际化 → code/vue-i18n/SKILL.md
- UI/UX 设计系统 → code/ui-ux-pro-max/SKILL.md（仅在明确提到时启用）
