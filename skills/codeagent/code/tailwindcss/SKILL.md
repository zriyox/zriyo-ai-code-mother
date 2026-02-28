---
name: tailwindcss
description: "Tailwind CSS 使用 Skill 文档"
metadata:
  short-description: "Tailwind CSS 使用 Skill 文档"
---

# Tailwind CSS 使用 Skill 文档

> 官方文档: https://tailwindcss.com/docs

## 概述

Tailwind CSS 是一个功能类优先的 CSS 框架，提供了高度可定制的低级实用工具类，无需离开 HTML 即可快速构建现代网站。

## 安装与配置

### 安装

```bash
```

### 初始化配置

```bash
npx tailwindcss init -p
```

### tailwind.config.js

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  // 扫描文件路径
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  // 主题扩展
  theme: {
    extend: {
      colors: {
        primary: '#3b82f6',
        secondary: '#22c55e',
      },
      spacing: {
        '128': '32rem',
      },
    },
  },
  // 插件
  plugins: [],
}
```

### postcss.config.js

```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

## 基础用法

### 添加指令

```css
/* main.css */
@tailwind base;
@tailwind components;
@tailwind utilities;
```

## 布局

### 容器

```html
<!-- Flex 容器 -->
<div class="flex flex-row flex-wrap justify-center items-center">
  <!-- 内容 -->
</div>

<!-- Grid 容器 -->
<div class="grid grid-cols-3 gap-4">
  <!-- 网格项 -->
</div>

<!-- 宽度 -->
<div class="w-full w-1/2 w-64 max-w-md"></div>
```

### 内边距与外边距

```html
<!-- Padding -->
<div class="p-4 px-2 py-1">
  <!-- p-4: 全方向 1rem -->
  <!-- px-2: 水平 0.5rem -->
  <!-- py-1: 垂直 0.25rem -->
</div>

<!-- Margin -->
<div class="m-4 mx-auto my-2">
  <!-- m-4: 全方向 1rem -->
  <!-- mx-auto: 水平居中 -->
  <!-- my-2: 垂直 0.5rem -->
</div>

<!-- Space Between (Flex 子元素间距) -->
<div class="space-x-4 space-y-2">
  <!-- 子元素之间水平/垂直间距 -->
</div>
```

### 显示

```html
<!-- 块级/行内/隐藏 -->
<div class="block inline hidden"></div>

<!-- Flexbox -->
<div class="flex flex-col flex-row flex-wrap justify-between items-center">
  <!-- justify: start, end, center, between, around, evenly -->
  <!-- items: start, end, center, stretch, baseline -->
</div>

<!-- Grid -->
<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
  <!-- cols: 1-12, none -->
  <!-- gap: 0-12 -->
</div>
```

## 排版

### 文本样式

```html
<!-- 字体大小 -->
<p class="text-xs text-sm text-base text-lg text-xl text-3xl">
  <!-- xs: 0.75rem, sm: 0.875rem, base: 1rem -->
  <!-- lg: 1.125rem, xl: 1.25rem, 3xl: 1.875rem -->
</p>

<!-- 字重 -->
<p class="font-light font-normal font-medium font-semibold font-bold">
  <!-- light: 300, normal: 400, medium: 500 -->
  <!-- semibold: 600, bold: 700 -->
</p>

<!-- 文本对齐 -->
<p class="text-left text-center text-right text-justify">
  <!-- left, center, right, justify -->
</p>

<!-- 文本颜色 -->
<p class="text-gray-900 text-blue-600 text-white">
  <!-- gray: 50-900, blue: 50-900, white, black -->
</p>
```

### 背景与边框

```html
<!-- 背景颜色 -->
<div class="bg-white bg-gray-100 bg-blue-500 bg-transparent">
  <!-- 命名颜色 + 透明度 -->
</div>

<div class="bg-opacity-50 bg-gradient-to-r from-blue-500 to-purple-500">
  <!-- 渐变背景 -->
</div>

<!-- 边框 -->
<div class="border border-2 border-dashed border-blue-500 rounded-lg">
  <!-- border-{width}, rounded-{radius} -->
</div>
```

## 响应式设计

### 断点前缀

```html
<!-- 默认（移动端优先） -->
<div class="w-full md:w-1/2 lg:w-1/3 xl:w-1/4">
  <!-- sm: 640px+, md: 768px+, lg: 1024px+, xl: 1280px+, 2xl: 1536px+ -->
</div>

<!-- 条件渲染 -->
<div class="hidden md:block">
  <!-- 移动端隐藏，中等及以上显示 -->
</div>
```

### Hover、Focus 等状态

```html
<!-- Hover -->
<button class="hover:bg-blue-600 hover:scale-105">
  <!-- hover: 鼠标悬停时应用 -->
</button>

<!-- Focus -->
<input class="focus:ring-2 focus:ring-blue-500" />

<!-- Active -->
<button class="active:bg-blue-700">
  <!-- active: 点击按下时 -->
</button>
```

## 伪类

### first、last 等

```html
<ul>
  <li class="first:text-red-500">第一项</li>
  <li class="last:text-blue-500">最后一项</li>
  <li class="even:bg-gray-100">偶数项</li>
  <li class="odd:bg-gray-50">奇数项</li>
</ul>
```

## 过渡与动画

### Transition

```html
<div class="transition-all duration-300 ease-in-out hover:scale-105">
  <!-- all, colors, opacity, shadow, transform -->
  <!-- duration: 75, 100, 150, 200, 300, 500, 700, 1000 -->
  <!-- ease: linear, in, out, in-out -->
</div>
```

### Transform

```html
<div class="scale-105 rotate-12 translate-x-2">
  <!-- scale-{percentage}, rotate-{deg} -->
  <!-- translate-{x|y}, skew-{x|y} -->
</div>
```

### Animation

```html
<!-- 使用 animate-pulse、animate-bounce 等内置动画 -->
<div class="animate-pulse animate-bounce animate-spin">
  <!-- pulse, bounce, spin, ping -->
</div>

<!-- 自定义动画 -->
<div class="animate-custom">
  <!-- 在 tailwind.config.js 中定义 -->
</div>
```

## 交互

### 光标与指针

```html
<button class="cursor-pointer cursor-not-allowed">
  <!-- pointer, not-allowed, default -->
</button>
```

### 用户选择

```html
<div class="select-none select-all select-text">
  <!-- none, all, text -->
</div>
```

## Flexbox 实用类

```html
<!-- Flex 方向 -->
<div class="flex flex-row flex-col flex-row-reverse flex-col-reverse">
  <!-- row, col, row-reverse, col-reverse -->
</div>

<!-- Flex 换行 -->
<div class="flex-wrap flex-nowrap">
  <!-- wrap, nowrap -->
</div>

<!-- Justify Content -->
<div class="justify-start justify-end justify-center justify-between justify-around justify-evenly">
  <!-- start, end, center, between, around, evenly -->
</div>

<!-- Align Items -->
<div class="items-start items-end items-center items-stretch items-baseline">
  <!-- start, end, center, stretch, baseline -->
</div>

<!-- Flex Grow/Shrink -->
<div class="flex-grow flex-shrink flex-1">
  <!-- grow, shrink, flex-1: grow: 1, shrink: 1 -->
</div>
```

## Grid 实用类

```html
<!-- Grid 列 -->
<div class="grid grid-cols-1 grid-cols-2 grid-cols-3 grid-cols-4 grid-cols-6 grid-cols-12">
  <!-- 1-12 -->
</div>

<!-- Grid 行 -->
<div class="grid-rows-1 grid-rows-2 grid-rows-3 grid-rows-4 grid-rows-6">
  <!-- 1-6 -->
</div>

<!-- Grid 间隙 -->
<div class="gap-0 gap-1 gap-2 gap-4 gap-6 gap-8">
  <!-- 0-8 (以 0.25rem 递增) -->
</div>

<!-- Grid 跨度 -->
<div class="col-span-1 col-span-2 col-span-full">
  <!-- 跨列数 -->
</div>

<div class="row-span-1 row-span-2">
  <!-- 跨行数 -->
</div>

<!-- Grid 自动 -->
<div class="col-auto row-auto">
  <!-- 自动大小 -->
</div>

<!-- Grid 开始/结束 -->
<div class="col-start-1 col-end-3">
  <!-- 指定网格线位置 -->
</div>
```

## 尺寸

### 宽度与高度

```html
<div class="w-full w-1/2 w-screen w-min w-max">
  <!-- full, 1/2, screen, min, max -->
  <!-- 固定尺寸: w-0, w-px, w-0.5, w-1, w-2, w-3...w-96 -->
</div>

<div class="h-full h-screen h-1/2">
  <!-- full, screen, 1/2 -->
</div>

<!-- 最大/最小尺寸 -->
<div class="max-w-md max-w-lg max-w-xl max-w-2xl min-w-0 min-h-screen">
  <!-- max-w: {xs, sm, md, lg, xl, 2xl, 4xl, 5xl, 6xl, 7xl} -->
  <!-- min-h: {0, full, screen} -->
</div>
```

## 颜色

### 文本颜色

```html
<p class="text-gray-900 text-blue-600 text-white text-opacity-50">
  <!-- 命名颜色 + 等级 -->
  <!-- opacity-{25,50,75,100} -->
</p>
```

### 背景颜色

```html
<div class="bg-white bg-gray-100 bg-gradient-to-br from-blue-400 to-purple-500">
  <!-- bg-{color}-{100-900} -->
  <!-- gradient-to-{direction} from-{color} to-{color} -->
</div>
```

## 边框与圆角

### 边框

```html
<div class="border border-2 border-dotted border-blue-500">
  <!-- border-{side}-{width} -->
  <!-- divide-{side}-{width}: 子元素之间分隔线 -->
</div>
```

### 圆角

```html
<div class="rounded rounded-sm rounded-md rounded-lg rounded-xl rounded-2xl rounded-3xl rounded-full">
  <!-- none, sm, md, lg, xl, 2xl, 3xl, full -->
</div>

## 阴影

```html
<div class="shadow shadow-md shadow-lg shadow-xl shadow-2xl">
  <!-- sm, md, lg, xl, 2xl, inner, none -->
</div>

<div class="shadow-blue-500/50">
  <!-- 阴影颜色 -->
</div>
```

## 透明度

```html
<div class="opacity-0 opacity-50 opacity-100">
  <!-- 0-100 (步长为 100) -->
</div>
```

## 定位

```html
<div class="static fixed absolute relative sticky">
  <!-- static: 默认定位 -->
  <!-- fixed: 固定定位 -->
  <!-- absolute: 绝对定位 -->
  <!-- relative: 相对定位 -->
  <!-- sticky: 粘性定位 -->
</div>

<div class="inset-0 inset-x-0 inset-y-0 top-0 bottom-0 left-0 right-0">
  <!-- inset: 全方向 0 -->
  <!-- {x|y}-{position} -->
</div>

<!-- z-index -->
<div class="z-0 z-10 z-20 z-30 z-40 z-50">
  <!-- 0-50 (auto) -->
</div>
```

## 伪元素

```html
<!-- Before/After -->
<div class="before:content-['*'] after:content-['']">
  <!-- before:, after: -->
  <!-- content-[string] -->
</div>
```

## 自定义配置

### 扩展颜色

```javascript
theme: {
  extend: {
    colors: {
      brand: {
        50: '#f0f9ff',
        100: '#e0f2fe',
        500: '#0ea5e9',
        900: '#0c4a6e',
      },
    },
  },
}
```

### 扩展间距

```javascript
theme: {
  extend: {
    spacing: {
      '128': '32rem',
      '144': '36rem',
    },
  },
}
```

### 扩展字体

```javascript
theme: {
  extend: {
    fontFamily: {
      sans: ['Inter', 'sans-serif'],
      mono: ['Fira Code', 'monospace'],
    },
  },
}
```

### 扩展断点

```javascript
theme: {
  extend: {
    screens: {
      'xs': '480px',
      '3xl': '1600px',
    },
  },
}
```

## 使用 @apply 指令

```css
/* 定义组件样式 */
.btn {
  @apply px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600;
}

/* 组合样式 */
.card {
  @apply bg-white rounded-lg shadow-md p-6;
}

/* 响应式 */
.container {
  @apply max-w-7xl mx-auto px-4 sm:px-6 lg:px-8;
}
```

## 最佳实践

1. **移动端优先**：默认使用移动端样式，使用断点添加桌面端样式
2. **工具类组合**：将常用样式组合抽象为组件类
3. **避免 !important**：优先考虑类优先级而非使用 !important
4. **清除默认样式**：使用 @layer base 自定义基础样式
5. **性能优化**：使用 purge 选项移除未使用的样式

## 与 Vue 3 集成

```vue
<template>
  <!-- 响应式卡片 -->
  <div class="max-w-md mx-auto bg-white rounded-xl shadow-md overflow-hidden md:max-w-2xl">
    <div class="md:flex">
      <div class="md:shrink-0">
        <img class="h-48 w-full object-cover md:h-full md:w-48" src="/img.jpg" alt="">
      </div>
      <div class="p-8">
        <div class="uppercase tracking-wide text-sm text-indigo-500 font-semibold">
          案例
        </div>
        <p class="mt-2 text-gray-900">这是一段示例文本</p>
      </div>
    </div>
  </div>
</template>
```
