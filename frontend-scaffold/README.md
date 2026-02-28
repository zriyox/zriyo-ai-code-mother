# AI Generated Frontend Scaffold

> 一个完整的 Vue 3 + TypeScript 前端脚手架，专为 AI 生成代码设计

## ✨ 特性

- 🚀 **Vue 3** - 使用最新的 Vue 3 Composition API
- 💻 **TypeScript** - 完整的类型支持
- 🎨 **Element Plus** - 丰富的 UI 组件库
- 📊 **ECharts** - 强大的图表库
- 🎮 **Three.js** - 3D 图形支持 (通过 TresJS)
- 🎯 **Tailwind CSS** - 原子化 CSS 框架
- 📦 **Vite** - 快速的构建工具
- 🌙 **暗色模式** - 完整的主题支持
- 🌐 **i18n** - 国际化支持

## 📦 技术栈

### 核心框架
- Vue 3.5+
- Vue Router 4.x
- Pinia 2.x
- Vite 6.x

### UI 组件
- Element Plus 2.x
- @iconify/vue (图标)
- Headless UI (自定义组件)

### 图表
- ECharts 5.x
- vue-echarts

### 3D 图形
- Three.js
- @tresjs/core
- @tresjs/cientos

### 工具库
- Axios (HTTP)
- dayjs (日期)
- lodash-es (工具函数)
- @vueuse/core (组合式函数)

### 代码编辑器
- CodeMirror 6.x

## 📁 项目结构

```
frontend-scaffold/
├── src/
│   ├── api/                    # API 请求
│   │   ├── request.ts         # Axios 封装
│   │   └── python.ts          # Python 服务 API
│   ├── assets/                # 静态资源
│   │   ├── images/
│   │   ├── icons/
│   │   └── styles/
│   │       ├── index.scss    # 全局样式
│   │       └── variables.scss # SCSS 变量
│   ├── components/            # 组件
│   │   ├── common/           # 通用组件
│   │   │   ├── SvgIcon.vue
│   │   │   ├── PageLoading.vue
│   │   │   └── EmptyState.vue
│   │   ├── 3d/               # 3D 组件
│   │   │   └── Scene3D.vue
│   │   ├── charts/           # 图表组件
│   │   │   └── ChartCard.vue
│   │   └── business/         # 业务组件
│   ├── composables/          # 组合式函数
│   ├── directives/           # 自定义指令
│   │   └── index.ts
│   ├── layouts/              # 布局组件
│   │   ├── DefaultLayout.vue
│   │   └── BlankLayout.vue
│   ├── router/               # 路由配置
│   │   └── index.ts
│   ├── stores/               # 状态管理
│   │   ├── app.ts
│   │   └── user.ts
│   ├── types/                # 类型定义
│   │   ├── index.d.ts
│   │   └── env.d.ts
│   ├── utils/                # 工具函数
│   ├── views/                # 页面组件
│   │   └── pages/
│   │       ├── Dashboard.vue
│   │       ├── Projects.vue
│   │       ├── ProjectDetail.vue
│   │       ├── Settings.vue
│   │       ├── Login.vue
│   │       └── NotFound.vue
│   ├── App.vue               # 根组件
│   └── main.ts               # 入口文件
├── public/                   # 公共资源
├── index.html
├── vite.config.ts           # Vite 配置
├── tsconfig.json            # TypeScript 配置
├── tailwind.config.js       # Tailwind CSS 配置
├── eslint.config.mjs        # ESLint 配置
├── .prettierrc.json         # Prettier 配置
└── package.json

## 🚀 快速开始

### 安装依赖

```bash
npm install
# 或
pnpm install
# 或
yarn install
```

### 启动开发服务器

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 🔧 配置

### 环境变量

创建 `.env.local` 文件：

```bash
# 应用标题
VITE_APP_TITLE=AI Generated App

# API 地址
VITE_APP_BASE_API=/api
VITE_APP_BASE_PYTHON_API=/python-api

# 端口
VITE_APP_PORT=5173
```

## 📝 常用组件

### 3D 场景组件

```vue
<template>
  <scene-3d
    :show-controls="true"
    :show-default="false"
    height="400px"
    @ready="onSceneReady"
  >
    <template #default>
      <!-- 自定义 3D 对象 -->
    </template>
  </scene-3d>
</template>

<script setup>
import Scene3D from '@/components/3d/Scene3D.vue'

function onSceneReady(scene) {
  console.log('3D scene ready', scene)
}
</script>
```

### 图表组件

```vue
<template>
  <chart-card
    :option="chartOption"
    height="400px"
    @click="handleChartClick"
  />
</template>

<script setup>
import ChartCard from '@/components/charts/ChartCard.vue'

const chartOption = {
  xAxis: { type: 'category', data: ['A', 'B', 'C'] },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: [10, 20, 30] }]
}
</script>
```

## 🎨 自定义主题

### 修改主题色

在 `src/assets/styles/variables.scss` 中修改 SCSS 变量：

```scss
$primary-color: #3b82f6;
$success-color: #22c55e;
$warning-color: #f59e0b;
$danger-color: #ef4444;
```

### Tailwind 主题

在 `tailwind.config.js` 中修改：

```js
theme: {
  extend: {
    colors: {
      primary: {
        // 自定义颜色
      }
    }
  }
}
```

## 📚 AI 生成指南

这个脚手架专门为 AI 代码生成设计，具有以下特点：

1. **标准化结构** - AI 熟悉的目录结构和命名规范
2. **类型安全** - 完整的 TypeScript 类型定义
3. **组件化** - 可复用的组件设计
4. **配置集中** - 所有配置都在根目录
5. **注释完整** - 中文注释，方便 AI 理解

### AI 生成建议

当使用 AI 生成代码时，请参考：

- 组件放在 `src/components/` 对应目录
- 页面放在 `src/views/pages/` 目录
- API 调用使用 `src/api/` 中的封装
- 类型定义添加到 `src/types/` 目录
- 状态管理使用 `src/stores/` 中的 Pinia stores

## 📄 License

MIT
