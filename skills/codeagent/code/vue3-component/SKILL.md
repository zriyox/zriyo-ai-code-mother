---
name: vue3-component
description: "Vue 3 组件开发 Skill 文档"
metadata:
  short-description: "Vue 3 组件开发 Skill 文档"
---

# Vue 3 组件开发 Skill 文档

> 官方文档: https://cn.vuejs.org/guide/introduction.html

## 概述

Vue (发音为 /vjuː/，类似 view) 是一款用于构建用户界面的 JavaScript 框架。它基于标准 HTML、CSS 和 JavaScript 构建，并提供了一套声明式的、组件化的编程模型。

## 核心概念

### 1. 声明式渲染

Vue 基于标准 HTML 拓展了一套模板语法，可以声明式地描述 HTML 和 JavaScript 状态之间的关系。

```vue
<template>
  <button @click="count++">
    Count is: {{ count }}
  </button>
</template>

<script setup>
import { ref } from 'vue'

const count = ref(0)
</script>
```

### 2. 响应性

Vue 会自动跟踪 JavaScript 状态并在其发生变化时响应式地更新 DOM。

```javascript
import { ref } from 'vue'

// 响应式状态
const count = ref(0)
count.value++ // 修改值会触发视图更新
```

### 3. 单文件组件 (SFC)

Vue 的单文件组件将逻辑、模板和样式封装在同一个文件中：

```vue
<script setup>
import { ref } from 'vue'

const message = ref('Hello')
</script>

<template>
  <div>{{ message }}</div>
</template>

<style scoped>
div {
  color: blue;
}
</style>
```

## API 风格

### 组合式 API (Composition API) - 推荐

使用 `<script setup>` 语法：

```vue
<script setup>
import { ref, onMounted } from 'vue'

const count = ref(0)

function increment() {
  count.value++
}

onMounted(() => {
  console.log('Component mounted')
})
</script>
```

### 选项式 API (Options API)

```vue
<script>
export default {
  data() {
    return { count: 0 }
  },
  methods: {
    increment() {
      this.count++
    }
  },
  mounted() {
    console.log('Component mounted')
  }
}
</script>
```

## 组件基础

### Props 定义

```vue
<script setup>
const props = defineProps({
  title: String,
  count: {
    type: Number,
    default: 0
  }
})

// 或使用 TypeScript
interface Props {
  title: string
  count?: number
}

const props = withDefaults(defineProps<Props>(), {
  count: 0
})
</script>
```

### Emits 定义

```vue
<script setup>
const emit = defineEmits<{
  click: [event: MouseEvent]
  change: [value: string]
}>()

function handleClick(e: MouseEvent) {
  emit('click', e)
  emit('change', 'new value')
}
</script>
```

### 插槽 (Slots)

```vue
<!-- 父组件 -->
<template>
  <MyComponent>
    <template #default>
      默认内容
    </template>
    <template #header>
      头部内容
    </template>
  </MyComponent>
</template>

<!-- 子组件 MyComponent.vue -->
<template>
  <div>
    <slot name="header">默认头部</slot>
    <slot>默认内容</slot>
  </div>
</template>
```

## 生命周期钩子

```javascript
import { onMounted, onUpdated, onUnmounted } from 'vue'

onMounted(() => {
  console.log('组件已挂载')
})

onUpdated(() => {
  console.log('组件已更新')
})

onUnmounted(() => {
  console.log('组件已卸载')
})
```

## 计算属性与侦听器

```javascript
import { ref, computed, watch } from 'vue'

const count = ref(0)

// 计算属性
const doubleCount = computed(() => count.value * 2)

// 侦听器
watch(count, (newValue, oldValue) => {
  console.log(`count 从 ${oldValue} 变为 ${newValue}`)
})

// 深度侦听
watch(state, () => {
  // ...
}, { deep: true })
```

## 组件通信

### 父传子 (Props)

```vue
<!-- 父组件 -->
<template>
  <ChildComponent :message="parentMessage" />
</template>

<!-- 子组件 -->
<script setup>
const props = defineProps<{
  message: string
}>()
</script>
```

### 子传父 (Emit)

```vue
<!-- 父组件 -->
<template>
  <ChildComponent @update="handleUpdate" />
</template>

<!-- 子组件 -->
<script setup>
const emit = defineEmits<{
  update: [value: string]
}>()

function sendUpdate() {
  emit('update', 'new value')
}
</script>
```

### 依赖注入 (Provide/Inject)

```javascript
// 父组件
import { provide } from 'vue'

provide('theme', 'dark')

// 子组件
import { inject } from 'vue'

const theme = inject('theme', 'light')
```

## 条件渲染

```vue
<template>
  <!-- v-if: 条件性地渲染元素 -->
  <div v-if="type === 'A'">A</div>
  <div v-else-if="type === 'B'">B</div>
  <div v-else>其他</div>

  <!-- v-show: 切换 display 属性 -->
  <div v-show="isVisible">可见内容</div>
</template>
```

## 列表渲染

```vue
<template>
  <!-- 遍历数组 -->
  <li v-for="(item, index) in items" :key="item.id">
    {{ index }} - {{ item.name }}
  </li>

  <!-- 遍历对象 -->
  <li v-for="(value, key) in object" :key="key">
    {{ key }}: {{ value }}
  </li>
</template>
```

## 表单输入绑定

```vue
<template>
  <input v-model="text" placeholder="编辑文本" />

  <textarea v-model="message"></textarea>

  <input type="checkbox" v-model="checked" />

  <input type="radio" v-model="picked" value="选项1" />
  <input type="radio" v-model="picked" value="选项2" />

  <select v-model="selected">
    <option disabled value="">请选择</option>
    <option>A</option>
    <option>B</option>
  </select>
</template>

<script setup>
import { ref } from 'vue'

const text = ref('')
const message = ref('')
const checked = ref(false)
const picked = ref('')
const selected = ref('')
</script>
```

## 最佳实践

1. **使用 `<script setup>`**：更简洁的语法，更好的性能
2. **使用 TypeScript**：提供完整的类型支持
3. **组件命名**：使用 PascalCase，如 `MyComponent.vue`
4. **Props 验证**：始终定义 props 类型
5. **事件命名**：使用 kebab-case，如 `@update-value`
6. **单一职责**：每个组件只做一件事
7. **避免深层嵌套**：考虑拆分组件

## 组件目录结构

```
src/components/
├── common/           # 通用组件
│   ├── Button.vue
│   ├── Input.vue
│   └── Modal.vue
├── business/         # 业务组件
│   ├── UserCard.vue
│   └── ProductList.vue
└── layouts/          # 布局组件
    └── DefaultLayout.vue
```
