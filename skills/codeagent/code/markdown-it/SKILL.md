---
name: markdown-it
description: "Markdown-it Markdown 解析 Skill 文档"
metadata:
  short-description: "Markdown-it Markdown 解析 Skill 文档"
---

# Markdown-it Markdown 解析 Skill 文档

## 概述

Markdown-it 是一个功能强大的 Markdown 解析器，支持完整的 CommonMark 规范和许多插件扩展。

## 安装

```bash
```

## 基础用法

```typescript
import MarkdownIt from 'markdown-it'

const md = MarkdownIt()

// 解析 Markdown
const result = md.render('# Hello World')
console.log(result) // <h1>Hello World</h1>
```

## 常用插件

### 容器插件

```typescript
import MarkdownIt from 'markdown-it'
import container from 'markdown-it-container'

const md = MarkdownIt()
md.use(container, 'div')

// 渲染为容器
// :::
// content
// :::
```

### 高亮插件

```typescript
import hljs from 'highlight.js'

md.use(highlight, {
  highlight: (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (e) {}
    }
    return ''
  },
})
```

### 任务列表插件

```typescript
import taskLists from 'markdown-it-task-lists'

md.use(taskLists, {
  enabled: true,
  label: true,
  labelAfter: 'number.',
})
```

### 表情插件

```typescript
import emoji from 'markdown-it-emoji'

md.use(emoji, {
  shortcuts: {
    ':thumbs_up:': '👍'
  }
})
```

## Vue 3 组件封装

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { container } from 'markdown-it-container'
import taskLists from 'markdown-it-task-lists'
import emoji from 'markdown-it-emoji'

interface Props {
  content: string
  theme?: 'light' | 'dark' | 'github'
}

const props = withDefaults(defineProps<Props>(), {
  theme: 'light',
})

const md = MarkdownIt()
  .use(container, 'div')
  .use(hljs)
  .use(taskLists)
  .use(emoji)

// 渲染后的 HTML
const htmlContent = computed(() => md.render(props.content))
</script>

<template>
  <div class="markdown-body" :class="`markdown-${theme}`" v-html="htmlContent""></div>
</template>

<style scoped>
.markdown-body {
  line-height: 1.6;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
}

.markdown-body code {
  padding: 2px 6px;
  background-color: rgba(175, 184, 193, 0.2);
  border-radius: 4px;
}

.markdown-body pre {
  padding: 16px;
  background-color: #1e1e1e;
  border-radius: 6px;
  overflow-x: auto;
}
</style>
```

## 安全考虑

### XSS 防护

```typescript
import DOMPurify from 'dompurify'

const md = MarkdownIt()

// 渲染前净化
function safeRender(markdown: string): string {
  const dirty = md.render(markdown)
  const clean = DOMPurify.sanitize(dirty)
  return clean
}
```

## 配置选项

```typescript
const md = MarkdownIt({
  html: true,        // 启用 HTML 标签
  linkify: true,      // 自动转换 URL 为链接
  typographer: true,  // 智能引号和排版
  breaks: true,       // 转换换行为
  langPrefix: '',    // 语言前缀
})
```
