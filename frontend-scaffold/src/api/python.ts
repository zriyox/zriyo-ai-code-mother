/**
 * Python AI 服务 API 封装
 */

import { request } from '@/api/request'
import type { AgentRequest, AgentResult } from '@/types'

const PYTHON_BASE_URL = import.meta.env.VITE_APP_BASE_PYTHON_API || '/python-api'

// ================ Token 计算 API ================

/**
 * 计算文本 token 数量
 */
export function countTokens(params: {
  provider: string
  text: string
  model?: string
}) {
  return request<number>({
    baseURL: PYTHON_BASE_URL,
    url: '/api/internal/count-tokens',
    method: 'post',
    data: params,
  })
}

/**
 * 计算消息列表 token 数量
 */
export function countMessages(params: {
  provider: string
  messages: Array<{ role: string; content: string }>
  model?: string
}) {
  return request<{ token_count: number; message_count: number }>({
    baseURL: PYTHON_BASE_URL,
    url: '/api/internal/count-messages',
    method: 'post',
    data: params,
  })
}

/**
 * 获取支持的提供商列表
 */
export function getProviders() {
  return request<{ providers: string[]; count: number }>({
    baseURL: PYTHON_BASE_URL,
    url: '/api/internal/providers',
    method: 'get',
  })
}

/**
 * 获取提供商信息
 */
export function getProviderInfo(provider: string) {
  return request({
    baseURL: PYTHON_BASE_URL,
    url: `/api/internal/providers/${provider}`,
    method: 'get',
  })
}

// ================ Agent API ================

/**
 * 执行 Agent 任务（流式）
 */
export function runAgent(
  params: AgentRequest,
  onMessage: (event: MessageEvent) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void
): () => void {
  const url = `${PYTHON_BASE_URL}/api/v1/agent-run?stream=true`

  const eventSource = new EventSource(url, {
    // 添加认证头（需要后端支持）
  })

  // 处理 SSE 事件
  eventSource.addEventListener('request_start', onMessage)
  eventSource.addEventListener('agent_thought', onMessage)
  eventSource.addEventListener('tool_call', onMessage)
  eventSource.addEventListener('tool_result', onMessage)
  eventSource.addEventListener('text_chunk', onMessage)
  eventSource.addEventListener('code_chunk', onMessage)
  eventSource.addEventListener('markdown_chunk', onMessage)
  eventSource.addEventListener('progress', onMessage)
  eventSource.addEventListener('file_created', onMessage)
  eventSource.addEventListener('file_updated', onMessage)
  eventSource.addEventListener('request_complete', (e) => {
    onMessage(e)
    onComplete?.()
    eventSource.close()
  })
  eventSource.addEventListener('error', (e) => {
    onError?.(new Error('Agent execution failed'))
    eventSource.close()
  })

  // 返回取消函数
  return () => {
    eventSource.close()
  }
}

/**
 * 执行 Agent 任务（非流式）
 */
export function runAgentSync(params: AgentRequest): Promise<AgentResult> {
  return request<AgentResult>({
    baseURL: PYTHON_BASE_URL,
    url: '/api/v1/agent-run',
    method: 'post',
    data: params,
  })
}

/**
 * 停止 Agent 执行
 */
export function stopAgent(traceId: string, requestId?: string) {
  return request({
    baseURL: PYTHON_BASE_URL,
    url: '/api/v1/stop',
    method: 'post',
    params: { trace_id: traceId, request_id: requestId },
  })
}

// ================ 聊天 API ================

/**
 * 聊天对话
 */
export function chat(params: {
  messages: Array<{ role: string; content: string }>
  llm_config: Record<string, any>
  stream?: boolean
}) {
  return request({
    baseURL: PYTHON_BASE_URL,
    url: '/api/v1/chat',
    method: 'post',
    data: params,
  })
}

// ================ 健康检查 API ================

/**
 * Python 服务 ping
 */
export function pingPython() {
  return request<{ pong: boolean; timestamp: number; version: string }>({
    baseURL: PYTHON_BASE_URL,
    url: '/api/internal/ping',
    method: 'get',
  })
}

/**
 * 健康检查
 */
export function healthCheck(services?: string[]) {
  return request<{
    status: 'healthy' | 'degraded' | 'unhealthy'
    checks: Record<string, any>
  }>({
    baseURL: PYTHON_BASE_URL,
    url: '/api/internal/health',
    method: 'post',
    data: { services },
  })
}
