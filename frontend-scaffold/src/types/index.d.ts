/**
 * 全局类型定义
 */

declare global {
  // 构建时注入的全局变量
  const __APP_VERSION__: string
  const __BUILD_TIME__: string

  interface Window {
    __APP_VERSION__: string
    __BUILD_TIME__: string
  }
}

// ================ 通用类型 ================

export type Nullable<T> = T | null
export type Optional<T> = T | undefined
export type Dict<T = any> = Record<string, T>
export type MaybeArray<T> = T | T[]
export type MaybePromise<T> = T | Promise<T>

// ================ API 类型 ================

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export interface ApiError {
  code: number
  message: string
  details?: Record<string, any>
}

export interface PageParams {
  page: number
  pageSize: number
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

// ================ 路由类型 ================

export interface RouteMeta {
  title?: string
  icon?: string
  hidden?: boolean
  keepAlive?: boolean
  affix?: boolean
  noCache?: boolean
  breadcrumb?: boolean
  fullscreen?: boolean
}

export interface RouteItem {
  path: string
  name?: string
  component?: () => Promise<any>
  redirect?: string
  meta?: RouteMeta
  children?: RouteItem[]
}

// ================ 用户类型 ================

export interface UserInfo {
  id: number | string
  username: string
  nickname?: string
  avatar?: string
  email?: string
  roles?: string[]
  permissions?: string[]
}

// ================ 菜单类型 ================

export interface MenuItem {
  id: string | number
  parentId: string | number | null
  name: string
  path: string
  component?: string
  icon?: string
  sort: number
  visible: boolean
  children?: MenuItem[]
}

// ================ 文件类型 ================

export interface UploadFile {
  uid: string
  name: string
  status: 'ready' | 'uploading' | 'success' | 'error'
  response?: any
  url?: string
  percentage?: number
}

// ================ 表单类型 ================

export interface FormItem {
  prop: string
  label: string
  type: 'input' | 'select' | 'radio' | 'checkbox' | 'date' | 'number' | 'textarea' | 'upload'
  placeholder?: string
  options?: Array<{ label: string; value: any }>
  required?: boolean
  rules?: any[]
}

// ================ Agent 相关类型 ================

export interface AgentType {
  value: string
  label: string
  description?: string
}

export interface AgentRequest {
  request_id: string
  trace_id: string
  user_id: number
  app_id: number
  task_type: string
  message: string
  target_files?: string[]
  project_context?: Record<string, any>
  conversation?: Array<{ role: string; content: string }>
  llm_config: LlmConfig
}

export interface LlmConfig {
  provider: string
  model: string
  api_key: string
  temperature?: number
  max_tokens?: number
  base_url?: string
}

export interface AgentResult {
  request_id: string
  trace_id: string
  status: 'success' | 'failed' | 'pending'
  files?: Array<{ path: string; action: 'created' | 'modified' }>
  data?: Record<string, any>
  error?: string
}

// ================ SSE 事件类型 ================

export interface SseEvent {
  trace_id: string
  event_type: string
  timestamp: number
  data: Record<string, any>
  event_id?: string
}

export type SseEventType =
  | 'request_start'
  | 'request_complete'
  | 'agent_thought'
  | 'agent_switch'
  | 'tool_call'
  | 'tool_result'
  | 'progress'
  | 'file_created'
  | 'file_updated'
  | 'text_chunk'
  | 'code_chunk'
  | 'markdown_chunk'
  | 'error'
  | 'warning'

// ================ 组件 Props 类型 ================

export interface SizeProps {
  size?: 'large' | 'default' | 'small'
}

export interface ColorProps {
  color?: string
}

// ================ 图表类型 ================

export interface ChartData {
  name: string
  value: number
  [key: string]: any
}

export interface SeriesOption {
  name?: string
  type?: 'line' | 'bar' | 'pie' | 'scatter' | 'gauge' | 'funnel' | 'radar'
  data?: any[]
  [key: string]: any
}

// ================ 3D 类型 ================

export interface Object3DConfig {
  type: 'cube' | 'sphere' | 'cylinder' | 'cone' | 'torus' | 'plane'
  position?: [number, number, number]
  rotation?: [number, number, number]
  scale?: [number, number, number] | number
  color?: string
  wireframe?: boolean
  opacity?: number
}

// ================ 主题类型 ================

export type ThemeMode = 'light' | 'dark' | 'auto'

export interface ThemeConfig {
  mode: ThemeMode
  primaryColor: string
  borderRadius: number
  fontSize: number
}

export {}

// ================ 环境变量类型 ================

export interface EnvConfig {
  VITE_APP_TITLE: string
  VITE_APP_PORT: number
  VITE_APP_BASE_API: string
  VITE_APP_BASE_PYTHON_API: string
}

export {}

export {}
