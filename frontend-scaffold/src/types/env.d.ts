/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'element-plus/dist/locale/zh-cn.mjs' {
  const locale: any
  export default locale
}

declare module 'element-plus/dist/locale/en.mjs' {
  const locale: any
  export default locale
}

declare module 'element-plus/theme-chalk/dark/css-vars.css' {
  const css: any
  export default css
}

// SVG 图标类型
declare module '*.svg' {
  const content: any
  export default content
}

// Markdown 样式
declare module 'highlight.js/lib/common' {
  const hljs: any
  export default hljs
}
