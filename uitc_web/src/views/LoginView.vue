<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { login, register } from '../api/auth'

const router = useRouter()
const auth = useAuthStore()

const account = ref('')
const password = ref('')
const loading = ref(false)
const err = ref('')

async function onLogin() {
  err.value = ''
  if (!account.value || !password.value) { err.value = '账号和密码必填'; return }
  loading.value = true
  try {
    const data = await login(account.value, password.value)
    auth.setAuth(data)
    // App.vue 的 watch 会自动连接 WS
    router.push({ name: 'main' })
  } catch (e) {
    err.value = '登录失败: ' + e.message
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  err.value = ''
  if (!account.value || !password.value) { err.value = '账号和密码必填'; return }
  try {
    await register(account.value, password.value)
    err.value = '注册成功，可登录'
  } catch (e) {
    err.value = '注册失败: ' + e.message
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="login-card">
      <h1>UITC 登录</h1>
      <label>账号</label>
      <input v-model="account" type="text" placeholder="account" @keydown.enter="onLogin" />
      <label>密码</label>
      <input v-model="password" type="password" placeholder="password" @keydown.enter="onLogin" />
      <div class="row">
        <button :disabled="loading" @click="onLogin">{{ loading ? '登录中…' : '登录' }}</button>
        <button class="secondary" @click="onRegister">注册</button>
      </div>
      <div v-if="err" class="err-text">{{ err }}</div>
    </div>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-card {
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 28px;
  width: 340px;
  box-shadow: 0 4px 16px rgba(0,0,0,.04);
}
.login-card h1 { margin: 0 0 16px; font-size: 18px; }
label { display: block; color: var(--muted); font-size: 12px; margin: 12px 0 4px; }
.row { display: flex; gap: 8px; margin-top: 18px; }
.row button { flex: 1; }
</style>
