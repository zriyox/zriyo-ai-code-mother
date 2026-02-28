---
name: codemirror
description: "CodeMirror 6 代码编辑器 Skill 文档"
metadata:
  short-description: "CodeMirror 6 代码编辑器 Skill 文档"
---

# CodeMirror 6 代码编辑器 Skill 文档

## 概述

CodeMirror 6 是一个基于模块化架构的代码编辑器，支持语法高亮、自动补全、快捷键等功能。

## 安装

```bash
# 核心包

# 语言支持

# 主题

# 扩展
```

## 基础用法

### 简单编辑器

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { EditorView, basicSetup } from 'codemirror'
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'

const editorRef = ref<HTMLElement>()
let view: EditorView | null = null

onMounted(() => {
  if (editorRef.value) {
    view = new EditorView({
      doc: '// 在这里编写代码\nconsole.log("Hello, World!")',
      extensions: [
        basicSetup,
        javascript(),
        oneDark,
      ],
      parent: editorRef.value,
    })
  }
})
</script>

<template>
  <div ref="editorRef" class="editor-container"></div>
</template>

<style scoped>
.editor-container {
  border: 1px solid #333;
  border-radius: 8px;
  overflow: hidden;
}
</style>
```

### 双向绑定

```vue
<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { EditorView, basicSetup } from 'codemirror'
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'
import { EditorState } from '@codemirror/state'

const code = ref('const greeting = "Hello"\nconsole.log(greeting)')
const editorRef = ref<HTMLElement>()
let view: EditorView | null = null

onMounted(() => {
  if (editorRef.value) {
    view = new EditorView({
      state: EditorState.create({
        doc: code.value,
        extensions: [
          basicSetup,
          javascript(),
          oneDark,
          EditorView.updateListener.of((update) => {
            if (update.docChanged) {
              code.value = update.state.doc.toString()
            }
          }),
        ],
      }),
      parent: editorRef.value,
    })
  }
})

// 监听外部变化
watch(code, (newCode) => {
  if (view && newCode !== view.state.doc.toString()) {
    view.dispatch({
      changes: {
        from: 0,
        to: view.state.doc.length,
        insert: newCode,
      },
    })
  }
})
</script>

<template>
  <div ref="editorRef" class="editor"></div>
</template>
```

## 语言支持

### JavaScript/TypeScript

```typescript
import { javascript } from '@codemirror/lang-javascript'

// JavaScript
javascript()

// TypeScript
javascript({ typescript: true })

// JSX
javascript({ jsx: true })

// TSX
javascript({ typescript: true, jsx: true })
```

### Python

```typescript
import { python } from '@codemirror/lang-python'

extensions: [
  python(),
]
```

### HTML

```typescript
import { html } from '@codemirror/lang-html'

extensions: [
  html({ matchClosingTags: true }),
]
```

### CSS

```typescript
import { css } from '@codemirror/lang-css'

extensions: [
  css(),
]
```

### JSON

```typescript
import { json } from '@codemirror/lang-json'

extensions: [
  json(),
]
```

### Markdown

```typescript
import { markdown } from '@codemirror/lang-markdown'

extensions: [
  markdown({ codeLanguages: (info) => {
    if (info.language === 'javascript') return javascript()
    if (info.language === 'python') return python()
    return null
  }}),
]
```

## 主题

### 内置主题

```typescript
import { oneDark } from '@codemirror/theme-one-dark'
import { eclipse } from '@codemirror/theme-light'

// 暗色主题
extensions: [oneDark]

// 亮色主题
extensions: [eclipse]
```

### 自定义主题

```typescript
import { EditorView } from '@codemirror/view'

const customTheme = EditorView.theme({
  '&': {
    backgroundColor: '#1e1e1e',
    color: '#d4d4d4',
  },
  '.cm-content': {
    fontFamily: 'Fira Code, monospace',
    fontSize: '14px',
    lineHeight: '1.6',
  },
  '.cm-gutters': {
    backgroundColor: '#252526',
    border: 'none',
  },
  '.cm-activeLine': {
    backgroundColor: '#2d2d30',
  },
  '.cm-activeLineGutter': {
    backgroundColor: '#2d2d30',
    color: '#d4d4d4',
  },
  '.cm-selectionBackground': {
    backgroundColor: '#264f78',
  },
  '&.cm-focused .cm-selectionBackground': {
    backgroundColor: '#264f78',
  },
})

extensions: [customTheme]
```

## 扩展功能

### 行号

```typescript
import { lineNumbers, highlightActiveLineGutter } from '@codemirror/view'

extensions: [
  lineNumbers(),
  highlightActiveLineGutter(),
]
```

### 活跃行高亮

```typescript
import { highlightActiveLine } from '@codemirror/view'

extensions: [
  highlightActiveLine(),
]
```

### 代码折叠

```typescript
import { foldGutter } from '@codemirror/language'
import { defaultKeymap, foldKeymap } from '@codemirror/commands'

extensions: [
  foldGutter({
    openText: '▼',
    closedText: '▶',
  }),
]
```

### 搜索替换

```typescript
import { search, highlightSelectionMatches } from '@codemirror/search'

extensions: [
  search({ top: true }),
  highlightSelectionMatches(),
]
```

### 自动补全

```typescript
import { autocompletion } from '@codemirror/autocomplete'

const customCompletions = (context: any) => {
  const word = context.matchBefore(/\w*/)
  if (!word || (word.from === word.to && !context.explicit)) {
    return null
  }

  return {
    from: word.from,
    options: [
      { label: 'console', type: 'keyword' },
      { label: 'log', type: 'function' },
      { label: 'error', type: 'function' },
      { label: 'warn', type: 'function' },
    ],
  }
}

extensions: [
  autocompletion({ override: [customCompletions] }),
]
```

### 括号匹配

```typescript
import { bracketMatching } from '@codemirror/language'

extensions: [
  bracketMatching(),
]
```

### 快捷键

```typescript
import { keymap, defaultKeymap } from '@codemirror/commands'

extensions: [
  keymap.of([
    ...defaultKeymap,
    {
      key: 'Mod-s',
      run: () => {
        console.log('保存')
        return true
      },
    },
  ]),
]
```

### 只读模式

```typescript
import { EditorState } from '@codemirror/state'

extensions: [
  EditorState.readOnly.of(true),
]
```

### 禁用编辑

```typescript
import { EditorView } from '@codemirror/view'

extensions: [
  EditorView.editable.of(false),
]
```

## Vue 3 组件封装

### 基础编辑器组件

```vue
<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount, computed } from 'vue'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { javascript } from '@codemirror/lang-javascript'
import { python } from '@codemirror/lang-python'
import { html } from '@codemirror/lang-html'
import { css } from '@codemirror/lang-css'
import { json } from '@codemirror/lang-json'
import { oneDark } from '@codemirror/theme-one-dark'

interface Props {
  modelValue: string
  language?: 'javascript' | 'typescript' | 'python' | 'html' | 'css' | 'json'
  theme?: 'light' | 'dark'
  readonly?: boolean
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  language: 'javascript',
  theme: 'dark',
  readonly: false,
  height: '400px',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'ready': [view: EditorView]
}>()

const editorRef = ref<HTMLElement>()
let view: EditorView | null = null

const languageExtension = computed(() => {
  switch (props.language) {
    case 'typescript':
      return javascript({ typescript: true })
    case 'python':
      return python()
    case 'html':
      return html()
    case 'css':
      return css()
    case 'json':
      return json()
    default:
      return javascript()
  }
})

const themeExtension = computed(() => {
  return props.theme === 'dark' ? oneDark : []
})

onMounted(() => {
  if (editorRef.value) {
    view = new EditorView({
      state: EditorState.create({
        doc: props.modelValue,
        extensions: [
          basicSetup,
          languageExtension.value,
          themeExtension.value,
          EditorState.readOnly.of(props.readonly),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) {
              emit('update:modelValue', update.state.doc.toString())
            }
          }),
        ],
      }),
      parent: editorRef.value,
    })
    emit('ready', view)
  }
})

watch(() => props.modelValue, (newValue) => {
  if (view && newValue !== view.state.doc.toString()) {
    view.dispatch({
      changes: {
        from: 0,
        to: view.state.doc.length,
        insert: newValue,
      },
    })
  }
})

watch(() => props.language, () => {
  // 重建编辑器以应用新语言
  if (view) {
    const currentValue = view.state.doc.toString()
    view.destroy()

    if (editorRef.value) {
      view = new EditorView({
        state: EditorState.create({
          doc: currentValue,
          extensions: [
            basicSetup,
            languageExtension.value,
            themeExtension.value,
            EditorState.readOnly.of(props.readonly),
            EditorView.updateListener.of((update) => {
              if (update.docChanged) {
                emit('update:modelValue', update.state.doc.toString())
              }
            }),
          ],
        }),
        parent: editorRef.value,
      })
    }
  }
})

onBeforeUnmount(() => {
  view?.destroy()
})
</script>

<template>
  <div
    ref="editorRef"
    class="code-editor"
    :style="{ height }"
  ></div>
</template>

<style scoped>
.code-editor {
  border: 1px solid #333;
  border-radius: 8px;
  overflow: hidden;
}

.code-editor :deep(.cm-editor) {
  height: 100%;
}

.code-editor :deep(.cm-scroller) {
  overflow: auto;
}
</style>
```

### 使用示例

```vue
<script setup lang="ts">
import { ref } from 'vue'
import CodeEditor from '@/components/CodeEditor.vue'

const code = ref(`function greet(name) {
  console.log(\`Hello, \${name}!\`)
}

greet('World')`)
</script>

<template>
  <CodeEditor
    v-model="code"
    language="javascript"
    theme="dark"
    height="500px"
  />
</template>
```

## API 参考

### EditorState

| 选项 | 类型 | 描述 |
|------|------|------|
| doc | string \| Text | 初始内容 |
| selection | EditorSelection | 初始选择 |
| extensions | Extension[] | 扩展列表 |

### EditorView

| 方法 | 描述 |
|------|------|
| `dispatch(transaction)` | 派发事务 |
| `destroy()` | 销毁编辑器 |
| `focus()` | 获取焦点 |
| `state` | 当前状态 |
| `dom` | DOM 元素 |

## 最佳实践

1. **按需引入**：只引入需要的语言包和扩展
2. **资源清理**：组件卸载时调用 `view.destroy()`
3. **性能优化**：大文件使用虚拟滚动
4. **主题切换**：动态切换主题扩展
5. **快捷键**：自定义快捷键提高效率
