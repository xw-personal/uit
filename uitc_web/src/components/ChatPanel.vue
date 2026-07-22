<script setup>
import { ref, nextTick } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const messages = ref([])          // {role:'user'|'assistant', content:string}
const input = ref('')
const sending = ref(false)
const err = ref('')
const boxEl = ref(null)

// 把 SSE 的 data 格式化成可读行(JSON 有 phase/agent 就简写,否则原样/pretty)
function formatData(data) {
  try {
    const obj = JSON.parse(data)
    if (obj.phase && obj.agent) {
      const detail = obj.output || obj.error || obj.message || (obj.tool ? '调用 ' + obj.tool : '')
      return `[${obj.phase}] ${obj.agent}${detail ? ': ' + detail : ''}`
    }
    return JSON.stringify(obj, null, 2)
  } catch {
    return data
  }
}

async function send() {
  const msg = input.value.trim()
  if (!msg || sending.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: msg })
  const assistant = { role: 'assistant', content: '' }
  messages.value.push(assistant)
  sending.value = true
  err.value = ''
  scroll()

  try {
    // POST /tasks/run,带 Authorization 头(走 AuthInterceptor),body 是纯文本消息
    const resp = await fetch('/tasks/run', {
      method: 'POST',
      headers: {
        'Content-Type': 'text/plain',
        'Authorization': auth.token
      },
      body: msg
    })
    if (!resp.ok) throw new Error('HTTP ' + resp.status)

    // 流式消费 SSE(EventSource 不支持 POST+自定义头,用 fetch+ReadableStream)
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buf = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      // SSE 事件以空行分隔
      const parts = buf.split('\n\n')
      buf = parts.pop()                 // 最后一段可能不完整,留到下次
      for (const part of parts) {
        // 一个事件里可能有多行 data:,按 SSE 规范用 \n 拼接
        const dataLines = part.split('\n')
          .filter(l => l.startsWith('data:'))
          .map(l => l.slice(5))
        if (dataLines.length) {
          const line = formatData(dataLines.join('\n'))
          assistant.content += (assistant.content ? '\n' : '') + line
          scroll()
        }
      }
    }
    if (!assistant.content) assistant.content = '(无返回)'
  } catch (e) {
    err.value = '发送失败: ' + e.message
    assistant.content = '❌ ' + e.message
  } finally {
    sending.value = false
    scroll()
  }
}

function scroll() {
  nextTick(() => { if (boxEl.value) boxEl.value.scrollTop = boxEl.value.scrollHeight })
}

function clearAll() {
  messages.value = []
  err.value = ''
}
</script>

<template>
  <div class="chat">
    <div class="panel-head">
      <h2>对话</h2>
      <button class="secondary small" @click="clearAll">清空</button>
    </div>
    <div ref="boxEl" class="box">
      <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
        <div class="bubble">{{ m.content }}</div>
      </div>
      <div v-if="!messages.length" class="empty">发送消息开始对话（后端 /tasks/run 流式返回）</div>
    </div>
    <div class="input-row">
      <input v-model="input" type="text" placeholder="输入消息，回车发送"
             :disabled="sending" @keydown.enter="send" />
      <button :disabled="sending || !input.trim()" @click="send">
        {{ sending ? '接收中…' : '发送' }}
      </button>
    </div>
    <div v-if="err" class="err-text">{{ err }}</div>
  </div>
</template>

<style scoped>
.chat { display: flex; flex-direction: column; }
.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.panel-head h2 { margin: 0; font-size: 14px; color: var(--muted); text-transform: uppercase; letter-spacing: .5px; }
.box { height: 360px; overflow-y: auto; border: 1px solid var(--border); border-radius: 8px; padding: 12px; background: #fafbfc; }
.msg { margin-bottom: 10px; display: flex; }
.msg.user { justify-content: flex-end; }
.msg.assistant { justify-content: flex-start; }
.bubble { max-width: 80%; padding: 8px 12px; border-radius: 10px; font-size: 13px; white-space: pre-wrap; word-break: break-word; }
.msg.user .bubble { background: var(--accent); color: #fff; }
.msg.assistant .bubble { background: #fff; border: 1px solid var(--border); color: var(--text); }
.empty { color: var(--muted); font-size: 13px; text-align: center; padding: 40px 0; }
.input-row { display: flex; gap: 8px; margin-top: 10px; }
.input-row input { flex: 1; }
.small { padding: 4px 10px; font-size: 12px; }
.err-text { color: var(--err); font-size: 13px; margin-top: 8px; }
</style>
