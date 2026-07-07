<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Object, default: null }   // UrlElements: {type, taskId, components:[{id,type,config}]}
})

const loginType = computed(() => props.data?.type || '')
const components = computed(() => props.data?.components || [])
const typeLabel = {
  PASSWORD_ONLY: '账号密码',
  PASSWORD_CAPTCHA: '账号密码 + 图形验证码',
  SMS: '手机短信',
  QRCODE: '二维码扫码',
  SLIDER: '滑块/点选'
}
const captchaImage = computed(() => {
  const c = components.value.find(c => c.type === 'captcha')
  return c?.config?.captchaImage || null
})
</script>

<template>
  <div class="result">
    <div v-if="!data" class="empty">暂无分析结果。提交 URL 后，结果将经 WebSocket 推送显示在此。</div>
    <template v-else>
      <div class="kv"><span class="k">登录类型</span>
        <span class="v tag">{{ loginType }}</span>
        <span class="v">{{ typeLabel[loginType] || loginType }}</span>
      </div>
      <div class="kv"><span class="k">taskId</span><span class="v mono">{{ data.taskId }}</span></div>

      <h3>识别到的组件</h3>
      <div class="comps">
        <div v-for="c in components" :key="c.id" class="comp">
          <span class="cid">{{ c.id }}</span>
          <span class="ctype">{{ c.type }}</span>
        </div>
      </div>

      <div v-if="captchaImage" class="captcha">
        <h3>验证码图片</h3>
        <img :src="captchaImage" alt="captcha" />
      </div>
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
.comps { display: flex; flex-wrap: wrap; gap: 8px; }
.comp {
  border: 1px solid var(--border); border-radius: 6px; padding: 6px 10px;
  display: flex; gap: 8px; align-items: center; background: #fafbfc;
}
.cid { font-weight: 600; }
.ctype { color: var(--muted); font-size: 12px; }
.captcha img { max-width: 320px; border: 1px solid var(--border); border-radius: 6px; }
</style>
