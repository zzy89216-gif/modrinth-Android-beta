# 项目规划

> 详细状态、CI 卡点与在途工作见 [HANDOFF.md](HANDOFF.md)。
> 更新：2026-11-28（大重构后刷新各 Phase 状态）。

## 一、项目目标

**ModBox（模组浏览器）**：基于 Modrinth 官方 REST API 的原生 Android 客户端 ——
浏览 / 搜索模组 → 查看详情 → 下载（校验 / 续传）→ 离线账户与收藏 → 键位映射（Phase 2.5 接宿主）。

**非逐行移植**：上游是 Rust + TS monorepo（无现成 Android 代码），本项目是按 API 重写的原生实现；
字段以 `upstream/modrinth-code/apps/labrinth`（Rust 结构体，只读）为准，
交互参考 `apps/app-frontend`。键位 / 卡片组件等第三方移植来自 ZalithLauncher2（GPL-3.0，见 NOTICE）。

## 二、阶段规划与状态

### Phase 1：基础框架 ✅ 已完成

- [x] Gradle 多模块结构（app / core / domain / data）
- [x] Kotlin 2.0.0 + Compose（BOM 2024.09.00）+ Navigation
- [x] MVVM 分层与 UseCase
- [x] CI 工作流（3 个：build / pr-check / release，纯云端构建）

### Phase 2：核心页面与 API ✅ 大部分完成（2026-11-28 重构后）

- [x] 页面骨架 + 底部导航路由（home / categories / mine / search / project_detail）
- [x] API 封装与 Repository（接口 + 模型全量对齐真实 API，见 API_REFERENCE.md）
- [x] 搜索页接通真实 API（`GET /v1/search` 的 `{hits,...}` 包装，含加载 / 错误态）
- [x] 分类页 `GET /v1/tag/category`（旧 `/v1/category` 已废弃）
- [x] 项目详情（字段对齐 labrinth `models/v2/*.rs`）
- [x] Coil 2.7.0 图片加载（Hilt 单例 ImageLoader，50MB 内存 + 128MB 磁盘缓存）
- [x] 下载链路：版本列表 → 下载直链（302 到 CDN）→ 下载管理器
      （dataSync 前台服务、进度通知、SHA-512 校验、暂停 / 续传 / 取消）
- [x] 离线本地账户（PIN SHA-256）+ 收藏
- [x] 单架构（arm64-v8a）构建；KSP 2.0.0-1.0.22 + Hilt 2.56.2
- [🔄] Retrofit 接口 / 仓储实现收尾（在途，见 HANDOFF.md「四」）
- [🔄] 三 tab UI 与「我的」页（ui-agent 在途）
- [ ] 云端构建变绿 + 真机验证
- [ ] 下拉刷新 + 分页（数据层已支持 offset 分页，UI 未接）
- [ ] 空态 / 错误态页

### Phase 2.5：渲染 / 保活优化 + 键位宿主接入 🆕（新增，未开始）

> 背景：Phase 2 重构把下载与列表渲染的关键项落地了（dataSync 前台服务、Coil 缓存策略、
> 固定行高卡片），调研结论分别在 `docs/保活与渲染调研.md` 与 `docs/zl2调研.md`；
> 本阶段做剩余优化与键位能力的「最后一公里」。

- [ ] **渲染 / 保活优化**
  - [ ] Macrobenchmark 基线（首帧 / 滚动帧时间 / jank 率），按 `docs/保活与渲染调研.md` 结论排查
  - [ ] 列表滚动 jank 专项（重组 / 大图解码 / stable key 验证）
  - [ ] 厂商电池优化文案与引导（不做程序化跳转，仅文案 + 手动引导）
- [ ] **键位宿主接入**
  - [ ] 接入一个可消费键位的宿主（游戏进程或调试容器），验证 `keymap` 包端到端可用
  - [ ] 键位变更实时下发（宿主侧监听 `keymap.json` 变更）
  - [ ] （可选）zl2 渲染器「配置层 + 选择 UI + 线程 / 生命周期模型」复用评估
        （依据 `docs/zl2调研.md`，native / 游戏进程绑定部分整体不搬）

### Phase 3：用户与本地存储 ✅ 大部分完成（原 Phase 3 内容已在 2026-11-28 提前落地）

- [x] 离线本地账户系统（PIN SHA-256，DataStore + JSON；纯本地，无 OAuth / PAT 登录）
- [x] 收藏（账户内项目收藏，`ToggleFavoriteUseCase`）
- [x] 下载管理器（SHA-512 校验、暂停 / 续传 / 取消、dataSync 前台服务保活）
- [x] Room 依赖已预置（2.6.1，ksp room-compiler）
- [ ] Room 离线缓存（项目 / 版本缓存；下载记录落库留二期，与跨进程续传一起做）
- [ ] 关注（上游 `POST /project/{id}/follow`，需认证；当前版本不做联网账户）

### Phase 4：高级功能 ⏳ 未开始

- [ ] 模组安装 / 卸载辅助
- [ ] 评论与评分
- [ ] 通知（Modrinth 站内通知，需认证）
- [ ] 主题切换（当前固定深色 + 主色 #6C5CE7）、多语言
- [ ] 正式发布签名（release 当前为 debug 签名 prerelease）

## 三、API 参考

详见 [API_REFERENCE.md](API_REFERENCE.md)（按真实接口重写，含旧代码错误对照）。

| 用途 | 接口 | 备注 |
|------|------|------|
| 搜索 | `GET /v1/search?query=&offset=&limit=` | **返回 `{hits, offset, limit, total_hits}`，不是裸数组** |
| 项目详情 | `GET /v1/project/{id_or_slug}` | 返回 Project 对象（无 `author` / `logo_url`） |
| 分类标签 | `GET /v1/tag/category` | 返回对象数组（**不是** 旧的 `/v1/category`） |
| 项目版本列表 | `GET /v1/project/{id}/version` | 版本对象数组，`files[]` 含 `hashes.{sha1, sha512}` |
| 下载直链 | 302 重定向到 CDN | 302 带 `{"url": ...}` JSON 兜底；实现见 `getFileDownloadUrl` |
| 认证 | PAT：`Authorization: Bearer <token>` | 当前版本只用离线账户，联网认证留 Phase 4 |

## 四、依赖选型

| 用途 | 选型 | 版本 |
|------|------|------|
| UI | Jetpack Compose + Material3 | BOM 2024.09.00 |
| 注解处理 | KSP（kapt 在 Kotlin 2.0 下崩溃） | 2.0.0-1.0.22 |
| DI | Hilt | 2.56.2 |
| 网络 | Retrofit + OkHttp + Gson | 2.11.0 / 4.12.0 |
| 图片 | Coil | 2.7.0 |
| 存储 | Room + DataStore Preferences | 2.6.1 / 1.1.1 |
| 异步 | Coroutines + StateFlow | 1.9.0 |

## 五、风险

1. **API 结构差异**：已通过对照 labrinth Rust 结构体重写模型消除大头；剩余风险是
   `/v1` 兼容层行为细节（见 API_REFERENCE「旧代码错误对照」）。
2. **构建环境**：仅云端构建（arm64-v8a 单架构），反馈周期长；用 API 直接读 CI 日志提速
   （方法见 HANDOFF.md 第三节）。
3. **保活与厂商差异**：Doze / 厂商自启动策略可能中断下载；对策与厂商白名单说明见
   `docs/保活与渲染调研.md`（TECHNICAL_DOC.md 有摘要）。
4. **下载与校验**：SHA-512 已做流式校验；跨进程续传未做（二期）；存储走 App 私有目录
   （`filesDir`），不涉及分区存储授权。
5. **合规**：GPL-3.0 移植代码须保留 NOTICE 与文件头声明（见 NOTICE）；遵守 Modrinth API
   使用规范与限流（未认证约 60 请求/分钟量级）。
