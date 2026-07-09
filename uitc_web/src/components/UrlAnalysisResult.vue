<script setup>
import { reactive, computed, ref, watch } from 'vue'
import { submitLogin } from '../api/tasks'

const props = defineProps({
  data: { type: Object, default: null },
  loginStatus: { type: Object, default: null }
})

const formData = reactive({})
const submitting = ref(false)
const msg = ref('')
const err = ref('')
const loginSuccess = ref(false)

const components = computed(() => props.data?.components || [])

const typeLabel = {
  PASSWORD_ONLY: '账号密码',
  PASSWORD_CAPTCHA: '账号密码 + 图形验证码',
  SMS: '手机短信',
  QRCODE: '二维码扫码',
  SLIDER: '滑块/点选'
}

watch(() => props.data?.taskId, () => {
  Object.keys(formData).forEach(k => delete formData[k])
  components.value.forEach(c => { formData[c.id] = '' })
  msg.value = ''
  err.value = ''
  loginSuccess.value = false
}, { immediate: true })

watch(() => props.loginStatus, (status) => {
  if (!status) return
  if (status.status === true) {
    alert(status.message)
    loginSuccess.value = true
  } else {
    alert(status.message)
    if (props.data?.type === 'PASSWORD_CAPTCHA' && status.data?.captchaImage) {
      const captchaComp = components.value.find(c => c.type === 'captcha' || c.id === 'captcha')
      if (captchaComp) {
        captchaComp.config = captchaComp.config || {}
        captchaComp.config.captchaImage = status.data.captchaImage
      }
    }
  }
}, { deep: true })

function inputType(id) {
  return id === 'password' ? 'password' : 'text'
}

function captchaImage(comp) {
  return comp.config?.captchaImage || null
}

async function onSubmit() {
  err.value = ''
  msg.value = ''
  if (!props.data?.taskId) { err.value = '无 taskId'; return }
  submitting.value = true
  try {
    const loginUser = {}
    components.value.forEach(c => { loginUser[c.id] = formData[c.id] ?? '' })
    await submitLogin(props.data.taskId, loginUser)
    msg.value = '已提交，等待后端登录结果…'
  } catch (e) {
    err.value = '提交失败: ' + e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="result">
    <div v-if="!data" class="empty">暂无分析结果。提交 URL 后，结果将经 WebSocket 推送显示在此。</div>
    <template v-else>
      <div class="kv"><span class="k">登录类型</span>
        <span class="tag">{{ data.type }}</span>
        <span class="v">{{ typeLabel[data.type] || data.type }}</span>
      </div>
      <div class="kv"><span class="k">taskId</span><span class="v mono">{{ data.taskId }}</span></div>

      <h3>请填写登录信息</h3>
      <div v-for="c in components" :key="c.id" class="field">
        <template v-if="c.type === 'captcha'">
          <label>{{ c.id }}</label>
          <div class="captcha-row">
            <img v-if="captchaImage(c)" :src="captchaImage(c)" class="captcha-img" alt="captcha" />
            <input v-model="formData[c.id]" :type="inputType(c.id)" :placeholder="c.id" :disabled="loginSuccess" />
          </div>
        </template>
        <template v-else>
          <label>{{ c.id }}</label>
          <input v-model="formData[c.id]" :type="inputType(c.id)" :placeholder="c.id" :disabled="loginSuccess" />
        </template>
      </div>

      <button class="submit" :disabled="submitting || loginSuccess" @click="onSubmit">
        {{ submitting ? '提交中…' : loginSuccess ? '已登录' : '提交' }}
      </button>
      <div v-if="err" class="err-text">{{ err }}</div>
      <div v-else-if="msg" class="ok-text">{{ msg }}</div>
      <div v-if="loginSuccess" class="ok-text">登录成功，可关闭此页面</div>
    </template>
  </div>
</template>

<style scoped>
.result { font-size: 13px; }
.empty { color: var(--muted); }
.kv { display: flex; align-items: center; gap: 8px; margin: 6px 0; }
.kv .k { color: var(--muted); min-width: 64px; }
.kv .v { color: var(--text); }
.kv .tag { background: var(--accent); color: #fff; padding: 1px 8px; border-radius: 4px; font-size: 12px; }
.mono { font-family: ui-monospace, Consolas, monospace; }
h3 { margin: 16px 0 8px; font-size: 13px; color: var(--muted); }
.field { margin-bottom: 12px; }
.field label { display: block; color: var(--muted); font-size: 12px; margin-bottom: 4px; }
.captcha-row { display: flex; gap: 8px; align-items: center; }
.captcha-row input { flex: 1; }
.captcha-img { width: 120px; height: 40px; object-fit: cover; border: 1px solid var(--border); border-radius: 4px; }
.submit { margin-top: 8px; width: 100%; }
.ok-text { color: var(--ok); font-size: 13px; margin-top: 8px; }
</style>
