---
name: vue-i18n
description: "Vue I18n 国际化 Skill 文档"
metadata:
  short-description: "Vue I18n 国际化 Skill 文档"
---

# Vue I18n 国际化 Skill 文档

## 概述

Vue I18n 是 Vue.js 的国际化插件，支持多语言切换、复数、日期时间格式化等功能。

## 安装

```bash
```

## 基础配置

### 创建 i18n 实例

```typescript
// locales/index.ts
import { createI18n } from 'vue-i18n'

// 导入语言文件
import zhCN from './zh-CN.json'
import enUS from './en-US.json'

const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: localStorage.getItem('locale') || 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

export default i18n
```

### 挂载到 Vue 应用

```typescript
// main.ts
import { createApp } from 'vue'
import App from './App.vue'
import i18n from './locales'

const app = createApp(App)
app.use(i18n)
app.mount('#app')
```

## 语言文件结构

### 嵌套结构

```json
// locales/zh-CN.json
{
  "app": {
    "name": "应用名称",
    "title": "欢迎"
  },
  "menu": {
    "home": "首页",
    "about": "关于",
    "settings": "设置"
  },
  "user": {
    "profile": "个人资料",
    "login": "登录",
    "logout": "退出登录"
  }
}
```

```json
// locales/en-US.json
{
  "app": {
    "name": "App Name",
    "title": "Welcome"
  },
  "menu": {
    "home": "Home",
    "about": "About",
    "settings": "Settings"
  },
  "user": {
    "profile": "Profile",
    "login": "Login",
    "logout": "Logout"
  }
}
```

## Composition API 用法

### useI18n

```vue
<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t, locale, d, n } = useI18n()

// 翻译文本
const title = t('app.title')

// 切换语言
const changeLanguage = (lang: string) => {
  locale.value = lang
  localStorage.setItem('locale', lang)
}

// 日期格式化
const date = d(new Date(), 'short')

// 数字格式化
const amount = n(1234.56, 'currency')
</script>

<template>
  <div>
    <h1>{{ t('app.title') }}</h1>
    <button @click="changeLanguage('zh-CN')">中文</button>
    <button @click="changeLanguage('en-US')">English</button>
  </div>
</template>
```

## 模板中使用

### 基础翻译

```vue
<template>
  <!-- 简单翻译 -->
  <h1>{{ $t('app.name') }}</h1>

  <!-- 嵌套翻译 -->
  <span>{{ $t('menu.home') }}</span>

  <!-- 带参数翻译 -->
  <p>{{ $t('user.greeting', { name: '张三' }) }}</p>
</template>
```

### 复数处理

```json
// locales/zh-CN.json
{
  "apple": "没有苹果 | 一个苹果 | {n} 个苹果"
}
```

```vue
<template>
  <p>{{ $tn('apple', 0) }}</p>  <!-- 没有苹果 -->
  <p>{{ $tn('apple', 1) }}</p>  <!-- 一个苹果 -->
  <p>{{ $tn('apple', 5) }}</p>  <!-- 5 个苹果 -->
</template>
```

### 日期时间格式化

```typescript
// locales/index.ts
const i18n = createI18n({
  datetimeFormats: {
    'zh-CN': {
      short: {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      },
      long: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        weekday: 'long',
        hour: 'numeric',
        minute: 'numeric',
      },
    },
    'en-US': {
      short: {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      },
    },
  },
})
```

```vue
<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { d } = useI18n()
const now = new Date()
</script>

<template>
  <p>{{ d(now, 'short') }}</p>
  <p>{{ d(now, 'long') }}</p>
</template>
```

### 数字格式化

```typescript
const i18n = createI18n({
  numberFormats: {
    'zh-CN': {
      currency: {
        style: 'currency',
        currency: 'CNY',
      },
      decimal: {
        style: 'decimal',
        minimumFractionDigits: 2,
      },
      percent: {
        style: 'percent',
      },
    },
    'en-US': {
      currency: {
        style: 'currency',
        currency: 'USD',
      },
    },
  },
})
```

```vue
<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { n } = useI18n()
</script>

<template>
  <p>{{ n(1234.56, 'currency') }}</p>  <!-- ¥1,234.56 -->
  <p>{{ n(0.85, 'percent') }}</p>       <!-- 85% -->
</template>
```

## 高级用法

### 带参数的翻译

```json
{
  "greeting": "你好，{name}！",
  "message": "你有 {count} 条新消息"
}
```

```vue
<template>
  <p>{{ $t('greeting', { name: '张三' }) }}</p>
  <p>{{ $t('message', { count: 5 }) }}</p>
</template>
```

### 链式翻译

```json
{
  "message": {
    "hello": "你好",
    "world": "世界"
  }
}
```

```vue
<template>
  <p>{{ $t('message.hello') }} {{ $t('message.world') }}</p>
</template>
```

### 缺失翻译处理

```typescript
const i18n = createI18n({
  missing: (locale, key) => {
    console.warn(`[i18n] Missing translation: ${key} for locale: ${locale}`)
    return key
  },
  fallbackLocale: 'zh-CN',
  silentFallbackWarn: true,
  silentTranslationWarn: true,
})
```

### 自定义修饰符

```typescript
const i18n = createI18n({
  modifiers: {
    snakeCase: (str: string) => str.split(' ').join('_').toLowerCase(),
    upperCase: (str: string) => str.toUpperCase(),
  },
})
```

```vue
<template>
  <p>{{ $t('menu.home').snakeCase }}</p>  <!-- menu_home -->
  <p>{{ $t('menu.home').upperCase }}</p>  <!-- MENU HOME -->
</template>
```

## 语言切换组件

### 语言选择器

```vue
<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { locale, availableLocales } = useI18n()

const languages = [
  { code: 'zh-CN', name: '简体中文', flag: '🇨🇳' },
  { code: 'en-US', name: 'English', flag: '🇺🇸' },
]

function changeLanguage(code: string) {
  locale.value = code
  localStorage.setItem('locale', code)
  location.reload()
}
</script>

<template>
  <el-dropdown>
    <span class="language-selector">
      {{ languages.find(l => l.code === locale)?.flag }}
      {{ languages.find(l => l.code === locale)?.name }}
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="lang in languages"
          :key="lang.code"
          @click="changeLanguage(lang.code)"
        >
          {{ lang.flag }} {{ lang.name }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>
```

## 懒加载翻译

```typescript
// locales/index.ts
import { createI18n } from 'vue-i18n'

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': await loadLocaleMessages('zh-CN'),
  },
})

export async function loadLocaleMessages(locale: string) {
  const messages = await import(`./${locale}.json`)
  return messages.default
}

export async function setI18nLanguage(locale: string) {
  if (!i18n.global.availableLocales.includes(locale)) {
    const messages = await loadLocaleMessages(locale)
    i18n.global.setLocaleMessage(locale, messages)
  }

  i18n.global.locale.value = locale
  document.querySelector('html')?.setAttribute('lang', locale)
}

export default i18n
```

## 路由集成

### 路由元信息翻译

```typescript
// router/index.ts
const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/Home.vue'),
    meta: {
      title: 'menu.home',
    },
  },
]

router.beforeEach((to, from, next) => {
  const { t } = i18n.global
  document.title = `${t(to.meta.title as string)} - ${t('app.name')}`
  next()
})
```

## TypeScript 支持

### 类型定义

```typescript
// locales/index.ts
import { createI18n } from 'vue-i18n'

// 定义翻译资源类型
type MessageSchema = typeof import('./zh-CN.json')

const i18n = createI18n<[MessageSchema], 'zh-CN' | 'en-US'>({
  legacy: false,
  locale: 'zh-CN',
  messages: {
    'zh-CN': () => import('./zh-CN.json'),
    'en-US': () => import('./en-US.json'),
  },
})

export default i18n
```

### 使用类型

```vue
<script setup lang="ts">
import type { MessageSchema } from '@/locales'
import { useI18n } from 'vue-i18n'

type MessageSchemaType = {
  message: keyof MessageSchema
}

const { t } = useI18n<MessageSchemaType>()

const title = t('app.name') // 有类型提示
</script>
```

## 最佳实践

1. **结构化组织**：按功能模块划分翻译文件
2. **key 命名**：使用点号分隔的层级结构
3. **本地存储**：保存用户选择的语言偏好
4. **懒加载**：大型应用按需加载语言包
5. **类型安全**：使用 TypeScript 增强类型检查
6. **自动化**：使用脚本提取翻译文本

## 常用 API

| 函数 | 描述 |
|------|------|
| `$t(key)` | 翻译文本 |
| `$tn(key, count)` | 复数翻译 |
| `$tc(key, choice)` | 选择翻译 |
| `$d(value, format)` | 日期格式化 |
| `$n(value, format)` | 数字格式化 |
| `locale` | 当前语言 |
| `availableLocales` | 可用语言列表 |
| `setLocaleMessage(locale, message)` | 设置翻译消息 |
| `mergeLocaleMessage(locale, message)` | 合并翻译消息 |
