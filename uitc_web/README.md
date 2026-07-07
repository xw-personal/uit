# uitc-web

UITC 前端（Vue 3 + Vite + Pinia + vue-router）。当前实现：登录 + URL 页面分析。

## 启动

```bash
cd uitc_web
npm install
npm run dev      # http://localhost:5173
```

后端需运行在 `http://localhost:8080`。Vite dev server 已配代理，前端通过同源访问后端，无 CORS 问题。

## 功能

- **登录**（`POST /user/login`）：成功后保存 token，自动连接 WebSocket。
- **退出登录**：清除 token，自动断开 WebSocket，跳回登录页。
- **URL 页面分析**（`POST /tasks/url?url=xxx`）：触发后端异步分析，结果经 WebSocket `/user/queue/analysis` 推送，展示登录类型、识别到的组件、验证码图片。
- **token 到期**：后端 `AuthInterceptor` 检测到 token 失效时，经 `/user/queue/kick` 推送踢出消息，前端收到后断开 WebSocket 并跳回登录页。

## WebSocket

- 端点 `/ws-uit`（STOMP over SockJS）
- 鉴权：连接 URL 带 `?token=<jwt>`（SockJS 不支持自定义握手头，由后端 `HandshakeInterceptor` 解析）
- 订阅：
  - `/user/queue/analysis` —— 接收 `UrlElements`（页面分析结果）
  - `/user/queue/kick` —— 接收 token 到期踢出消息

连接生命周期由 `App.vue` 监听 `auth.token` 驱动：有 token 即连，无 token 即断。覆盖登录、退出、被踢三种情况。

## 结构

```
src/
  main.js              入口
  App.vue              根组件，监听 token 驱动 WS 连接/断开
  router/              路由 + 登录守卫
  stores/
    auth.js            登录态（token/userId/username，持久化到 localStorage）
    ws.js              WebSocket 连接、订阅、消息、日志
  api/
    http.js            axios 实例 + 拦截器（带 token、401 跳登录）
    auth.js            login / register
    tasks.js           analyzeUrl
  views/
    LoginView.vue      登录页
    MainView.vue       主页：URL 分析 + 结果 + WS 日志
  components/
    UrlAnalysisResult.vue   分析结果展示（登录类型/组件/验证码图）
```
