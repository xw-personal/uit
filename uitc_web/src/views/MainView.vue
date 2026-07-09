<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useWsStore } from '../stores/ws'
import { analyzeUrl } from '../api/tasks'
import UrlAnalysisResult from '../components/UrlAnalysisResult.vue'

const router = useRouter()
const auth = useAuthStore()
const ws = useWsStore()

const url = ref('')
const analyzing = ref(false)
const err = ref('')

async function onAnalyze() {
  err.value = ''
  if (!url.value.trim()) { err.value = '请输入 URL'; return }
  analyzing.value = true
  ws.analysisResult = null
  try {
    await analyzeUrl(url.value.trim())
    // 请求只是触发后端异步分析，真正的结果通过 WebSocket /user/queue/analysis 推过来
  } catch (e) {
    err.value = '触发失败: ' + e.message
  } finally {
    analyzing.value = false
  }
}

function onLogout() {
  auth.clearAuth()   // App.vue watch 会断开 WS；路由守卫跳登录
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="main">
    <header>
      <span class="title">UITC · URL 页面分析</span>
      <span class="spacer"></span>
      <span class="user">{{ auth.username || auth.userId }}</span>
      <span class="ws" :class="ws.connected ? 'on' : 'off'">
        ● {{ ws.connected ? 'WS 已连接' : 'WS 未连接' }}
      </span>
      <button class="secondary" @click="onLogout">退出登录</button>
    </header>

    <section class="panel">
      <h2>分析 URL</h2>
      <div class="url-row">
        <input v-model="url" type="text" placeholder="https://10.10.19.210/ui/#/login" @keydown.enter="onAnalyze" />
        <button :disabled="analyzing" @click="onAnalyze">{{ analyzing ? '已触发…' : '分析' }}</button>
      </div>
      <div v-if="err" class="err-text">{{ err }}</div>
      <div class="hint">提交后，后端异步分析登录页，结果通过 WebSocket 推送显示在下方。</div>
    </section>

    <section class="panel">
      <h2>分析结果</h2>
      <UrlAnalysisResult :data="ws.analysisResult" :login-status="ws.loginStatus" />
    </section>

    <section class="panel">
      <div class="panel-head">
        <h2>WebSocket 日志</h2>
        <button class="secondary small" @click="ws.clearMessages()">清空</button>
      </div>
      <div class="log">
        <div v-for="(m, i) in ws.messages" :key="i" class="log-line" :class="'lv-' + m.level">
          <span class="ts">{{ m.ts }}</span>
          <span class="tag">[{{ m.tag }}]</span>
          <span class="msg">{{ m.msg }}</span>
        </div>
        <div v-if="!ws.messages.length" class="empty">暂无日志</div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.main { max-width: 960px; margin: 0 auto; padding: 20px; }
header {
  display: flex; align-items: center; gap: 12px; padding: 12px 16px;
  background: var(--panel); border: 1px solid var(--border); border-radius: 10px; margin-bottom: 16px;
}
.title { font-weight: 600; }
.spacer { flex: 1; }
.user { color: var(--muted); font-size: 13px; }
.ws { font-size: 12px; font-weight: 600; }
.ws.on { color: var(--ok); }
.ws.off { color: var(--muted); }
.panel {
  background: var(--panel); border: 1px solid var(--border); border-radius: 10px;
  padding: 16px; margin-bottom: 16px;
}
.panel h2 { margin: 0 0 12px; font-size: 14px; color: var(--muted); text-transform: uppercase; letter-spacing: .5px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; }
.panel-head h2 { margin: 0; }
.url-row { display: flex; gap: 8px; }
.url-row input { flex: 1; }
.hint { color: var(--muted); font-size: 12px; margin-top: 10px; }
.small { padding: 4px 10px; font-size: 12px; }
.log {
  background: #0f1115; color: #e6e8eb; border-radius: 6px; padding: 8px;
  height: 260px; overflow-y: auto; font-family: ui-monospace, Consolas, monospace; font-size: 12px;
}
.log-line { padding: 2px 0; word-break: break-all; }
.log-line .ts { color: #8b929e; margin-right: 8px; }
.log-line .tag { display: inline-block; min-width: 70px; font-weight: 600; margin-right: 4px; }
.lv-info .tag { color: #4f9cf9; }
.lv-ok .tag { color: #3fb950; }
.lv-warn .tag { color: #d29922; }
.lv-err .tag { color: #f85149; }
.lv-recv .tag { color: #a371f7; }
.empty { color: #8b929e; padding: 8px; }
</style>
