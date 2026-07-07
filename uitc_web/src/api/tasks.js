import http from './http'

// POST /tasks/url?url=xxx  触发后端异步分析，结果经 WebSocket /user/queue/analysis 推送
export function analyzeUrl(url) {
  return http.post('/tasks/url', null, { params: { url } })
}
