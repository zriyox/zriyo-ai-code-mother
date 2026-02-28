/**
 * Axios 请求封装
 */

import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
  type AxiosError,
} from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import type { ApiResponse, ApiError } from '@/types'

// 配置 NProgress
NProgress.configure({
  showSpinner: false,
  trickleSpeed: 200,
})

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 显示进度条
    NProgress.start()

    // 添加 token
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加 trace_id 用于链路追踪
    config.headers['X-Trace-ID'] = generateTraceId()

    // 添加时间戳防止缓存
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now(),
      }
    }

    return config
  },
  (error: AxiosError) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    NProgress.done()
    const { code, data, message } = response.data

    // 根据自定义错误码处理
    if (code === 0 || code === 200) {
      return data
    }

    // 业务错误
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || 'Error'))
  },
  (error: AxiosError<ApiError>) => {
    NProgress.done()

    const { response } = error
    let message = '请求失败'

    if (response) {
      const { status, data } = response

      switch (status) {
        case 401:
          message = '未授权，请重新登录'
          // 跳转到登录页
          ElMessageBox.confirm('登录状态已过期，请重新登录', '系统提示', {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning',
          }).then(() => {
            localStorage.clear()
            window.location.href = '/login'
          })
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        case 502:
          message = '网关错误'
          break
        case 503:
          message = '服务不可用'
          break
        case 504:
          message = '网关超时'
          break
        default:
          message = data?.message || `连接错误${status}`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时'
    } else if (error.message === 'Network Error') {
      message = '网络连接异常'
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/**
 * 生成 trace ID
 */
function generateTraceId(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 15)}`
}

/**
 * 通用请求方法
 */
export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service(config)
}

/**
 * GET 请求
 */
export function get<T = any>(
  url: string,
  params?: any,
  config?: AxiosRequestConfig
): Promise<T> {
  return request<T>({ method: 'get', url, params, ...config })
}

/**
 * POST 请求
 */
export function post<T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig
): Promise<T> {
  return request<T>({ method: 'post', url, data, ...config })
}

/**
 * PUT 请求
 */
export function put<T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig
): Promise<T> {
  return request<T>({ method: 'put', url, data, ...config })
}

/**
 * DELETE 请求
 */
export function del<T = any>(
  url: string,
  params?: any,
  config?: AxiosRequestConfig
): Promise<T> {
  return request<T>({ method: 'delete', url, params, ...config })
}

/**
 * 文件上传
 */
export function upload<T = any>(url: string, file: File, onProgress?: (percent: number) => void): Promise<T> {
  const formData = new FormData()
  formData.append('file', file)

  return request<T>({
    method: 'post',
    url,
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    },
  })
}

/**
 * 文件下载
 */
export function download(url: string, filename?: string): void {
  const link = document.createElement('a')
  link.href = url
  if (filename) {
    link.download = filename
  }
  link.click()
}

export default service
