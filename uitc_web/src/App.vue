<script setup>
import { watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useWsStore } from './stores/ws'

const auth = useAuthStore()
const ws = useWsStore()
const router = useRouter()

// token 出现→连接 WS；token 消失→断开 WS（覆盖登录、退出、被踢三种情况）
watch(() => auth.token, (token) => {
  if (token) ws.connect(token)
  else ws.disconnect()
}, { immediate: true })

// token 失效时若在受保护页面，跳回登录
watch(() => auth.token, (token) => {
  if (!token && router.currentRoute.value.meta?.requiresAuth) {
    router.push({ name: 'login' })
  }
})
</script>

<template>
  <router-view />
</template>
