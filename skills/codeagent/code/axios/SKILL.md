---
name: axios
description: "Axios HTTP 请求 Skill 文档"
metadata:
  short-description: "Axios HTTP 请求 Skill 文档"
---

# Axios HTTP 请求 Skill 文档

> 官方文档: https://axios-http.com/docs/intro

## 概述

Axios 是一个基于 Promise 的 HTTP 客户端，用于浏览器和 Node.js 环境中发送异步 HTTP 请求。

## 安装

```bash
```

## 基础用法

### GET 请求

```typescript
import axios from 'axios'

axios.get('/user?id=12345')
  .then(response => {
    console.log(response.data)
  })
  .catch(error => {
    console.error(error)
  })

// 使用配置对象
axios.get('/user', { params: { id: 12345 } })

// 响应数据结构
interface AxiosResponse<T> {
  data: T
  status: number
  statusText: string
  headers: any
  config: any
}
```

### POST 请求

```typescript
axios.post('/user', {
  firstName: 'Fred',
  lastName: 'Flintstone'
})
  .then(response => console.log(response.data))
  .catch(error => console.error(error))

// 发送 FormData
const formData = new FormData()
formData.append('file', fileBlob)
axios.post('/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
```

### 并发请求

```typescript
function getUserAccount() {
  return axios.get('/user/12345')
}

function getUserPermissions() {
  return axios.get('/user/12345/permissions')
}

Promise.all([getUserAccount(), getUserPermissions()])
  .then(([account, permissions]) => {
    // 处理响应
  })

axios.all([getUserAccount(), getUserPermissions()])
  .then(axios.spread((account, permissions) => {
    // 处理响应
  }))
```

## 请求配置

### 基础配置

```typescript
axios({
  method: 'post',
  url: '/user/12345',
  data: {
    firstName: 'Fred',
    lastName: 'Flintstone'
  },
  timeout: 1000, // 超时时间
  headers: {'X-Custom-Header': 'foobar'},
})
```

### 响应模式

```typescript
// 响应类型
axios({
  url: '/user/12345',
  method: 'get',
  responseType: 'json', // 'arraybuffer', 'document', 'blob', 'text'
})

// 响应编码
axios({
  url: '/user/12345',
  method: 'get',
  responseEncoding: 'utf8',
})
```

## 拦截器

### 请求拦截器

```typescript
axios.interceptors.request.use(
  config => {
    // 添加 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)
```

### 响应拦截器

```typescript
axios.interceptors.response.use(
  response => {
    // 统一处理响应数据
    return response.data
  },
  error => {
    // 统一错误处理
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 跳转登录
          break
        case 403:
          // 无权限
          break
        case 500:
          // 服务器错误
          break
      }
    }
    return Promise.reject(error)
  }
)
```

## 创建实例

### 实例配置

```typescript
const api = axios.create({
  baseURL: 'https://api.example.com',
  timeout: 1000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
api.interceptors.request.use(config => config, error => error)

// 响应拦截器
api.interceptors.response.use(response => response, error => error)
```

## Vue 3 封装

### 请求封装

```typescript
// api/request.ts
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 30000,
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 添加 token
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const { code, data, message } = response.data
    if (code === 0 || code === 200) {
      return data
    }
    return Promise.reject(new Error(message))
  },
  (error) => {
    console.error('Request Error:', error)
    return Promise.reject(error)
  }
)

// 通用请求方法
export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service(config)
}

// GET 请求
export function get<T = any>(
  url: string,
  params?: any,
  config?: AxiosRequestConfig
): Promise<T> {
  return request<T>({ method: 'get', url, params, ...config })
}

// POST 请求
export function post<T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig
): Promise<T> {
  return request<T>({ method: 'post', url, data, ...config })
}

// PUT 请求
export function put<T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig
): Promise<T> {
  return request<T>({ method: 'put', url, data, ...config })
}

// DELETE 请求
export function del<T = any>(
  url: string,
  params?: any,
  config?: AxiosRequestConfig
): Promise<T> {
  return request<T>({ method: 'delete', url, params, ...config })
}
```

### API 定义

```typescript
// api/user.ts
import { get, post } from '@/api/request'

// 获取用户信息
export function getUserInfo(id: number) {
  return get<UserInfo>(`/user/${id}`)
}

// 创建用户
export function createUser(data: CreateUserRequest) {
  return post<User>('/user', data)
}

// 更新用户
export function updateUser(id: number, data: UpdateUserRequest) {
  return put<User>(`/user/${id}`, data)
}

// 删除用户
export function deleteUser(id: number) {
  return del<void>(`/user/${id}`)
}
```

## 错误处理

### 统一错误处理

```typescript
// api/error.ts
interface ApiError {
  code: number
  message: string
  details?: any
}

class ApiError extends Error implements ApiError {
  code: number
  details?: any

  constructor(error: any) {
    super(error.message)
    this.code = error.code || 500
    this.details = error.details
  }
}

// 在响应拦截器中抛出错误
service.interceptors.response.use(
  (response) => response.data,
  (error) => {
    throw new ApiError(error.response?.data || {})
  }
)
```

## 请求取消

### CancelToken

```typescript
import axios from 'axios'

const CancelToken = axios.CancelToken
const source = CancelToken.source()

axios.get('/user/12345', {
  cancelToken: source.token
})

// 取消请求
source.cancel('Operation canceled by the user.')

// AbortController (推荐)
const controller = new AbortController()

axios.get('/user/12345', {
  signal: controller.signal
})

controller.abort()
```

## 请求重试

### 重试配置

```typescript
import axios from 'axios'

axios.get('/api/data', {
  'axios-retry': {
    retries: 3,
    retryDelay: 1000,
    retryCondition: (error) => {
      return error.code === 'ECONNABORTED' ||
        error.code === 'ETIMEDOUT' ||
        error.response.status >= 500
    },
  },
})
```

## 上传下载

### 文件上传

```typescript
async function uploadFile(file: File, onProgress?: (percent: number) => void) {
  const formData = new FormData()
  formData.append('file', file)

  return axios.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (progressEvent) => {
      if (progressEvent.total && onProgress) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    },
  })
}
```

### 文件下载

```typescript
function downloadFile(url: string, filename?: string) {
  axios.get(url, {
    responseType: 'blob'
  }).then(response => {
    const blob = new Blob([response.data])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename || 'download'
    link.click()
    URL.revokeObjectURL(link.href)
  })
}
```

## 超时设置

### 全局超时

```typescript
axios.defaults.timeout = 5000
```

### 单个请求超时

```typescript
axios.get('/api/data', {
  timeout: 1000, // 1 秒超时
})
```

## 最佳实践

1. **实例创建**：为不同 API 创建独立的 axios 实例
2. **拦截器**：使用拦截器统一处理请求和响应
3. **类型安全**：使用 TypeScript 定义请求和响应类型
4. **错误处理**：统一的错误处理机制
5. **请求取消**：组件卸载时取消未完成的请求
