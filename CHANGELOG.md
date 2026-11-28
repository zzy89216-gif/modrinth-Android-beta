# 变更日志

本项目所有重要变更记录于此。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]（2026-11-28 大重构，beta）

### 新增

- **离线本地账户系统**：纯本地多账户（PIN 以 SHA-256 摘要存储，不存明文）；当前登录 id 存
  DataStore Preferences，账户列表（含 PIN 摘要与收藏）存 `accounts.json`（Gson）；读-改-写
  在 IO 协程 + Mutex 串行化；用例：注册 / 登录 / 登出 / 收藏切换 / 读取收藏。
- **下载管理器**：`DownloadService`（**dataSync 前台服务**，切后台 / 锁屏继续下载）+
  `DownloadManager` 进程内状态机（任务列表 StateFlow）；OkHttp 流式下载，进度通知带
  「暂停 / 取消」操作；**SHA-512 流式校验**（续传时旧 `.part` 字节先喂进 digest 保证整体正确）；
  支持**暂停 / 续传（HTTP Range + `.part` 临时文件）/ 取消 / 重试**；Android 13+ 启动时申请
  `POST_NOTIFICATIONS`，拒绝则降级不崩溃。
- **键位映射系统**（移植自 ZalithLauncher2 InputMap 模块，GPL-3.0，见根目录 `NOTICE`）：
  GLFW 键码表 / 键名表、Minecraft 默认键位、Android 键位映射、`KeymapRepository`
  （`keymap.json` 持久化）、重绑定 UI（组合键 / 长按捕获、一键重置默认）。
- **CardGrid 卡片网格组件**（移植自 ZalithLauncher2，GPL-3.0）：11 个文件落地
  `ui/components/cardgrid/`（引擎 / 模型 / 状态 / UI），`CardGrid.kt` 移除 Material Expressive
  `motionScheme` 依赖，改用内置弹簧动画（Compose BOM 2024.09.00 尚不可用）。
- **ColorPicker 颜色选择器组件**（移植自 ZalithLauncher2，GPL-3.0）：9 个文件落地
  `ui/components/colorpicker/`。
- **下载 / 渲染配套**：`CoilModule`（Hilt 单例 Coil 2.7.0 ImageLoader：memoryCache 50MB /
  diskCache 128MB / crossfade 200ms；coil-compose 2.7.0 无 singleton 自动发现，
  `AsyncImage` 需显式传 `imageLoader`）；`ProjectCard` 固定行高（76dp）列表卡片；
  `DownloadModule` Hilt 绑定。
- 数据层新 DTO（字段对照上游 labrinth `models/v2/*.rs`）：`SearchResponse`
  （`{hits, offset, limit, total_hits}` 包装）、`SearchProjectResponse`、`CategoryResponse`、
  `VersionResponse`、`VersionFileResponse`（`{url, filename, primary, size, hashes:{sha1, sha512}}`）、
  `VersionDependencyResponse`、`ProjectFileResponse`（下载重定向 302 的 `{"url": ...}` JSON 兜底）。
- 领域层：`SearchResult` / `SearchProject` / `Version` / `VersionDependency` / `Category` /
  `License` / `GalleryItem` / `DonationUrl` / `File` 模型；`ProjectRepository` 新增
  `searchProjects(offset 分页): SearchResult`、`getProjectVersions`、`getCategories(): List<Category>`、
  `getFileDownloadUrl`（302 直链 + 404 回退取版本列表 primary 文件 url）；
  用例：`SearchProjectsUseCase.executePaged`、`GetProjectVersionsUseCase`、
  `GetCategoriesUseCase`、`GetFileDownloadUrlUseCase` 及账户 5 用例。

### 变更

- **注解处理 kapt → KSP**：全量切换到 **KSP 2.0.0-1.0.22**（kapt 在 Kotlin 2.0 下崩溃：
  `NonExistentClass cannot be converted to Annotation`）；所有 `kapt(...)` 依赖改为 `ksp(...)`。
- **Hilt 2.52 → 2.56.2**（经 2.53.1 过渡；2.53.1 仍不够，2.56.2 才适配 Kotlin 2.0 工具链）。
- **单架构构建**：`ndk.abiFilters` 只发 **arm64-v8a**（APK 体积与构建时间下降；x86 / 32 位设备不可用）。
- **数据层对齐真实 Modrinth API**（重写，字段全部对照上游 labrinth Rust 结构体）：
  - 搜索：`GET /v1/search`，返回 `{hits, offset, limit, total_hits}`（旧代码按裸数组解析是错的）；
  - 分类：`GET /v1/tag/category`（旧 `GET /v1/category` 是错的），返回对象数组
    `{name, icon, project_type, header, project_count?}`；
  - 版本：`GET /v1/project/{id}/version`（版本对象数组，`files[]` 每项含
    `hashes.{sha1, sha512}` / `filename` / `primary` / `size` / `url`）；
  - 下载直链：302 重定向到 CDN（`Location` 头），302 同时带 `{"url": ...}` JSON 体兜底；
  - 项目详情 `ProjectResponse` 重写：无 `author` / `logo_url`（图标是 `icon_url` / `raw_icon_url`），
    关注数是 `followers`（`follows` 只在搜索项），`license` 是对象，`body` 才是长描述。
- **UI 重构（进行中）**：底部导航三 tab（首页 / 分类 / 我的）+ 搜索 + 项目详情；深色主题
  （主色 `#6C5CE7`，见 `res/values/colors.xml`）；路由：home / categories / mine /
  search / project_detail。
- `Project` 领域模型重写：补 `slug` / `iconUrl` / `followers` / `body` / `license` 等，
  `author` 改为可空（详情接口不返回该字段）。
- `MainActivity`：13+ 启动时申请通知权限（`registerForActivityResult`）。
- AndroidManifest：新增 `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` /
  `POST_NOTIFICATIONS` 权限与 `DownloadService`（`foregroundServiceType="dataSync"`）声明。
- 应用标识更新：主题资源改用 `modbox_*` 颜色命名。

### 修复

- kapt 在 Kotlin 2.0 下的注解处理崩溃（`NonExistentClass cannot be converted to Annotation`，
  残留废弃接口 `ModrinthApi.kt` 触发）→ 删除废弃接口 + 整体迁移 KSP。
- 旧 `ModrinthApiImpl` 的 `/v1/project/search`、`/v1/category` 路径与返回类型均与真实 API 不符
  → 接口 / 实现按 `API_REFERENCE.md` 对齐（收尾见 HANDOFF.md）。

### 文档

- 7 份根文档全部重写对齐新现实（README / HANDOFF / CHANGELOG / PROJECT_PLAN /
  API_REFERENCE / TECHNICAL_DOC / CONTRIBUTING）。
- 新增根目录 `NOTICE`：ZalithLauncher2（GPL-3.0）移植文件清单与许可条款说明。
- 新增调研文档：`docs/保活与渲染调研.md`（前台服务 / 厂商自启动 / Compose 渲染）、
  `docs/zl2调研.md`（键位 / 渲染器移植可行性）。

### 待办

- `ModrinthApiImpl` / `ProjectRepositoryImpl` 新端点收尾（在途，见 HANDOFF.md「四」）。
- `@HiltAndroidApp` 导入更正（`dagger.hilt.android.HiltAndroidApp`，见 HANDOFF 踩坑 #15）。
- navigation 2.8.0 `navArgument` 包名、Material3 1.3 `TopAppBar` 的 `@file:OptIn`（在途）。
- 云端构建变绿 + 真机运行验证。
- 二期：WorkManager + Room 落库实现跨进程续传；键位宿主接入。

---

## [0.2.0] - 2026-11-27

### 新增

- 搜索页接通真实 Modrinth API（含加载态、错误提示、搜索按钮）
- `SearchProjectsUseCase` 领域用例
- `AppUseCaseModule`：Hilt 绑定 UseCase
- CI 拆分为 3 个工作流：`build.yml`（main 构建产 APK）、`pr-check.yml`（PR 编译检查）、
  `release.yml`（tag 触发 Release）
- 交接文档 `HANDOFF.md`
- 议题模板统一进 `ISSUE_TEMPLATE.md`（模板 A/B/C + 空白仅维护者）
- 上游仓库克隆：`modrinth-code`、`modrinth-docs`（工作区内只读副本）

### 修复

- **构建配置**
  - 移除失效的 `android-actions/setup-android` 步骤
  - 删除根项目 `allprojects.repositories` 与 settings 的 `FAIL_ON_PROJECT_REPOS` 冲突
  - `proguardFiles getDefaultProguardFile(...)` 补外层括号（Kotlin DSL 语法）
  - domain 模块插件由 `kotlin.android` 改为 `kotlin.jvm`
  - Compose 改用 `org.jetbrains.kotlin.plugin.compose`（Kotlin 2.0 要求），移除旧 `composeOptions`
  - data 模块补 `org.jetbrains.kotlin.kapt` 插件
  - 移除废弃的 `android.enableR8` 配置
- **源码**
  - Navigation 导入改为 `androidx.navigation.compose.*`
  - `MainActivity` 加 `@AndroidEntryPoint`，改用 `rememberNavController`
  - `DataModule` 改为标准 Hilt abstract 模块 + companion `@Provides`
    （原写法 `dagger.provisioning.ProvidesBinding` 不存在）
  - `ModrinthApiImpl` 补 `retrofit2.http.Path` 导入
  - `ProjectDetailScreen` / `CategoriesScreen` 补 `androidx.compose.ui.Alignment` 导入
  - `ProjectDetailScreen` 增加 `LaunchedEffect` 触发加载
  - `HomeScreen` 移除未引入依赖的 icons 用法，改用 TextButton
  - `themes.xml` 状态栏颜色改为已定义资源

---

## [0.1.0] - 2026-11-27

### 新增

- 项目初始化：Gradle 多模块（app / core / domain / data）
- Kotlin + Jetpack Compose + Hilt + Navigation 骨架
- 4 个页面骨架：Home、Search、ProjectDetail、Categories
- Modrinth API 封装（Retrofit）：`/v1/project/{id}`、`/v1/project/search`、`/v1/category`
  （后两者路径与结构在 2026-11-28 重构中修正，见上方 [Unreleased]）
- `Project` 领域模型与 `ProjectRepository` 接口
- 首批文档：README、PROJECT_PLAN、CHANGELOG、CONTRIBUTING、TECHNICAL_DOC、
  DEPLOYMENT_GUIDE、API_REFERENCE
- GitHub Actions 基础构建工作流
- 应用图标（自适应图标）与主题资源
