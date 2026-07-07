import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import { useAuthStore } from './auth'

// WebSocket 状态与连接管理。
// - 由 App.vue 监听 auth.token 驱动 connect/disconnect
// - 订阅 /user/queue/analysis（URL 页面分析结果）和 /user/queue/kick（token 到期踢出）
// - 收到 kick → 清除 auth（触发 App.vue 断开 WS + 跳登录）
export const useWsStore = defineStore('ws', () => {
  const connected = ref(false)
  const analysisResult = ref(null)   // 最新一条 UrlElements
  const messages = ref([])            // 事件日志，最新在前
  let client = null

  function log(tag, msg, level = 'info') {
    messages.value.unshift({ ts: new Date().toLocaleTimeString('zh-CN', { hour12: false }), tag, msg, level })
    if (messages.value.length > 200) messages.value.pop()
  }

  function connect(token) {
    if (client && client.active) return
    log('WS', '正在连接 /ws-uit …')
    client = new Client({
      // 后端 withSockJS()，token 走 URL query param（SockJS 不支持自定义握手头）
      webSocketFactory: () => new SockJS(`/ws-uit?token=${encodeURIComponent(token)}`),
      reconnectDelay: 0,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: (frame) => {
        connected.value = true
        log('CONNECT', 'STOMP 已连接', 'ok')
        client.subscribe('/user/queue/analysis', (m) => {
          let body
          try { body = JSON.parse(m.body) } catch { body = m.body }
          analysisResult.value = body
          log('RECV', `/user/queue/analysis → ${JSON.stringify(body).slice(0, 160)}`, 'recv')
        })
        client.subscribe('/user/queue/kick', (m) => {
          let body
          try { body = JSON.parse(m.body) } catch { body = { message: m.body } }
          log('KICK', `收到踢出: ${body.message || ''}`, 'err')
          // token 到期：清除 auth，App.vue 的 watch 会断开 WS 并跳登录
          useAuthStore().clearAuth()
        })
      },
      onStompError: (frame) => {
        log('ERROR', `STOMP 错误: ${frame.headers['message'] || ''}`, 'err')
      },
      onWebSocketError: () => {
        log('ERROR', 'WebSocket 错误', 'err')
      },
      onWebSocketClose: (evt) => {
        connected.value = false
        log('CLOSE', `code=${evt.code} reason=${evt.reason || '(空)'}`, 'warn')
      },
      onDisconnect: () => {
        connected.value = false
        log('DISCONNECT', '已断开')
      }
    })
    client.activate()
  }

  function disconnect() {
    if (client && client.active) {
      log('WS', '主动断开…')
      client.deactivate()
    }
    client = null
  }

  function clearMessages() {
    messages.value = []
  }

  return { connected, analysisResult, messages, connect, disconnect, clearMessages }
})
