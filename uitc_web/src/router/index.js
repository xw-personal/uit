import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  {
    path: '/main',
    name: 'main',
    component: () => import('../views/MainView.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) return { name: 'login' }
  if (to.name === 'login' && auth.token) return { name: 'main' }
})

export default router
