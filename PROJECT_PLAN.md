# 项目规划

> 详细状态与卡点见 [HANDOFF.md](HANDOFF.md)。

## 一、项目目标

基于 Modrinth 官方 REST API，做原生 Android 客户端：
浏览/搜索模组 → 查看详情 → 下载 →（后续）安装管理与账户同步。

**非逐行移植**：上游是 Rust + TS monorepo，本项目是按 API 重写的原生实现。

## 二、阶段规划与状态

### Phase 1：基础框架 ✅ 已完成
- [x] Gradle 多模块结构
- [x] Kotlin 2.0 + Compose + Hilt + Navigation
- [x] MVVM 分层与 UseCase
- [x] CI 工作流（3 个）

### Phase 2：核心页面与 API 🔄 进行中
- [x] 4 个页面骨架
- [x] API 封装与 Repository
- [x] 搜索页接通真实 API
- [ ] 云端 APK 构建通过 ← **当前阻塞点**
- [ ] 数据层字段对齐真实响应（搜索包装、分类结构）
- [ ] 列表跳详情
- [ ] Coil 图片加载
- [ ] 下拉刷新 + 分页
- [ ] 空态 / 错误态

### Phase 3：用户与本地存储 ⏳ 未开始
- [ ] 账户登录（Modrinth OAuth / PAT）
- [ ] 收藏、关注
- [ ] 下载管理器（断点续传、校验 SHA-512）
- [ ] Room 离线缓存

### Phase 4：高级功能 ⏳ 未开始
- [ ] 模组安装/卸载辅助
- [ ] 评论与评分
- [ ] 通知
- [ ] 主题切换、多语言

## 三、API 参考

官方文档：https://docs.modrinth.com/api/

| 用途 | 接口 | 备注 |
|------|------|------|
| 项目详情 | `GET /v1/project/{id_or_slug}` | 返回 Project 对象 |
| 搜索 | `GET /v1/search?query=&limit=&offset=` | **返回 `{hits:[...]}`，非数组** |
| 项目搜索（旧） | `GET /v1/project/search` | 同 search，结构一致 |
| 分类标签 | `GET /v1/tag/category` | 返回 `[{name, ...}]` |
| 版本列表 | `GET /v1/project/{id}/version` | 下载用 |
| 认证 | `POST /v1/auth` | 或使用 PAT |

> ⚠️ 当前代码里 `/v1/category` 与搜索返回结构可能与真实 API 不符，见 HANDOFF.md。

## 四、依赖选型

| 用途 | 选型 |
|------|------|
| UI | Jetpack Compose + Material3 |
| 网络 | Retrofit + OkHttp + Gson |
| 图片 | Coil（待接入） |
| DI | Hilt 2.52 |
| 存储 | Room + DataStore（待接入） |
| 异步 | Coroutines + StateFlow |

## 五、风险

1. **API 结构差异**：手写模型易与真实响应不符，需以真实接口/上游 labrinth 为准。
2. **构建环境**：仅云端构建，反馈周期长；已改为用 API 直接读日志提速。
3. **下载与校验**：文件校验（SHA-512）、存储权限与 Android 分区存储限制。
4. **合规**：遵守 Modrinth API 使用规范与限流（未认证 60/min 量级）。
