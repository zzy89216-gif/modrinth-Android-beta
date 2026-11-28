# 技术文档

> 更新：2026-11-28（大重构后：KSP 迁移、数据层重写、账户 / 下载 / 键位子系统、
> 单架构构建）。CI 细节见 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)，
> 保活与渲染的完整调研见 [docs/保活与渲染调研.md](docs/保活与渲染调研.md)。

## 一、模块与包结构

```
app (com.android.application, namespace com.modrinth.android)
  ├── core    (android.library, 基本为空/预留)
  ├── domain  (kotlin.jvm 纯 JVM，无 Android 依赖)
  └── data    (android.library, namespace com.modrinth.data)
        ├── domain
        └── core
```

依赖方向：`app → data → domain`，`domain` 不反向依赖任何层。

| 模块 / 包 | 职责 | 关键类 |
|-----------|------|--------|
| `app` · 根 | 入口 | `ModrinthApplication`（`@HiltAndroidApp`）、`MainActivity`（13+ 通知权限申请） |
| `app` · `ui/` | Compose 页面与导航 | `home/`、`search/`、`categories/`、`project/`、`navigation/`（`Screen`、`NavGraph`）、`theme/` |
| `app` · `ui/components/` | 通用组件（zl2 移植，GPL-3.0） | `cardgrid/`（11 文件：engine / model / state / ui）、`colorpicker/`（9 文件） |
| `app` · `service/` | **下载管理器**（dataSync 前台服务） | `DownloadManager`（状态机）、`DownloadService`（执行器 + 通知）、`DownloadItem`、`ProjectCard`（固定行高列表卡片） |
| `app` · `keymap/` | **键位映射**（zl2 移植，GPL-3.0） | `GlKeycodes`、`GlKeyNames`、`KeymapConfig`、`AndroidKeyMapper`、`KeymapRepository`、`KeymapViewModel`、`KeymapScreen` |
| `app` · `di/` | 应用级 Hilt 模块 | `AppUseCaseModule`、`CoilModule`、`DownloadModule` |
| `data` · `api/` | Retrofit 接口 + DTO | `ModrinthApiImpl`、`model/`（`ProjectResponse`、`SearchResponse`、`VersionResponse` 等 12 个 DTO） |
| `data` · `repository/` | 仓储实现 | `ProjectRepositoryImpl` |
| `data` · `account/` | **离线本地账户** | `AccountRepositoryImpl`（PIN SHA-256 + DataStore + `accounts.json`）、`AccountModule` |
| `data` · `di/` | 数据层 Hilt 模块 | `DataModule`（Retrofit 单例 + Repository 绑定） |
| `domain` · `model/` | 领域模型（纯 JVM） | `Project`、`SearchResult`、`Version`、`Category`、`License`、`File` 等 |
| `domain` · `repository/` | 仓储接口 | `ProjectRepository` |
| `domain` · `usecase/` | 用例 | `GetProjectUseCase`、`SearchProjectsUseCase`、`GetProjectVersionsUseCase`、`GetCategoriesUseCase`、`GetFileDownloadUrlUseCase` |
| `domain` · `account/` | 账户接口 / 用例 | `AccountRepository`、`LocalAccount`、`RegisterUseCase`、`LoginUseCase`、`LogoutUseCase`、`ToggleFavoriteUseCase`、`GetFavoritesUseCase` |
| `core` | 预留通用组件 | — |

## 二、架构分层

```
UI (Compose Screen, 三 tab：首页/分类/我的 + 搜索 + 详情)
  ↓ 收集 StateFlow
ViewModel (HiltViewModel + viewModelScope)
  ↓ 调用
UseCase (domain)
  ↓ 调用接口
ProjectRepository / AccountRepository (domain 接口)
  ↑ 实现
ProjectRepositoryImpl / AccountRepositoryImpl (data)
  → ModrinthApiImpl (Retrofit + Gson)
  → DataStore Preferences + filesDir JSON（账户）

独立子系统（app 层）：
  DownloadManager(状态机) ⇄ DownloadService(dataSync 前台服务, OkHttp 流式下载)
  keymap(KeymapConfig ⇄ KeymapRepository, keymap.json)
```

- ViewModel 只依赖 UseCase / 注入的 Manager，不直接接触 Retrofit
- `domain` 保持纯 Kotlin/JVM，便于单测
- 下载与键位是 app 层子系统（涉及 Service / 通知 / UI），不进 domain

## 三、依赖注入（Hilt 2.56.2 + KSP）

| 模块 | 提供 |
|------|------|
| `DataModule`（data） | `ModrinthApiImpl`（Retrofit 单例）、`ProjectRepository` ← `ProjectRepositoryImpl`（`@Binds`） |
| `AccountModule`（data） | `DataStore<Preferences>`（文件 `datastore/accounts.preferences_pb`）、`AccountRepository` 绑定、5 个账户 UseCase |
| `AppUseCaseModule`（app） | `GetProjectUseCase`、`SearchProjectsUseCase` |
| `CoilModule`（app） | Coil `ImageLoader` 单例 |
| `DownloadModule`（app） | `DownloadManager` 单例 |

注意：

- 注解处理用 **KSP**（`com.google.devtools.ksp` 2.0.0-1.0.22，与 Kotlin 2.0.0 配套）；
  kapt 在 Kotlin 2.0 下崩溃（`NonExistentClass cannot be converted to Annotation`），已全部移除
- 依赖声明：`implementation("com.google.dagger:hilt-android:2.56.2")` +
  `ksp("com.google.dagger:hilt-compiler:2.56.2")`（app、data 两模块）
- 模块必须是 `abstract class` + `companion object` 放 `@Provides`（object 不能有 abstract 方法），
  并标 `@InstallIn(SingletonComponent::class)`
- `MainActivity` 用 `@AndroidEntryPoint`；`ModrinthApplication` 用 `@HiltAndroidApp`，
  正确导入是 **`dagger.hilt.android.HiltAndroidApp`**（`com.google.dagger` 是 Maven groupId，不是包名）

## 四、版本矩阵（必须保持一致，以 build.gradle.kts 为准）

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | CI 用 temurin 17 |
| Gradle | 8.7 | wrapper 固定 |
| AGP | 8.6.0 | 根 `build.gradle.kts` |
| Kotlin | 2.0.0 | android / jvm / compose 插件同版本 |
| **KSP** | **2.0.0-1.0.22** | 与 Kotlin 2.0.0 配套；hilt-compiler / room-compiler 都走 `ksp(...)` |
| Compose 编译器 | `org.jetbrains.kotlin.plugin.compose` 2.0.0 | Kotlin 2.0 起必须用插件，不能用 `composeOptions` |
| Compose BOM | 2024.09.00 | 带出 Material3 1.3（`TopAppBar` 为实验性 API，需 `@OptIn`） |
| **Hilt** | **2.56.2** | 2.53.1 仍不兼容 Kotlin 2.0 工具链，2.56.x 起才适配 |
| Retrofit / OkHttp | 2.11.0 / 4.12.0 | converter-gson 同版本 |
| **Coil** | **2.7.0** | `coil-compose`；注意该依赖不带 singleton 自动发现（见下文） |
| **Room** | **2.6.1** | runtime + ktx + `ksp(room-compiler)`；当前版本尚未建表（二期离线缓存） |
| **DataStore** | **1.1.1** | `datastore-preferences`，本地账户当前登录 id |
| Navigation | 2.8.0 | `navigation-compose` |
| Coroutines | 1.9.0 | |
| minSdk / target / compile | 26 / 34 / 34 | Android 8.0+ |
| ABI | **arm64-v8a 单架构** | `ndk.abiFilters`；APK 更小，x86 / 32 位设备不可用 |

## 五、网络层

基础地址：`https://api.modrinth.com/`（`DataModule` 内 Retrofit.Builder + GsonConverterFactory）。

已定义 / 待收尾的接口（`ModrinthApiImpl`，完整形状见 [API_REFERENCE.md](API_REFERENCE.md)）：

| 方法 | 路径 | 返回 |
|------|------|------|
| `getProject` | `GET v1/project/{project_id}` | `ProjectResponse` |
| `searchProjects` | `GET v1/search` | `SearchResponse`（`{hits, offset, limit, total_hits}` 包装，非裸数组） |
| `getCategories` | `GET v1/tag/category` | `List<CategoryResponse>` |
| `getProjectVersions` | `GET v1/project/{id}/version` | `List<VersionResponse>` |
| 下载直链 | 302 重定向到 CDN（302 带 `{"url": ...}` JSON 兜底体） | `ProjectFileResponse` |

> 旧端点（`/v1/project/search` 裸数组、`/v1/category` 字符串数组）已废弃，
> Retrofit 接口与仓储实现的最后收尾见 HANDOFF.md「四、代码现状」。

## 六、数据流示例：搜索

```
用户输入 → SearchScreen 点击搜索
  → SearchViewModel.search(query)
    → SearchProjectsUseCase.executePaged(query, offset, limit)
      → ProjectRepositoryImpl.searchProjects()
        → ModrinthApiImpl.searchProjects()  (Retrofit)
          → SearchResponse 解析 → mapToProject() DTO → 领域模型
    → 更新 _searchResult / _isLoading / _error (StateFlow)
  → Compose 重组渲染列表 / 加载态 / 错误提示
```

## 七、子系统技术要点

### 下载管理器（app/service）

- **`DownloadService`**：`android:foregroundServiceType="dataSync"` 前台服务
  （权限 `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC`；minSdk 26 无需 property 白名单）。
  `onCreate` 起一个主循环协程收集 `DownloadManager.items`：QUEUED→RUNNING 就起协程跑
  OkHttp 流式下载，状态离开 RUNNING 就取消对应协程（暂停即停流，`.part` 保留续传）。
- **`DownloadManager`**：进程内单例状态机（`@Provides` 于 `DownloadModule`），
  任务列表 `StateFlow<List<DownloadItem>>`；API：enqueue / pause / resume / cancel / retry / clearFinished。
- **校验**：SHA-512 流式计算（期望值来自版本文件的 `hashes.sha512`，hex 小写）；
  续传时先把 `.part` 已有字节喂进 digest，保证整体哈希正确；失败置 FAILED 并报「sha512 校验失败」。
- **续传**：HTTP Range 请求 + `.part` 临时文件；完成后 rename 为正式文件名。
- **通知**：同一通知 id 两态——有活动任务时 ongoing + 进度条 + 「暂停 / 取消」操作；
  全部结束时一次性通知（autoCancel），服务退出前台并自毁。
- **边界**：进程存活期间切后台 / 锁屏可继续（前台服务保活）；**进程被杀后状态丢失**，
  跨进程续传（WorkManager + Room 落库）是二期（调研结论见保活文档「结论清单」第 9 条）。

### 离线本地账户（domain/account + data/account）

- **纯本地**：无任何网络 / 服务器交互。当前登录 id → DataStore Preferences；
  账户列表（id / 用户名 / 显示名 / 头像色 / **PIN 的 SHA-256 hex 摘要** / 收藏列表）→
  `filesDir/accounts.json`（Gson 序列化）。
- PIN 不存明文；验证 = 重新算 SHA-256 后比对摘要。
- 所有读-改-写在 `Dispatchers.IO` + `Mutex` 下串行，避免并发 toggleFavorite 互相覆盖；
  写操作后 `accountsRevision` 自增，与 DataStore 流 combine，收藏变更 / 重新登录都会
  重新发射 `observeCurrentAccount()`。

### 键位映射（app/keymap，GPL-3.0 移植）

- 数据链：`KeymapConfig`（默认 Minecraft 键位 + 用户绑定）⇄ `KeymapRepository`
  （`filesDir/keymap.json`，Android 自带 `org.json` 实现——app 模块无 Gson 依赖，
  避免新增未审批三方依赖；接口与 Gson 版本签名一致，日后易切回）。
- `AndroidKeyMapper` 提供 Android keycode → 逻辑键位映射；
  `KeymapScreen` / `KeymapViewModel` 提供重绑定 UI（组合键 / 长按捕获、一键重置默认）。
- **当前无宿主游戏消费键位**（宿主接入见 PROJECT_PLAN Phase 2.5）；
  移植范围与源文件对照见根目录 NOTICE 与 `docs/zl2调研.md`。

### 图片与渲染（Coil + Compose）

- `CoilModule` 提供 Hilt 单例 `ImageLoader`：memoryCache 50MB（解码后 bitmap 驻留，
  滚动不二次解码）、diskCache 128MB（原始响应缓存）、crossfade 200ms。
- **Coil 2.7.0 注意**：`io.coil-kt:coil-compose:2.7.0` 不带 `coil-singleton` 包，
  没有 ImageLoaderFactory 自动发现，`AsyncImage` 必须显式传 `imageLoader` 参数；
  Coil 2.x 已移除 BitmapPool，不要再配置。
- 列表性能：`ProjectCard` 固定行高（76dp）+ stable data（data class structural equality），
  LazyColumn 预布局免测量；不锁 120Hz，jank 优先排查重组与大图解码。
- 完整依据：[docs/保活与渲染调研.md](docs/保活与渲染调研.md)（「Compose 渲染」与「结论清单」）。

## 八、保活策略摘要（详见 docs/保活与渲染调研.md）

| 策略 | 做法 | 状态 |
|------|------|------|
| 前台服务类型 | 下载用 **dataSync** 前台服务（Android 9+ 声明、14 起强制校验 type） | ✅ 已实现 |
| 后台启动限制（Android 12+） | 只允许「用户刚点了下载」的**前台上下文**里 `startForegroundService`，启动后靠服务自身保活，不从后台拉起 | ✅ 已实现 |
| 通知权限（13+） | 启动时申请一次 `POST_NOTIFICATIONS`；拒绝则服务照跑、只是无通知，不崩溃 | ✅ 已实现 |
| 断点续传 | Range + `.part` 临时文件 + 整体 SHA-512 流式校验（续传先喂旧段） | ✅ 已实现（进程内） |
| Doze / App Standby | 前台服务只有「部分 Doze 豁免」，不保证持续联网；产品定位「用户在场时下载」 | 已知限制 |
| 厂商白名单 / 自启动 | **不做程序化跳转**（各厂商设置页入口 / 权限命名差异太大，跳转不可靠）；下载页放「下载中断请检查电池优化策略」文案，用户手动进系统设置开启自启动 / 忽略电池优化（小米、华为、OPPO、vivo 等各有入口） | 文案方案（见调研文档） |
| 跨进程续传 | 二期：WorkManager + Room 落库 | ⏳ 未做 |

## 九、CI 与构建

详见 [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) 与 HANDOFF.md「三、CI 踩坑全记录」。

- 构建全部在 GitHub Actions 云端完成（push main → debug APK artifact；PR → 编译检查；
  tag → Release），**本地不装 Android SDK**（构建值守角色除外）
- 单架构 arm64-v8a：`app/build.gradle.kts` 的 `ndk.abiFilters`
- KSP 替代 kapt 是本次构建稳定的关键（Hilt 2.56.2 + KSP 2.0.0-1.0.22 与 Kotlin 2.0.0 配套）

## 十、已知技术债

1. `ModrinthApiImpl` / `ProjectRepositoryImpl` 新端点收尾在途（见 HANDOFF.md「四」）
2. Room 未建表：离线缓存（项目 / 版本 / 下载记录）待二期
3. 无单元测试
4. `core` 模块基本为空
5. 下拉刷新与 offset 分页的 UI 接入未完成
6. 键位无宿主游戏（Phase 2.5）；下载无跨进程续传（二期）
7. release 构建仍为 debug 签名（beta 阶段），正式签名密钥待配置
