/**
 * 用户状态管理
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types'

const TOKEN_KEY = 'access_token'
const USER_KEY = 'user_info'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  // 计算属性
  const isLogin = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const nickname = computed(() => userInfo.value?.nickname || '')
  const avatar = computed(() => userInfo.value?.avatar || '')
  const roles = computed(() => userInfo.value?.roles || [])
  const permissions = computed(() => userInfo.value?.permissions || [])

  // 初始化
  function init() {
    const savedToken = localStorage.getItem(TOKEN_KEY)
    const savedUser = localStorage.getItem(USER_KEY)

    if (savedToken) {
      token.value = savedToken
    }
    if (savedUser) {
      try {
        userInfo.value = JSON.parse(savedUser)
      } catch (e) {
        console.error('Failed to parse saved user info:', e)
      }
    }
  }

  // 设置 token
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem(TOKEN_KEY, newToken)
  }

  // 设置用户信息
  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem(USER_KEY, JSON.stringify(info))
  }

  // 登录
  async function login(credentials: { username: string; password: string }) {
    // TODO: 调用登录 API
    // const data = await loginApi(credentials)
    // setToken(data.token)
    // setUserInfo(data.user)

    // 模拟登录
    return new Promise((resolve) => {
      setTimeout(() => {
        const mockToken = 'mock-token-' + Date.now()
        const mockUser: UserInfo = {
          id: 1,
          username: credentials.username,
          nickname: '测试用户',
          avatar: '',
          roles: ['admin'],
          permissions: ['*'],
        }
        setToken(mockToken)
        setUserInfo(mockUser)
        resolve(mockUser)
      }, 300)
    })
  }

  // 登出
  async function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  // 更新用户信息
  function updateUserInfo(info: Partial<UserInfo>) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...info }
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo.value))
    }
  }

  // 检查权限
  function hasPermission(permission: string): boolean {
    return permissions.value.includes('*') || permissions.value.includes(permission)
  }

  // 检查角色
  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  return {
    // 状态
    token,
    userInfo,
    // 计算属性
    isLogin,
    username,
    nickname,
    avatar,
    roles,
    permissions,
    // 方法
    init,
    setToken,
    setUserInfo,
    login,
    logout,
    updateUserInfo,
    hasPermission,
    hasRole,
  }
})
