---
name: element-plus
description: "Element Plus 组件使用 Skill 文档"
metadata:
  short-description: "Element Plus 组件使用 Skill 文档"
---

# Element Plus 组件使用 Skill 文档

> 官方文档: https://element-plus.org/zh-CN/guide/design.html

## 概述

Element Plus 是一套基于 Vue 3 的组件库，提供了一套完整的桌面端组件。

## 设计原则

### 一致性 (Consistency)
- 与现实生活一致：遵循用户习惯的语言和概念
- 界面中一致：设计样式、图标和文本、元素的位置保持一致

### 反馈 (Feedback)
- 控制反馈：通过界面样式和交互动效让用户感知操作
- 页面反馈：操作后通过页面元素变化展现当前状态

### 效率 (Efficiency)
- 简化流程：简洁直观的操作流程
- 清晰明确：语言表达清晰且表意明确

### 可控 (Controllability)
- 用户决策：根据场景给予用户操作建议，但不代替决策
- 结果可控：用户可以自由进行操作，包括撤销、回退和终止

## 安装与配置

```bash
```

### 完整引入

```typescript
// main.ts
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

app.use(ElementPlus, { locale: zhCn })
```

### 按需引入 (推荐)

```typescript
// vite.config.ts
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
})
```

## 常用组件

### 按钮 (ElButton)

```vue
<template>
  <!-- 基础用法 -->
  <el-button>默认按钮</el-button>
  <el-button type="primary">主要按钮</el-button>
  <el-button type="success">成功按钮</el-button>
  <el-button type="warning">警告按钮</el-button>
  <el-button type="danger">危险按钮</el-button>
  <el-button type="info">信息按钮</el-button>

  <!-- plain 按钮 -->
  <el-button plain>朴素按钮</el-button>
  <el-button type="primary" plain>主要按钮</el-button>

  <!-- 圆角按钮 -->
  <el-button round>圆角按钮</el-button>

  <!-- 图标按钮 -->
  <el-button :icon="Search" circle />

  <!-- 加载状态 -->
  <el-button :loading="true">加载中</el-button>

  <!-- 禁用状态 -->
  <el-button disabled>禁用按钮</el-button>

  <!-- 按钮组 -->
  <el-button-group>
    <el-button type="primary">上一页</el-button>
    <el-button type="primary">下一页</el-button>
  </el-button-group>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
</script>
```

### 表单 (ElForm)

```vue
<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
    <el-form-item label="活动名称" prop="name">
      <el-input v-model="form.name" />
    </el-form-item>

    <el-form-item label="活动区域" prop="region">
      <el-select v-model="form.region" placeholder="请选择活动区域">
        <el-option label="区域一" value="shanghai" />
        <el-option label="区域二" value="beijing" />
      </el-select>
    </el-form-item>

    <el-form-item label="活动时间" prop="date">
      <el-date-picker v-model="form.date" type="date" />
    </el-form-item>

    <el-form-item label="即时配送" prop="delivery">
      <el-switch v-model="form.delivery" />
    </el-form-item>

    <el-form-item label="活动性质" prop="type">
      <el-checkbox-group v-model="form.type">
        <el-checkbox label="美食/餐厅线上活动" />
        <el-checkbox label="地推活动" />
        <el-checkbox label="线下主题活动" />
        <el-checkbox label="单纯品牌曝光" />
      </el-checkbox-group>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="submitForm">创建</el-button>
      <el-button @click="resetForm">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref } from 'vue'

const formRef = ref()
const form = ref({
  name: '',
  region: '',
  date: '',
  delivery: false,
  type: [],
})

const rules = {
  name: [
    { required: true, message: '请输入活动名称', trigger: 'blur' },
    { min: 3, max: 5, message: '长度在 3 到 5 个字符', trigger: 'blur' },
  ],
  region: [
    { required: true, message: '请选择活动区域', trigger: 'change' },
  ],
}

function submitForm() {
  formRef.value.validate((valid) => {
    if (valid) {
      alert('submit!')
    }
  })
}

function resetForm() {
  formRef.value.resetFields()
}
</script>
```

### 表格 (ElTable)

```vue
<template>
  <el-table :data="tableData" stripe style="width: 100%">
    <el-table-column prop="date" label="日期" width="180" />
    <el-table-column prop="name" label="姓名" width="180" />
    <el-table-column prop="address" label="地址" />
    <el-table-column label="操作" width="180">
      <template #default="{ row }">
        <el-button type="primary" link @click="handleEdit(row)">
          编辑
        </el-button>
        <el-button type="danger" link @click="handleDelete(row)">
          删除
        </el-button>
      </template>
    </el-table-column>
  </el-table>

  <!-- 分页 -->
  <el-pagination
    v-model:current-page="currentPage"
    v-model:page-size="pageSize"
    :page-sizes="[10, 20, 50, 100]"
    layout="total, sizes, prev, pager, next, jumper"
    :total="1000"
    @size-change="handleSizeChange"
    @current-change="handleCurrentChange"
  />
</template>

<script setup>
import { ref } from 'vue'

const tableData = ref([
  { date: '2016-05-02', name: '王小虎', address: '上海市普陀区金沙江路 1518 弄' },
  { date: '2016-05-04', name: '王小虎', address: '上海市普陀区金沙江路 1517 弄' },
  { date: '2016-05-01', name: '王小虎', address: '上海市普陀区金沙江路 1519 弄' },
])

const currentPage = ref(1)
const pageSize = ref(10)

function handleEdit(row) {
  console.log('编辑', row)
}

function handleDelete(row) {
  console.log('删除', row)
}

function handleSizeChange(val) {
  console.log(`每页 ${val} 条`)
}

function handleCurrentChange(val) {
  console.log(`当前页: ${val}`)
}
</script>
```

### 对话框 (ElDialog)

```vue
<template>
  <el-button type="text" @click="dialogVisible = true">
    点击打开 Dialog
  </el-button>

  <el-dialog
    v-model="dialogVisible"
    title="提示"
    width="30%"
    :before-close="handleClose"
  >
    <span>这是一段信息</span>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="dialogVisible = false">
          确认
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

const dialogVisible = ref(false)

function handleClose(done) {
  ElMessageBox.confirm('确认关闭？')
    .then(() => {
      done()
    })
    .catch(() => {})
}
</script>
```

### 消息提示 (ElMessage)

```javascript
import { ElMessage } from 'element-plus'

// 成功
ElMessage.success('操作成功')

// 警告
ElMessage.warning('警告信息')

// 错误
ElMessage.error('错误信息')

// 普通
ElMessage('普通信息')

// 可关闭
ElMessage({
  message: '恭喜你，这是一条成功消息',
  type: 'success',
  showClose: true,
  duration: 3000,
})
```

### 弹框 (ElMessageBox)

```javascript
import { ElMessageBox } from 'element-plus'

// 确认消息
ElMessageBox.confirm('此操作将永久删除该文件, 是否继续?', '提示', {
  confirmButtonText: '确定',
  cancelButtonText: '取消',
  type: 'warning',
})
  .then(() => {
    ElMessage.success('删除成功!')
  })
  .catch(() => {
    ElMessage.info('已取消删除')
  })

// 提示内容
ElMessageBox.alert('这是一段内容', '标题', {
  confirmButtonText: '确定',
  callback: (action) => {
    ElMessage.info(`action: ${action}`)
  },
})
```

### 卡片 (ElCard)

```vue
<template>
  <el-card class="box-card">
    <template #header>
      <div class="card-header">
        <span>卡片名称</span>
        <el-button class="button" text>操作按钮</el-button>
      </div>
    </template>
    <div v-for="o in 4" :key="o" class="text item">
      {{ '列表内容 ' + o }}
    </div>
  </el-card>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.text item {
  margin-bottom: 18px;
}
</style>
```

### 标签页 (ElTabs)

```vue
<template>
  <el-tabs v-model="activeTab" @tab-click="handleClick">
    <el-tab-pane label="用户管理" name="first">用户管理</el-tab-pane>
    <el-tab-pane label="配置管理" name="second">配置管理</el-tab-pane>
    <el-tab-pane label="角色管理" name="third">角色管理</el-tab-pane>
    <el-tab-pane label="定时任务补偿" name="fourth">定时任务补偿</el-tab-pane>
  </el-tabs>
</template>

<script setup>
import { ref } from 'vue'

const activeTab = ref('first')

function handleClick(tab) {
  console.log(tab)
}
</script>
```

### 下拉菜单 (ElDropdown)

```vue
<template>
  <el-dropdown>
    <span class="el-dropdown-link">
      下拉菜单
      <el-icon class="el-icon--right">
        <arrow-down />
      </el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item>黄金糕</el-dropdown-item>
        <el-dropdown-item>狮子头</el-dropdown-item>
        <el-dropdown-item divided>螺蛳粉</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { ArrowDown } from '@element-plus/icons-vue'
</script>
```

## 图标使用

```vue
<template>
  <!-- 直接使用图标组件 -->
  <el-icon :size="20">
    <Edit />
  </el-icon>

  <!-- 通过属性使用 -->
  <el-button :icon="Search" />

  <!-- 动态图标 -->
  <el-icon>
    <component :is="dynamicIcon" />
  </el-icon>
</template>

<script setup>
import { ref } from 'vue'
import { Edit, Search, Check } from '@element-plus/icons-vue'

const dynamicIcon = ref(Check)
</script>
```

## 暗色模式

```vue
<template>
  <!-- 暗色模式下的组件会自动应用暗色样式 -->
  <html class="dark">
    <el-button>暗色按钮</el-button>
    <el-table :data="data" />
  </html>
</template>
```

## 最佳实践

1. **按需引入**：使用自动导入插件按需引入组件，减小打包体积
2. **全局配置**：统一配置组件尺寸、z-index 等全局属性
3. **主题定制**：通过 CSS 变量定制主题颜色
4. **国际化**：根据用户语言动态切换 locale
5. **表单验证**：合理设置验证规则，提供友好的错误提示

## 主题定制

```css
/* styles/element-variables.css */
:root {
  --el-color-primary: #409eff;
  --el-color-success: #67c23a;
  --el-color-warning: #e6a23c;
  --el-color-danger: #f56c6c;
  --el-color-error: #f56c6c;
  --el-color-info: #909399;
}
```
