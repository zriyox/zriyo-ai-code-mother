/**
 * 应用入口
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import ElementPlusLocaleZhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import App from './App.vue'
import router from './router'
import { setupDirectives } from './directives'

// 导入全局样式
import './assets/styles/index.scss'

// 创建应用实例
const app = createApp(App)

// 创建 Pinia
const pinia = createPinia()
app.use(pinia)

// 注册路由
app.use(router)

// 注册 Element Plus
app.use(ElementPlus, {
  locale: ElementPlusLocaleZhCn,
  size: 'default',
  zIndex: 3000,
})

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册自定义指令
setupDirectives(app)

// 挂载应用
app.mount('#app')

export default app
