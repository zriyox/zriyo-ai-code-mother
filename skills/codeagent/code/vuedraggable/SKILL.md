---
name: vuedraggable
description: "VueDraggable 拖拽排序 Skill 文档"
metadata:
  short-description: "VueDraggable 拖拽排序 Skill 文档"
---

# VueDraggable 拖拽排序 Skill 文档

## 概述

VueDraggable 是基于 Sortable.js 的 Vue 3 拖拽排序组件，支持触摸设备和大多数现代浏览器。

## 安装

```bash
```

## 基础用法

### 简单列表排序

```vue
<script setup lang="ts">
import draggable from 'vuedraggable'
import { ref } from 'vue'

const items = ref(['Item 1', 'Item 2', 'Item 3', 'Item 4', 'Item 5'])
</script>

<template>
  <draggable v-model="items" item-key="id">
    <template #item="{ element }">
      <div class="drag-item">{{ element }}</div>
    </template>
  </draggable>
</template>

<style scoped>
.drag-item {
  padding: 12px;
  margin: 8px 0;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: move;
}
</style>
```

### 对象数组排序

```vue
<script setup lang="ts">
import draggable from 'vuedraggable'
import { ref } from 'vue'

interface Item {
  id: number
  name: string
}

const items = ref<Item[]>([
  { id: 1, name: 'Item 1' },
  { id: 2, name: 'Item 2' },
  { id: 3, name: 'Item 3' },
])
</script>

<template>
  <draggable v-model="items" item-key="id">
    <template #item="{ element }">
      <div class="drag-item">
        {{ element.id }} - {{ element.name }}
      </div>
    </template>
  </draggable>
</template>
```

## 常用配置

### 禁用拖拽

```vue
<draggable
  v-model="items"
  item-key="id"
  :disabled="false"
>
  <!-- ... -->
</draggable>
```

### 设置动画

```vue
<draggable
  v-model="items"
  item-key="id"
  :animation="200"
>
  <!-- ... -->
</draggable>
```

### 设置拖拽手柄

```vue
<draggable v-model="items" item-key="id" handle=".handle">
  <template #item="{ element }">
    <div class="drag-item">
      <span class="handle">☰</span>
      {{ element.name }}
    </div>
  </template>
</draggable>
```

### 拖拽样式

```vue
<draggable
  v-model="items"
  item-key="id"
  ghost-class="ghost"
  drag-class="drag"
  chosen-class="chosen"
>
  <!-- ... -->
</draggable>

<style scoped>
.ghost {
  opacity: 0.5;
  background: #c8ebfb;
}

.drag {
  opacity: 1;
  background: #fff;
}

.chosen {
  border: 2px solid #4a90e2;
}
</style>
```

## 事件处理

### 事件监听

```vue
<script setup lang="ts">
import draggable from 'vuedraggable'
import { ref } from 'vue'

const items = ref(['Item 1', 'Item 2', 'Item 3'])

const onStart = () => {
  console.log('开始拖拽')
}

const onEnd = (event: any) => {
  console.log('拖拽结束', event)
  console.log('旧索引:', event.oldIndex)
  console.log('新索引:', event.newIndex)
}

const onMove = (event: any) => {
  console.log('移动中', event)
  return true // 返回 false 可以阻止移动
}

const onAdd = (event: any) => {
  console.log('添加元素', event)
}

const onRemove = (event: any) => {
  console.log('移除元素', event)
}
</script>

<template>
  <draggable
    v-model="items"
    item-key="id"
    @start="onStart"
    @end="onEnd"
    @move="onMove"
    @add="onAdd"
    @remove="onRemove"
  >
    <!-- ... -->
  </draggable>
</template>
```

## 常用场景

### 卡片拖拽排序

```vue
<script setup lang="ts">
import draggable from 'vuedraggable'
import { ref } from 'vue'

interface Card {
  id: number
  title: string
  description: string
}

const cards = ref<Card[]>([
  { id: 1, title: '卡片 1', description: '这是卡片 1 的描述' },
  { id: 2, title: '卡片 2', description: '这是卡片 2 的描述' },
  { id: 3, title: '卡片 3', description: '这是卡片 3 的描述' },
])
</script>

<template>
  <div class="card-list">
    <draggable
      v-model="cards"
      item-key="id"
      :animation="300"
      ghost-class="ghost-card"
    >
      <template #item="{ element }">
        <el-card class="card-item">
          <template #header>
            <div class="card-header">
              <span>{{ element.title }}</span>
              <el-icon class="drag-handle"><Rank /></el-icon>
            </div>
          </template>
          <p>{{ element.description }}</p>
        </el-card>
      </template>
    </draggable>
  </div>
</template>

<style scoped>
.card-list {
  padding: 20px;
}

.card-item {
  margin-bottom: 16px;
  cursor: move;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ghost-card {
  opacity: 0.5;
  background: #f0f9ff;
}
</style>
```

### 看板拖拽（列之间）

```vue
<script setup lang="ts">
import draggable from 'vuedraggable'
import { ref } from 'vue'

interface Task {
  id: string
  title: string
}

interface Column {
  name: string
  tasks: Task[]
}

const columns = ref<Column[]>([
  {
    name: '待办',
    tasks: [
      { id: '1', title: '任务 1' },
      { id: '2', title: '任务 2' },
    ],
  },
  {
    name: '进行中',
    tasks: [
      { id: '3', title: '任务 3' },
    ],
  },
  {
    name: '已完成',
    tasks: [],
  },
])

const onLog = (evt: any) => {
  window.console.log(evt)
}
</script>

<template>
  <div class="board">
    <div v-for="(column, index) in columns" :key="index" class="column">
      <h3>{{ column.name }}</h3>
      <draggable
        :model-value="column.tasks"
        item-key="id"
        :group="{ name: 'tasks' }"
        @change="
          (evt: any) => {
            if (evt.added) {
              columns[evt.added.newIndex].tasks = evt.added.element
            }
          }
        "
        class="task-list"
      >
        <template #item="{ element }">
          <div class="task-card">{{ element.title }}</div>
        </template>
      </draggable>
    </div>
  </div>
</template>

<style scoped>
.board {
  display: flex;
  gap: 16px;
  padding: 20px;
}

.column {
  flex: 1;
  min-width: 250px;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 16px;
}

.column h3 {
  margin-top: 0;
  margin-bottom: 16px;
}

.task-list {
  min-height: 100px;
}

.task-card {
  padding: 12px;
  margin-bottom: 8px;
  background: white;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  cursor: move;
}
</style>
```

### 嵌套拖拽

```vue
<script setup lang="ts">
import draggable from 'vuedraggable'
import { ref } from 'vue'

interface NestedItem {
  id: number
  name: string
  children?: NestedItem[]
}

const items = ref<NestedItem[]>([
  {
    id: 1,
    name: '分组 1',
    children: [
      { id: 11, name: '项目 1-1' },
      { id: 12, name: '项目 1-2' },
    ],
  },
  {
    id: 2,
    name: '分组 2',
    children: [
      { id: 21, name: '项目 2-1' },
      { id: 22, name: '项目 2-2' },
    ],
  },
])
</script>

<template>
  <draggable v-model="items" item-key="id" class="group-list">
    <template #item="{ element }">
      <div class="group">
        <div class="group-header">{{ element.name }}</div>
        <draggable
          v-model="element.children"
          item-key="id"
          class="item-list"
        >
          <template #item="{ element: child }">
            <div class="item">{{ child.name }}</div>
          </template>
        </draggable>
      </div>
    </template>
  </draggable>
</template>

<style scoped>
.group-list,
.item-list {
  min-height: 50px;
}

.group {
  margin: 16px 0;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
}

.group-header {
  font-weight: bold;
  margin-bottom: 12px;
}

.item {
  padding: 8px 12px;
  margin: 8px 0;
  background: white;
  border-radius: 4px;
  cursor: move;
}
</style>
```

### 跨列表拖拽

```vue
<script setup lang="ts">
import draggable from 'vuedraggable'
import { ref } from 'vue'

const list1 = ref(['A', 'B', 'C'])
const list2 = ref(['D', 'E', 'F'])

const log = (event: any) => {
  console.log(event)
}
</script>

<template>
  <div class="lists">
    <div class="list-container">
      <h3>列表 1</h3>
      <draggable
        v-model="list1"
        item-key="id"
        :group="{ name: 'people', pull: true, put: true }"
        @change="log"
      >
        <template #item="{ element }">
          <div class="list-item">{{ element }}</div>
        </template>
      </draggable>
    </div>

    <div class="list-container">
      <h3>列表 2</h3>
      <draggable
        v-model="list2"
        item-key="id"
        :group="{ name: 'people', pull: true, put: true }"
        @change="log"
      >
        <template #item="{ element }">
          <div class="list-item">{{ element }}</div>
        </template>
      </draggable>
    </div>
  </div>
</template>

<style scoped>
.lists {
  display: flex;
  gap: 32px;
}

.list-container {
  flex: 1;
}

.list-item {
  padding: 12px;
  margin: 8px 0;
  background: #f5f5f5;
  border-radius: 4px;
  cursor: move;
}
</style>
```

## 属性说明

### Props

| 属性 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| modelValue | Array | [] | 列表数据（v-model） |
| itemKey | string \| function | 'id' | 每个项的唯一键 |
| group | string \| object | null | 拖拽分组 |
| animation | number | 0 | 动画时长（ms） |
| handle | string | null | 拖拽手柄选择器 |
| disabled | boolean | false | 是否禁用 |
| delay | number | 0 | 延迟开始拖拽（ms） |
| delayOnTouchOnly | boolean | false | 仅在触摸设备延迟 |
| ghostClass | string | null | 拖拽时占位元素的类名 |
| dragClass | string | null | 拖拽元素的类名 |
| chosenClass | string | null | 选中元素的类名 |
| forceFallback | boolean | false | 强制使用 fallback |
| fallbackClass | string | null | fallback 元素的类名 |
| fallbackOnBody | boolean | false | 将 fallback 添加到 body |
| swapThreshold | number | 1 | 交换阈值 |
| invertSwap | boolean | false | 反转交换 |
| direction | string | vertical | 排序方向 |
| lockAxis | string | null | 锁定轴向 |

### Events

| 事件 | 参数 | 描述 |
|------|------|------|
| @start | event | 开始拖拽 |
| @end | event | 结束拖拽 |
| @move | event | 移动中 |
| @add | event | 添加元素 |
| @remove | event | 移除元素 |
| @change | event | 列表变化 |

### Slots

| 插槽 | 描述 |
|------|------|
| default | 列表项 |
| header | 列表头部 |
| footer | 列表尾部 |

## 最佳实践

1. **设置唯一 key**：确保每个元素有唯一的 itemKey
2. **合理设置动画**：200-300ms 的动画效果最好
3. **使用手柄**：复杂元素建议使用拖拽手柄
4. **样式反馈**：通过 ghostClass 和 chosenClass 提供视觉反馈
5. **性能优化**：大数据量时使用虚拟滚动
