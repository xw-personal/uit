import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const http = axios.create({ baseURL: '', timeout: 30000 })

// 请求拦截：自动带 Authorization 头
http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) config.headers.Authorization = auth.token
  return config
})

// 响应拦截：拆 Result 信封 {code,message,data}；401 清登录态跳登录
http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 200) {
      return Promise.reject(new Error(body.message || `code ${body.code}`))
    }
    return body && typeof body === 'object' && 'data' in body ? body.data : body
  },
  (error) => {
    if (error.response?.status === 401) {
      const auth = useAuthStore()
      auth.clearAuth()   // App.vue watch 会断开 WS；路由守卫拦回登录
      router.push({ name: 'login' })
    }
    const msg = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export default http
