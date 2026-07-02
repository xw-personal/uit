# UI 测试用例生成 Agent —— 需求文档（Proposal）

> 文档版本：v0.2
> 创建日期：2026-06-29
> 状态：草案，待评审
> 变更：v0.2 主服务技术栈由 Python 改为 Java 17 + Spring Boot 3.x；脚本产物保留 Playwright Python，由独立 Python Runner 微服务执行。

---

## 0. 文档说明

本文档定义"基于 UI 自动分析并生成测试用例的 Agent"项目的需求范围、技术架构与分阶段交付计划。文档分三个部分：

1. **功能需求清单** —— 描述 Agent 要做什么。
2. **技术架构设计** —— 描述 Agent 怎么做。
3. **项目路线图与验收** —— 描述分阶段交付节奏与验收标准。

文档面向项目立项评审，最终实现细节将在后续设计文档中展开。

---

## 1. 项目背景与目标

### 1.1 背景

手工编写 UI 测试用例存在以下问题：

- QA 需要逐一查看页面、枚举控件，工作量大且容易遗漏；
- UI 改版后用例维护成本高；
- 自动化脚本（如 Playwright）编写门槛高，QA 同学不一定熟悉；
- 功能用例与异常用例往往依赖工程师经验，覆盖度不一致。

### 1.2 目标

构建一个 Agent 系统，输入一组运行中的网页 URL（可选附带需求文档），自动产出：

- **人类 QA 阅读的测试用例文档**（Markdown 形式，含步骤、预期、优先级）；
- **可执行的 Playwright 自动化脚本**（Python，覆盖功能与异常/边界）；
- **执行验证报告**（Agent 自跑一遍并尝试自修复后的结果）。

### 1.3 非目标（本期不做）

- 不做性能、压力、安全渗透类测试；
- 不做兼容性测试（多浏览器/多分辨率/移动端），可作为后续扩展；
- 不做视觉回归（pixel diff）测试；
- 不替代 QA 的最终判断，所有用例必须经过人工校验后才可正式入库。

---

## 2. 功能需求清单

### 2.1 输入

| 项目 | 必填 | 说明 |
| --- | --- | --- |
| 页面 URL 清单 | 是 | 一次任务可包含 1~N 个 URL（同一站点） |
| 登录凭证 | 否 | 账号 + 密码 + 登录页 URL；如不提供则仅扫描公开页面 |
| 需求文档 | 否 | PRD/需求描述文本或文件，作为业务上下文增强用例语义 |
| 业务标签 | 否 | 如"电商-下单"，用于辅助用例命名与分类 |

### 2.2 核心功能

#### F1. UI 解析

- **F1.1 DOM 解析**：通过 Playwright 加载页面，提取所有可交互元素（按钮、输入框、链接、表单、下拉、复选/单选、上传、表格、模态框等），获取选择器、可见文本、属性、绑定事件、可达性信息。
- **F1.2 视觉补盲**：对页面整体截图与关键区域截图，调用多模态 LLM 识别 DOM 难以发现的视觉元素（如纯图片按钮、自定义渲染组件、Canvas 中的交互区域），与 DOM 结果交叉验证、补漏。
- **F1.3 页面状态发现**：自动触发常见状态（hover、focus、表单空提交、错误提示），记录每种状态下的元素差异。
- **F1.4 登录处理**：若提供账号密码，则使用账号密码登录并保存 storage state，供后续页面复用。

#### F2. 用例生成

- **F2.1 功能用例**：每个交互元素生成正向用例（典型输入→预期结果），覆盖表单提交、跳转、增删改查等主流程。
- **F2.2 异常/边界用例**：针对输入控件生成边界值、非法字符、空值、超长、特殊字符、SQL/XSS 关键字、网络中断等异常用例。
- **F2.3 用例分级**：按 P0/P1/P2 标注优先级（基于元素显著性、业务关键路径推断）。
- **F2.4 业务语义增强**：若提供 PRD，用例描述与命名结合业务术语，而非"点击 btn-1"这类机械描述。

#### F3. 双形态输出

- **F3.1 文档输出**：生成 Markdown 文件，包含用例编号、所属页面、前置条件、步骤、预期结果、优先级、用例类型。
- **F3.2 脚本输出**：生成 Playwright Python 脚本，按页面或按用户旅程组织文件，包含 fixtures、storage state 复用、断言。
- **F3.3 文档与脚本一一对应**：每条文档用例附带对应脚本的位置（文件名 + 函数名），方便追溯。

#### F4. 自动验证与自修复

- **F4.1 执行**：脚本生成后 Agent 自动跑一遍（headless）。
- **F4.2 失败分类**：将失败分为「选择器失效」「等待时序」「断言过严」「真实 Bug」四类。
- **F4.3 自修复**：前三类失败 Agent 自动重写脚本最多 N 次（N 可配，默认 3）。
- **F4.4 真实 Bug 报告**：第四类标记为疑似缺陷，附截图与失败上下文，供 QA 复核。
- **F4.5 验证报告**：输出本轮通过率、修复次数、剩余疑似缺陷数。

#### F5. 用例库与版本管理

- **F5.1 用例入库**：所有用例存入服务端用例库（含元数据：URL、生成时间、版本号、Agent 模型版本）。
- **F5.2 变更检测**：同一 URL 重新生成时，与上一版本对比，识别「新增 / 删除 / 修改」的用例，输出变更摘要。
- **F5.3 人工校验闭环**：提供接口允许 QA 标记用例为「通过 / 修改 / 拒绝」，被修改/拒绝的用例下次生成时作为反馈样本注入 prompt。
- **F5.4 执行历史**：每次脚本运行结果（成功/失败/截图/日志）入库，可按 URL、时间、用例号查询。

#### F6. 任务编排

- **F6.1 任务调度**：支持异步任务，提交后返回 task_id，可轮询状态。
- **F6.2 多页面并发**：同一任务内的多个 URL 并发处理（受配置上限约束）。
- **F6.3 任务取消与重试**：支持中途取消、按页面级别重试。

### 2.3 交互形式

- **形态**：REST API 服务。
- **核心端点（示意）**：
  - `POST /tasks` 提交生成任务（URL 清单 / 登录凭证 / PRD / 配置）→ 返回 `task_id`
  - `GET /tasks/{task_id}` 查询任务状态与产物链接
  - `GET /tasks/{task_id}/cases` 获取本次生成的用例
  - `POST /cases/{case_id}/review` 人工标记用例（通过/修改/拒绝）
  - `GET /cases/{case_id}/history` 查询某用例的执行历史
  - `POST /tasks/{task_id}/run` 触发脚本重跑

---

## 3. 技术架构设计

### 3.1 整体形态

采用 **多 Agent 协作** 架构，按职责拆分子 Agent，由 Java 端的编排层串联。理由：

- UI 解析、用例生成、脚本生成、验证修复职责差异大，单 Agent 长 prompt 容易失焦；
- 不同子任务对模型能力侧重不同（视觉理解 vs 代码生成 vs 推理），便于针对性选模型；
- 每个子 Agent 可独立评估与迭代，便于分阶段交付。

主服务采用 **Java 17 + Spring Boot 3.x**；脚本最终产物仍为 **Playwright Python**（QA 友好、生态成熟），由独立的 **Python Runner 微服务** 负责执行。Java 主服务与 Python Runner 通过 HTTP/gRPC 解耦，避免在 Java 容器内捆绑 Python 运行环境。

### 3.2 模块划分

```
┌──────────────────────────────────────────────────────────────────┐
│              API Gateway (Spring Boot 3.x · Spring MVC)          │
│         /tasks  /cases  /review  /run  + 鉴权 + 速率限制          │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│            Orchestrator（Spring + RabbitMQ 消息驱动）             │
│      任务状态机 · 失败重试 · 产物归档 · 子 Agent 编排              │
└──────────────────────────────────────────────────────────────────┘
                                │
        ┌───────────────┬───────┴───────┬───────────────┐
        ▼               ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ UI Explorer  │ │ Case Designer│ │ Script Writer│ │ Verifier     │
│ Agent (Java) │ │ Agent (Java) │ │ Agent (Java) │ │ Agent (Java) │
│              │ │              │ │              │ │              │
│ DOM 抓取     │ │ 功能用例     │ │ 生成          │ │ 调 Python    │
│ (PW for Java)│ │ 异常用例     │ │ Playwright   │ │ Runner 跑脚本│
│ 截图+VLM     │ │ 优先级       │ │ Python 脚本  │ │ 失败分类     │
│ 状态探索     │ │ 业务语义     │ │ + Markdown   │ │ 自修复       │
│ 登录处理     │ │              │ │ 文档         │ │ Bug 标记     │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
        │               │               │               │
        │   (LangChain4j 统一封装 LLM/VLM 调用)         │
        └───────────────┴───────┬───────┴───────────────┘
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│       Python Runner 微服务（独立部署）                            │
│  接口：gRPC/HTTP    职责：接收脚本+用例 → Playwright 执行         │
│  → 返回 通过/失败/截图/trace/日志                                 │
│  栈：Python 3.11 + Playwright Python + FastAPI                   │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                          数据持久层                              │
│  PostgreSQL：任务/用例/执行历史/版本                              │
│  对象存储 (MinIO/S3)：截图/trace/脚本/日志                        │
│  Redis：分布式锁 / 缓存 / storage state 临时存储                  │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│            外部依赖：LLM Provider（国内厂商 API）                 │
│   推理/代码：DeepSeek-V3 / Qwen-Max / GLM-4                       │
│   视觉：Qwen-VL-Max / GLM-4V                                     │
│   通过 LangChain4j 统一抽象，便于切换                             │
└──────────────────────────────────────────────────────────────────┘
```

### 3.3 子 Agent 职责

| Agent | 实现位置 | 输入 | 输出 | 关键技术 |
| --- | --- | --- | --- | --- |
| **UI Explorer** | Java 主服务 | URL + 凭证 + PRD | 结构化 UI 描述（元素树 + 状态 + 截图引用） | Playwright for Java + LangChain4j（VLM 调用） |
| **Case Designer** | Java 主服务 | UI 描述 + PRD + 历史反馈 | 用例 JSON（步骤、预期、类型、优先级） | LangChain4j + 用例规则模板 |
| **Script Writer** | Java 主服务 | 用例 JSON + UI 描述 | Playwright Python 脚本 + Markdown 文档 | LangChain4j + Python 项目模板（pytest + playwright fixture） |
| **Verifier** | Java 主服务 + Python Runner | 脚本 + 用例 | 执行报告 + 修复后脚本 + 疑似 Bug 列表 | gRPC 调 Python Runner，LangChain4j 做失败诊断 |

### 3.4 关键技术选型

| 项 | 选型 | 备选 / 说明 |
| --- | --- | --- |
| 主服务语言 | **Java 17 LTS** | 团队主栈，长期支持，Spring 生态最佳契合 |
| Web 框架 | **Spring Boot 3.x（Spring MVC）** | OpenAPI 3 通过 springdoc-openapi 暴露；如需异步流式可局部引入 WebFlux |
| Agent / LLM 编排 | **LangChain4j** | Java 生态最活跃 Agent 框架，支持 Function Calling、Tool、RAG、Memory；统一封装多家国内 LLM |
| 任务队列 | **RabbitMQ** | MVP 阶段足够，运维成本低；后续扩展再评估 Kafka |
| 浏览器自动化（采集端） | **Playwright for Java 1.4x** | 与主服务同语言，部署简单，能力与 Playwright Python 对齐 |
| 浏览器自动化（脚本产物） | **Playwright Python + pytest-playwright** | QA 友好、案例多；以 pytest 作为测试组织框架 |
| 脚本执行 | **独立 Python Runner 微服务** | Python 3.11 + FastAPI + Playwright；通过 gRPC（首选）或 REST 与 Java 通信 |
| 服务间通信 | gRPC（Java ↔ Python Runner）；REST（对外 API） | gRPC 用 protobuf 严格 schema，避免脚本/结果格式漂移 |
| LLM 客户端 | LangChain4j 内置 + 国内厂商 OpenAI 兼容端点 | 推理/代码：DeepSeek-V3；视觉：Qwen-VL-Max；可热切换 |
| 关系型存储 | **PostgreSQL 15+** | 用例/任务/历史；JSONB 存非结构化用例数据 |
| 对象存储 | **MinIO**（自建）/ S3 兼容 | 截图、脚本文件、Playwright trace、运行日志 |
| 缓存 / 锁 | **Redis 7** | 分布式锁、storage state 短期缓存、限流计数 |
| ORM | Spring Data JPA + Hibernate；复杂查询用 MyBatis-Plus（按需） | |
| 构建 | Maven 或 Gradle（建议 Gradle 8） | 子模块：`api-gateway` / `orchestrator` / `agent-core` / `common-proto` |
| 部署 | Docker + docker-compose（MVP）→ Kubernetes + Helm（生产） | Java 与 Python Runner 各自独立镜像 |
| 可观测性 | Spring Boot Actuator + Micrometer + Prometheus + Grafana；日志 Logback + ELK | |

### 3.5 数据模型（核心表）

- `tasks(id, status, urls, prd_ref, created_at, finished_at, summary_json)`
- `pages(id, task_id, url, ui_snapshot_ref, screenshot_ref)`
- `cases(id, page_id, version, title, steps_jsonb, expected, priority, type, status, script_ref)`
- `case_reviews(case_id, version, reviewer, action, comment, ts)`
- `runs(id, case_id, status, error_class, log_ref, trace_ref, screenshot_ref, ts)`
- `script_artifacts(id, case_id, version, language, file_ref, checksum)`

### 3.6 关键风险与对策

| 风险 | 对策 |
| --- | --- |
| LLM 生成的选择器不稳定 | 优先使用 role/text/data-testid；生成后由 Verifier 实跑校验；Playwright for Java 采集时保留多种 locator 候选 |
| 自修复死循环 | 限制最大修复次数（默认 3）；区分"语法/选择器"与"业务真实失败" |
| 登录态泄露 | 凭证仅内存中使用；storage state 加密后存 Redis，任务结束按 TTL 清理 |
| 用例噪声多 | 引入"人工校验闭环"，被拒用例作为负样本回流到 LangChain4j 的 Memory / Prompt 样例 |
| 国内 LLM 视觉能力差异 | 视觉模型独立配置，保留切换能力；DOM 路径不依赖 VLM 也能跑通 |
| 用例与脚本不一致 | Case Designer 一次产出 case_id；Script Writer 必须按 case_id 注入到 Python 测试函数装饰器/注释；编译期校验对应关系 |
| **Java 与 Python 跨语言协作复杂度** | gRPC + protobuf 严格契约；Python Runner 独立部署独立扩缩容；通过版本号管理协议演进 |
| **Playwright for Java 与 Python 行为差异** | 采集与执行使用同一份 Playwright 版本号；关键 API（locator、wait、screenshot）有跨语言适配测试用例 |
| LangChain4j 相对年轻 | 对 LLM 调用做一层薄封装接口，必要时可降级到原生 HTTP 调用 |

---

## 4. 项目路线图与验收

### 4.1 阶段划分

#### 阶段 0：技术预研（2 周）

**目标**：跑通一条最小链路，验证可行性。

- 选定 1 个国内 LLM + 1 个国内 VLM，完成可用性评估；
- Playwright 抓取 DOM + 截图，调用 VLM 输出 UI 描述；
- 单页面、单条用例、单脚本，手跑通；
- 不做 API、不做存储、不做并发。

**交付物**：技术预研报告 + demo 脚本。

**验收标准**：
- ✅ 给定 1 个示例 URL（如登录页），能输出至少 5 条功能用例与对应 Playwright 脚本；
- ✅ 脚本至少 80% 可直接跑过。

#### 阶段 1：MVP（4–6 周）

**目标**：跑通完整闭环，单租户内部可用。

- 完整四个子 Agent；
- REST API（仅 `/tasks` 提交 + 查询）；
- 支持账号密码登录；
- 多页面清单（同任务内串行处理即可）；
- 自动验证 + 自修复（修复次数固定 3 次）；
- 用例 + 脚本写文件输出，简单存到本地文件系统 + H2/PostgreSQL 单实例（用 PostgreSQL 而非 SQLite，避免 Java 端额外引入嵌入式数据库迁移成本）。

**交付物**：可独立部署的服务（docker-compose 起）+ API 文档 + 使用手册。

**验收标准**：
- ✅ 输入 3 个真实业务页面（含 1 个需登录），15 分钟内输出用例文档与 Playwright Python 脚本；
- ✅ 用例总数 ≥ 30，其中异常/边界用例占比 ≥ 30%；
- ✅ 自动验证脚本通过率 ≥ 70%（经自修复后）；
- ✅ QA 抽查 20 条用例，主观可用率 ≥ 60%。

#### 阶段 2：V1 正式版（6–8 周）

**目标**：用例可管理、可追溯、可协作。

- 完整 REST API（含用例审核、执行历史、重跑）；
- 用例版本管理与变更检测；
- 人工校验闭环 + 反馈回流到 prompt；
- 执行历史与报告（按用例查询时间线）；
- 多页面并发处理；
- Postgres + 对象存储（MinIO/S3）替换本地文件存储；
- 鉴权（API Key）+ 多任务隔离。

**交付物**：生产就绪的服务 + 运维手册 + 监控看板。

**验收标准**：
- ✅ 任意页面重生成时，能正确输出新增/删除/修改用例清单；
- ✅ 人工标记"拒绝"的用例，下次生成时同类用例占比下降；
- ✅ 同时处理 5 个任务（每任务 10 页面）不丢任务、不超时；
- ✅ 服务 SLA：可用性 ≥ 99%；P95 单页面处理时间 ≤ 3 分钟。

#### 阶段 3：V2 增强（持续迭代，按需）

候选方向（按反馈优先级排）：

- 兼容性测试（多浏览器、多分辨率）；
- 视觉回归（截图基线对比）；
- 用户旅程级用例（跨页面、跨任务的串行场景）；
- 与 TestRail / 禅道 / Xray 等用例平台对接；
- Web 可视化界面（任务提交 + 用例审核）；
- 模型微调（用积累的反馈样本训练专用 case-generation 模型）。

### 4.2 总体里程碑

| 里程碑 | 时间（相对启动） | 标志 |
| --- | --- | --- |
| M0：立项评审通过 | T0 | 本文档评审通过 |
| M1：技术预研收敛 | T0 + 2w | 选型敲定，demo 跑通 |
| M2：MVP 上线 | T0 + 8w | 内部可用 |
| M3：V1 正式发布 | T0 + 16w | 生产可用 |
| M4：V2 启动 | T0 + 16w 之后 | 根据使用反馈规划 |

### 4.3 度量指标（持续观测）

- **生成质量**：人工抽样可用率、异常用例占比、误报率；
- **稳定性**：脚本一次通过率、自修复后通过率、修复轮次均值；
- **效率**：单页面端到端耗时、并发吞吐；
- **成本**：单页面 LLM token 消耗、单页面综合成本（¥）；
- **闭环效果**：被拒用例数、相同模式被拒次数随版本下降速度。

---

## 5. 待进一步明确的问题（遗留项）

以下问题不阻塞立项，需在阶段 0 / 阶段 1 启动前解决：

1. 国内 LLM 与 VLM 的最终选型，需在阶段 0 完成横评（LangChain4j 对各厂商兼容度同步验证）；
2. 用例的具体 Schema（字段、枚举值）需在阶段 0 输出 v1 定义；
3. 自修复策略的细化规则（何时升级为真实 Bug）需在阶段 1 早期定标；
4. 数据安全与合规：凭证、PRD、截图的存储与销毁策略；
5. 团队分工与外部依赖（Playwright runner 机房资源、LLM 配额、Java 与 Python 镜像基线）；
6. Java ↔ Python Runner 的 gRPC/REST 契约 v1：脚本下发格式、执行结果回传 schema、超时与取消语义；
7. Playwright for Java 与 Playwright Python 的版本对齐策略（建议同主版本，差异行为需单测覆盖）。

---

*—— 文档结束 ——*
