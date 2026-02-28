---
name: highlight-js
description: "Highlight.js 代码高亮 Skill 文档"
metadata:
  short-description: "Highlight.js 代码高亮 Skill 文档"
---

# Highlight.js 代码高亮 Skill 文档

## 概述

Highlight.js 是一个支持 190+ 语言和 230+ 样式主题的语法高亮库，自动检测语言，无依赖。

## 安装

```bash
```

## 基础用法

### 简单高亮

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

const codeRef = ref<HTMLElement>()

onMounted(() => {
  if (codeRef.value) {
    hljs.highlightElement(codeRef.value)
  }
})
</script>

<template>
  <pre><code ref="codeRef" class="language-javascript">
function hello() {
  console.log('Hello, World!')
}
  </code></pre>
</template>
```

### 自动高亮所有代码块

```typescript
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

// 页面加载后自动高亮所有代码块
hljs.highlightAll()
```

### 高亮特定语言

```vue
<script setup lang="ts">
import { computed } from 'vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

const code = `function hello() {
  console.log('Hello, World!')
}`

const highlighted = computed(() => {
  return hljs.highlight(code, { language: 'javascript' }).value
})
</script>

<template>
  <pre><code class="language-javascript" v-html="highlighted"></code></pre>
</template>
```

## 支持的语言

### 常用语言

```typescript
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import cpp from 'highlight.js/lib/languages/cpp'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import php from 'highlight.js/lib/languages/php'
import ruby from 'highlight.js/lib/languages/ruby'
import html from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import scss from 'highlight.js/lib/languages/scss'
import json from 'highlight.js/lib/languages/json'
import yaml from 'highlight.js/lib/languages/yaml'
import markdown from 'highlight.js/lib/languages/markdown'
import bash from 'highlight.js/lib/languages/bash'
import sql from 'highlight.js/lib/languages/sql'
import docker from 'highlight.js/lib/languages/docker'
```

### 注册语言

```typescript
import hljs from 'highlight.js/core'
import javascript from 'highlight.js/lib/languages/javascript'
import python from 'highlight.js/lib/languages/python'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('python', python)
```

## 主题样式

### 引入主题

```typescript
// 亮色主题
import 'highlight.js/styles/github.css'
import 'highlight.js/styles/atom-one-light.css'
import 'highlight.js/styles/vs.css'

// 暗色主题
import 'highlight.js/styles/github-dark.css'
import 'highlight.js/styles/atom-one-dark.css'
import 'highlight.js/styles/vs2015.css'
import 'highlight.js/styles/monokai.css'
import 'highlight.js/styles/dracula.css'
import 'highlight.js/styles/nord.css'
```

### 动态切换主题

```vue
<script setup lang="ts">
import { ref, watch } from 'vue'
import hljs from 'highlight.js'

const theme = ref<'light' | 'dark'>('dark')

watch(theme, (newTheme) => {
  // 移除旧主题
  document.querySelector(`link[href^='highlight.js']`)?.remove()

  // 添加新主题
  const link = document.createElement('link')
  link.rel = 'stylesheet'
  link.href = `https://cdn.jsdelivr.net/gh/highlightjs/cdn-release@11.9.0/build/styles/${
    newTheme === 'dark' ? 'github-dark' : 'github'
  }.min.css`
  document.head.appendChild(link)
})
</script>
```

## Vue 3 组件封装

### 代码高亮组件

```vue
<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

interface Props {
  code: string
  language?: string
  theme?: 'light' | 'dark'
  autodetect?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  language: 'javascript',
  theme: 'dark',
  autodetect: true,
})

const codeRef = ref<HTMLElement>()

const highlightCode = () => {
  if (codeRef.value) {
    if (props.autodetect) {
      hljs.highlightElement(codeRef.value)
    } else {
      codeRef.value.innerHTML = hljs.highlight(
        props.code,
        { language: props.language }
      ).value
    }
  }
}

onMounted(highlightCode)

watch(() => props.code, highlightCode)
</script>

<template>
  <pre class="hljs-wrapper">
    <code
      ref="codeRef"
      :class="`language-${language}`"
    >{{ code }}</code>
  </pre>
</template>

<style scoped>
.hljs-wrapper {
  position: relative;
  padding: 16px;
  background: #1e1e1e;
  border-radius: 8px;
  overflow-x: auto;
}

.hljs-wrapper code {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
}
</style>
```

### 带行号的代码高亮

```vue
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

interface Props {
  code: string
  language?: string
  showLineNumbers?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  language: 'javascript',
  showLineNumbers: true,
})

const highlightedCode = ref('')

onMounted(() => {
  const result = hljs.highlight(props.code, { language: props.language })
  highlightedCode.value = result.value

  if (props.showLineNumbers) {
    const lines = props.code.split('\n')
    highlightedCode.value = lines
      .map((_, i) => `<div class="line"><span class="line-number">${i + 1}</span>${result.value.split('\n')[i] || ''}</div>`)
      .join('\n')
  }
})
</script>

<template>
  <pre class="code-block">
    <code :class="`language-${language}`" v-html="highlightedCode"></code>
  </pre>
</template>

<style scoped>
.code-block {
  padding: 16px;
  background: #1e1e1e;
  border-radius: 8px;
  overflow-x: auto;
}

.line {
  display: flex;
  line-height: 1.6;
}

.line-number {
  display: inline-block;
  width: 40px;
  padding-right: 12px;
  color: #6e7681;
  text-align: right;
  user-select: none;
}
</style>
```

### 复制功能的代码块

```vue
<script setup lang="ts">
import { ref } from 'vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

interface Props {
  code: string
  language?: string
}

const props = withDefaults(defineProps<Props>(), {
  language: 'javascript',
})

const copied = ref(false)
const highlighted = hljs.highlight(props.code, { language: props.language }).value

async function copyCode() {
  await navigator.clipboard.writeText(props.code)
  copied.value = true
  setTimeout(() => {
    copied.value = false
  }, 2000)
}
</script>

<template>
  <div class="code-block">
    <div class="code-header">
      <span class="language-badge">{{ language }}</span>
      <button class="copy-btn" @click="copyCode">
        {{ copied ? '已复制!' : '复制' }}
      </button>
    </div>
    <pre><code :class="`language-${language}`" v-html="highlighted"></code></pre>
  </div>
</template>

<style scoped>
.code-block {
  margin: 16px 0;
  border: 1px solid #30363d;
  border-radius: 8px;
  overflow: hidden;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #161b22;
  border-bottom: 1px solid #30363d;
}

.language-badge {
  font-size: 12px;
  color: #8b949e;
  text-transform: uppercase;
}

.copy-btn {
  padding: 4px 12px;
  font-size: 12px;
  color: #c9d1d9;
  background: #21262d;
  border: 1px solid #30363d;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.copy-btn:hover {
  background: #30363d;
}
</style>
```

## 常用主题速查

| 主题名 | 描述 | 适用场景 |
|--------|------|----------|
| github | GitHub 亮色 | 明亮环境 |
| github-dark | GitHub 暗色 | 默认推荐 |
| atom-one-light | Atom One 亮色 | 清爽风格 |
| atom-one-dark | Atom One 暗色 | 深色主题 |
| monokai | Monokai | 经典暗色 |
| dracula | Dracula | 护眼暗色 |
| nord | Nord | 北欧风格 |
| vs | VS Code 亮色 | IDE 风格 |
| vs2015 | VS Code 2015 | IDE 暗色 |
| solarized-light | Solarized 亮色 | 低对比度 |
| solarized-dark | Solarized 暗色 | 低对比度 |

## 支持的语言列表

### 编程语言

- JavaScript/TypeScript
- Python
- Java
- C/C++
- C#
- Go
- Rust
- PHP
- Ruby
- Swift
- Kotlin
- Dart
- Scala
- Haskell

### 标记语言

- HTML/XML
- CSS/SCSS/SASS/Less
- Markdown
- JSON
- YAML
- TOML

### 脚本语言

- Bash/Shell
- PowerShell
- Lua
- Perl
- R

### 数据库

- SQL
- GraphQL

### 配置文件

- Dockerfile
- Nginx
- Apache
- Vim

## 最佳实践

1. **按需加载语言**：只引入需要的语言以减小体积
2. **选择合适主题**：根据网站风格选择主题
3. **自动检测语言**：不确定语言时使用自动检测
4. **缓存高亮结果**：大代码块建议缓存高亮结果
5. **安全性**：只高亮可信的代码，防止 XSS 攻击
