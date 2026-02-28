/**
 * 路由配置
 */

import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAppStore } from '@/stores/app'

// 布局组件
const Layout = () => import('@/layouts/DefaultLayout.vue')
const BlankLayout = () => import('@/layouts/BlankLayout.vue')

// 页面组件
const Dashboard = () => import('@/views/pages/Dashboard.vue')
const Projects = () => import('@/views/pages/Projects.vue')
const ProjectDetail = () => import('@/views/pages/ProjectDetail.vue')
const Settings = () => import('@/views/pages/Settings.vue')
const Login = () => import('@/views/pages/Login.vue')
const NotFound = () => import('@/views/pages/NotFound.vue')

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'Login',
        component: Login,
        meta: {
          title: '登录',
          hidden: true,
        },
      },
    ],
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: {
          title: '工作台',
          icon: 'Dashboard',
          affix: true,
        },
      },
      {
        path: 'projects',
        name: 'Projects',
        component: Projects,
        meta: {
          title: '项目管理',
          icon: 'FolderOpened',
        },
      },
      {
        path: 'projects/:id',
        name: 'ProjectDetail',
        component: ProjectDetail,
        meta: {
          title: '项目详情',
          icon: 'Document',
          hidden: true,
        },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: Settings,
        meta: {
          title: '设置',
          icon: 'Setting',
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    component: BlankLayout,
    children: [
      {
        path: '',
        name: 'NotFound',
        component: NotFound,
        meta: {
          title: '404',
          hidden: true,
        },
      },
    ],
  },
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_APP_BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  const appStore = useAppStore()

  // 设置页面加载进度
  window.scrollTo(0, 0)

  // 检查登录状态
  const token = localStorage.getItem('access_token')
  if (token) {
    if (to.name === 'Login') {
      next({ name: 'Dashboard' })
    } else {
      next()
    }
  } else {
    if (to.name !== 'Login') {
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } else {
      next()
    }
  }
})

// 全局后置钩子
router.afterEach((to) => {
  // 可以在这里做一些统计或日志记录
})

export default router
