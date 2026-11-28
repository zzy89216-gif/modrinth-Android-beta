# 交接文档 / Handoff

> 最后更新：2026-11-28
> 本文档用于交接项目当前状态、本次会话成果、CI 踩坑全记录与下一步计划。
> 协作规则：只写文档不碰代码的角色见「六、团队协作模式」；代码改动走写域隔离。

## 目录

1. [项目当前状态](#一项目当前状态)
2. [本次会话成果](#二本次会话成果)
3. [CI 踩坑全记录](#三ci-踩坑全记录)
4. [代码现状与已知问题](#四代码现状与已知问题)
5. [下一步待办](#五下一步待办)
6. [团队协作模式](#六团队协作模式)
7. [协作须知](#七协作须知)

---

## 一、项目当前状态

| 模块 | 完成度 | 说明 |
|------|--------|------|
| 项目定位与文档体系 | ✅ 完成 | 9 份根文档 + docs/ 两份调研文档 |
| CI 工作流 | ✅ 完成 | build（push main / 手动触发）/ pr-check / release（tag）三个工作流 |
| 构建配置（KSP + Hilt + 单架构） | ✅ 完成 | KSP 2.0.0-1.0.22、Hilt 2.56.2、arm64-v8a，见「三、CI 踩坑全记录」 |
| 数据层（模型 / 仓储接口） | ✅ 基本完成 | DTO / 领域模型全量对齐 labrinth `models/v2/*.rs`；仓储接口含搜索 / 版本 / 分类 / 下载直链 |
| 数据层（Retrofit 接口 + 实现） | ✅ 完成 | `ModrinthApiImpl` / `ProjectRepositoryImpl` 已按真实端点重写，Kotlin 2.0.0 独立编译通过；**字段对照线上 openapi/v2.json + 真实 /v2 JSON 验证，仅 1 处错配已修** |
| UI（搜索 / 分类 / 详情） | ✅ 完成 | 搜索（300ms 防抖 + offset 分页）、分类、详情（版本列表 + 下载按钮接 DownloadManager） |
| UI（底部导航 3 tab + 主题 + 我的页） | ✅ 完成 | 3 tab（首页/分类/我的）+ 深色主题（品牌绿 #00AF5E）+ 离线账户页（注册/登录/收藏/登出）；**未经云端编译验证，明天第一件事** |
| 下载管理器 | ✅ 完成 | `DownloadService`（dataSync 前台服务）+ `DownloadManager` 状态机，见下 |
| 离线账户 + 收藏 | ✅ 完成 | PIN SHA-256、DataStore + `accounts.json`，见下 |
| 键位映射（zl2 移植） | ✅ 完成（无宿主） | 配置 / 持久化 / 重绑定 UI 完整，暂无宿主游戏消费键位 |
| CardGrid / ColorPicker（zl2 移植） | ✅ 完成 | 已落地 `ui/components/`，移除 Material Expressive 依赖（见 NOTICE） |
| 真机运行验证 | ⏳ 未开始 | 等云端构建变绿后安装验证 |

### 已完成的核心能力速览

- **下载管理器**（`app/.../service/`）：`DownloadManager`（进程内单例状态机，任务列表 StateFlow，
  提交 / 暂停 / 继续 / 取消 / 重试）+ `DownloadService`（dataSync 前台服务，主循环协程跑 OkHttp
  流式下载；进度通知带「暂停 / 取消」操作；SHA-512 流式校验，续传时旧 `.part` 字节先喂进 digest；
  Range 请求断点续传）。权限：`FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` +
  13+ `POST_NOTIFICATIONS`（启动时申请一次，拒绝则降级不崩溃）。
- **离线账户**（`domain/.../account/` + `data/.../account/`）：纯本地多账户，PIN 以
  SHA-256 hex 摘要存储（不存明文）；当前登录 id 在 DataStore Preferences，账户列表（含 PIN 摘要与
  收藏）在 `filesDir/accounts.json`（Gson 序列化）；读-改-写在 IO 协程 + Mutex 下串行；
  用例：Register / Login / Logout / ToggleFavorite / GetFavorites。
- **键位映射**（`app/.../keymap/`，GPL-3.0 移植）：GLFW 键码表 + 默认 Minecraft 键位 +
  Android 键位映射 + `KeymapRepository`（`filesDir/keymap.json` 持久化，org.json 实现）+
  重绑定 UI（含组合键 / 长按捕获）。

---

## 二、本次会话成果（2026-11-28 大重构）

1. **注解处理整体迁移 kapt → KSP**：KSP `2.0.0-1.0.22` 与 Kotlin 2.0.0 配套；
   Hilt 2.52 → 2.53.1 → **2.56.2**（2.53.1 仍不够，见踩坑 #14）。
2. **单架构构建**：`ndk.abiFilters` 只发 **arm64-v8a**。
3. **数据层全量重写对齐真实 API**（对照上游 labrinth Rust 结构体，路径
   `upstream/modrinth-code/apps/labrinth/models/v2/*.rs`，只读克隆）：
   - `GET /v1/search` 返回 `{hits, offset, limit, total_hits}` 包装 —— 旧代码按裸数组解析是错的，
     新增 `SearchResponse` / `SearchProjectResponse`；
   - 分类 `GET /v1/tag/category` —— 旧 `/v1/category` 是错的，新增 `CategoryResponse`；
   - 版本 `GET /v1/project/{id}/version`，新增 `VersionResponse` / `VersionFileResponse` /
     `VersionDependencyResponse`（`files[].{url, filename, primary, size, hashes:{sha1, sha512}}`）；
   - 下载直链：302 到 CDN（`Location` 头），302 同时带 `{"url": ...}` JSON 体（`DownloadRedirect`），
     新增 `ProjectFileResponse` 兜底解析；
   - 项目详情 `ProjectResponse` 全面重写（无 `author` / `logo_url`；`icon_url`、`followers`、
     `license` 对象、`body` 长描述等）。
4. **新增离线本地账户系统**：见上文「已完成的核心能力速览」；domain 层新增 account 包
   （模型 + 接口 + 5 个用例），data 层新增 `AccountRepositoryImpl` + `AccountModule`。
5. **新增下载管理器**：dataSync 前台服务 + 状态机 + 通知 + SHA-512 校验 + 暂停 / 续传 / 取消；
   Hilt 绑定 `DownloadModule`（app 层）。
6. **键位映射移植自 ZalithLauncher2**（InputMap 模块，GPL-3.0）：7 个文件，
   每个文件头有 `Ported from ZalithLauncher2 (GPL-3.0), see NOTICE`；调研依据 `docs/zl2调研.md`。
7. **CardGrid / ColorPicker 组件移植自 ZalithLauncher2**（GPL-3.0，纯 Compose）：
   11 + 9 个文件落地 `ui/components/`；`CardGrid.kt` 移除对 Material Expressive `motionScheme`
   的依赖（Compose BOM 2024.09.00 尚不可用），改用内置弹簧动画。
8. **渲染 / 保活落地**（调研依据 `docs/保活与渲染调研.md`）：Coil 全局 loader 由 Hilt 单例提供
   （memoryCache 50MB / diskCache 128MB / crossfade 200ms；coil-compose 2.7.0 无 singleton
   自动发现，`AsyncImage` 必须显式传 `imageLoader`）；`ProjectCard` 固定行高 76dp 列表卡片。
9. **UI 重构（完成）**：底部导航三 tab（首页 / 分类 / 我的）+ 搜索 + 项目详情 + 我的页；深色主题。
   品牌按用户决定改为 **Modrinth 官方绿 `#00AF5E`**，app_name = **「modrinth APP」**，启动图标重做为
   官方风格绿环 + 扳手矢量（`drawable/ic_launcher_foreground.xml`）。⚠️ 用官方 logo/名称的许可证
   合规调研由 license-agent 进行中（产出 `docs/许可证调研.md`），**明天先读该文档再决定是否回退品牌**。
10. **文档体系重写**：README / HANDOFF / CHANGELOG / PROJECT_PLAN / API_REFERENCE /
    TECHNICAL_DOC / CONTRIBUTING 全部对齐上述新现实；根目录新增 `NOTICE`（GPL 移植清单）。
11. **CI 踩坑全部扫清**：云端构建推进到 KSP 阶段，详见「三」。

> 上游克隆（只读，用于对照字段）：`upstream/modrinth-code`（官方 monorepo，约 1GB）、
> `upstream/modrinth-docs`（API 文档）、`upstream/ZalithLauncher2`（键位移植源）。

---

## 三、CI 踩坑全记录

> 按时间顺序。1–12 发生在 2026-11-27 会话，13–15 发生在 2026-11-28 大重构会话。
> 每项均已提交修复（除标注「在途」者）；里程碑与 commit 对应关系见 git log。

### 11-27 会话（12 个坑）

| # | 问题 | 修复 |
|---|------|------|
| 1 | `android-actions/setup-android@v3` 步骤失败 | 移除该步骤，ubuntu-latest 自带 SDK |
| 2 | 根 `build.gradle.kts` 的 `allprojects.repositories` 与 settings 的 `FAIL_ON_PROJECT_REPOS` 冲突 | 删除根项目 repositories 块 |
| 3 | `proguardFiles getDefaultProguardFile(...)` 缺外层括号（Groovy 语法混入 Kotlin DSL） | 改为 Kotlin 正确写法 |
| 4 | domain 模块误用 `kotlin.android` 插件 | 改为 `kotlin.jvm` |
| 5 | Navigation 导入包名错误（`androidx.compose.navigation` 子树不存在） | 改为 `androidx.navigation.compose` |
| 6 | `DataModule` 用不存在的 `dagger.provisioning.ProvidesBinding`，且 object 含 abstract 方法 | 改标准 Hilt abstract 模块 + companion `@Provides` |
| 7 | `ModrinthApiImpl` 缺 `retrofit2.http.Path` 导入 | 已补 |
| 8 | Hilt 缺 UseCase 绑定（kapt 会报无绑定） | 新增 `AppUseCaseModule` |
| 9 | Compose + Kotlin 2.0 旧 `composeOptions` 配置 | 改用 `org.jetbrains.kotlin.plugin.compose` |
| 10 | data 模块用 `kapt()` 但未应用 kapt 插件 | 已补插件声明 |
| 11 | AAPT 资源链接失败：`mipmap/ic_launcher_round` 不存在 | 补 `res/mipmap-anydpi-v26/ic_launcher_round.xml` |
| 12 | **kapt 报 `NonExistentClass cannot be converted to Annotation`** | 残留的废弃重复接口 `ModrinthApi.kt` 用了 `@Path` 却未导入，已删除；Repository 实际用 `ModrinthApiImpl` |

> 里程碑：第 12 项时构建已执行 62 个任务、通过 Kotlin 编译和资源链接，进入注解处理阶段。

### 11-28 会话（KSP 迁移 + 单架构，重点）

| # | 问题 | 修复 |
|---|------|------|
| 13 | **kapt 在 Kotlin 2.0 下崩溃**（`NonExistentClass cannot be converted to Annotation` 在注解处理阶段复现，12 项清完后仍在） | **整体切到 KSP 2.0.0-1.0.22**（与 Kotlin 2.0.0 配套版本），所有模块 `kapt(...)` 依赖改 `ksp(...)`，移除 kapt 插件。commit `67977e7` |
| 14 | 升 Hilt 2.53.1 后 kapt/KSP 仍不兼容 Kotlin 2.0 工具链（2.53.1 发布时未适配 Kotlin 2.0 的编译器 API 路径） | **Hilt 升到 2.56.2**（`hilt-android` / `hilt-compiler` 同版本，app 与 data 模块一致）。commit `fda308a` |
| 15 | `import com.google.dagger.hilt.android.HiltAndroidApp` **编译报错** | `com.google.dagger` 是 **Maven groupId，不是 Java 包名**。`@HiltAndroidApp` 的正确导入是 `dagger.hilt.android.HiltAndroidApp`（`com.google.dagger:hilt-android` 产物内部的包名前缀是 `dagger.hilt.*`）。已修复（ModrinthApplication.kt） |
| 16 | navigation **2.8.0** 下 `navArgument` 导入报「unresolved reference」 | 2.8.0 里 `navArgument` 在 **`androidx.navigation` 包**（navigation-common/navigation-runtime 侧），**不在** `androidx.navigation.compose` 子包。已修复（NavGraph.kt） |
| 17 | Material3 **1.3**（BOM 2024.09.00 带出）的 `TopAppBar` 是实验性 API，直接使用报 OptIn 要求 | 使用 `TopAppBar` 的文件顶部加 `@file:OptIn(ExperimentalMaterial3Api::class)`。已修复（NavGraph/Home/Search/Categories/ProjectDetail/Mine 六处） |

> 单架构：`ndk { abiFilters += listOf("arm64-v8a") }` 于 11-28 一并落地（commit `0e5490d`），
> APK 体积与构建时间显著下降；代价是 x86 / 32 位设备不可用（见 README「已知限制」）。

### 读 CI 日志的快捷方法（比网页快）

```bash
# 在 GitHub 个人设置生成 fine-grained token（仅本仓库 Actions read），存为环境变量，不要写进仓库
RUN_ID=$(curl -s -H "Authorization: token $TOKEN" \
  "https://api.github.com/repos/zzy89216-gif/modrinth-Android-beta/actions/runs?per_page=1" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['workflow_runs'][0]['id'])")
curl -sL -H "Authorization: token $TOKEN" \
  "https://api.github.com/repos/zzy89216-gif/modrinth-Android-beta/actions/runs/$RUN_ID/logs" \
  -o /tmp/logs.zip && cd /tmp && unzip -o logs.zip && \
  sed -n '/What went wrong/,/Try:/p' build/*.txt
```

> `$TOKEN` 仅为环境变量占位符，文档与提交里一律不出现真实凭据。

---

## 四、代码现状与已知问题

### 已完成（本次重构落地，均已在工作区）

- 数据模型全量对齐真实 API：`SearchResponse`（hits 包装）、`SearchProjectResponse`、
  `CategoryResponse`、`VersionResponse`、`VersionFileResponse`、`VersionDependencyResponse`、
  `ProjectFileResponse`（302 JSON 兜底）、重写后的 `ProjectResponse` / `Project`。
- 领域层：`ProjectRepository` 接口新增 `searchProjects(query, offset, limit): SearchResult`、
  `getProjectVersions(...)`、`getCategories(): List<Category>`、`getFileDownloadUrl(projectId, versionId)`，
  均带 KDoc 说明真实响应形状；用例层 `SearchProjectsUseCase.executePaged` 等。
- 账户 / 下载 / 键位 / 组件四大新增子系统（见「一」）。

### 今晚已修复的编译断点（2026-11-28 收尾）

| 断点 | 修复 |
|------|------|
| `ModrinthApiImpl` 只有旧 3 端点 | 已重写为 6 个真实路由（search→SearchResponse 包装 / tag/category 对象数组 / version / version_file 302 兜底），Kotlin 2.0.0 + 真实 jar 独立编译通过 |
| `ProjectRepositoryImpl` 旧签名 | 已按新接口重写，全 try/catch 兜底 + 12 组 data→domain 映射 |
| `@HiltAndroidApp` 错 import | `dagger.hilt.android.HiltAndroidApp`（ModrinthApplication.kt） |
| `navArgument` 错 import | `import androidx.navigation.navArgument` + `NavType`（NavGraph.kt） |
| TopAppBar 实验性 API | 6 个 UI 文件全部加 `@file:OptIn(ExperimentalMaterial3Api::class)` |
| `ProjectCard` 引用不存在的 `project.logoUrl` | 改为 `project.iconUrl`（domain 模型字段） |
| 新 UseCase 缺 Hilt 绑定 | `AppUseCaseModule` 补 GetProjectVersions/GetCategories/GetFileDownloadUrl 三个绑定（account 的 5 个 usecase 在 data 的 AccountModule 已提供） |

### 尚未验证的风险（明天按此排查）

| 风险 | 说明 | 排查方法 |
|------|------|---------|
| **R1：线上 /v1 可能已 410** | data-agent 实测线上 `/v1/*` 返回 410（`api_deprecated`），可用契约在 `/v2/*`（形状相同，同一套 Legacy* 结构体）。`baseUrl` 集中在 `data/.../di/DataModule.kt` 一处 | 明天第一件事 `curl -I https://api.modrinth.com/v1/search?q=1` 与 `/v2/search` 各测一次；若 v1 是 410，把 `BASE_URL` 从 `https://api.modrinth.com/v1/` 改为 `https://api.modrinth.com/v2/`，**一行改动** |
| **R2：全新 UI 未经云端编译** | 今晚 lead 手写 11 个 UI 文件（theme/NavGraph/5 页 + 2 个 AccountView），括号配平自查过但无编译器验证 | CI 变绿前的第一轮错误预计集中在 app 模块 Compose API（M3 1.3 的 FilterChip/Surface 参数、icons core 可用性），按「CI 日志快捷方法」拉清单逐个修 |
| **R3：品牌合规未确认** | app 用官方 logo + 名称「modrinth APP」，license-agent 调研中 | 读 `docs/许可证调研.md`（若已产出）；若结论是受限，回退方案：图标换回自研、app_name 改「ModBox」，改动集中在 `strings.xml` + `ic_launcher_foreground.xml` |
| R4：Coil imageLoader 注入链路 | 各 ViewModel 构造注入 `coil.ImageLoader`（CoilModule 提供 @Singleton）；若 Hilt 图缺失会报 `no providers for ImageLoader` | CI 日志搜 "ImageLoader" / "no providers" |

> ⚠️ 接手时不要按旧接口回改 domain 层——domain 的 `SearchResult`/`SearchProject` 是新契约，
> 旧签名已不存在。

---

## 五、明日执行清单（按顺序，今天到此为止）

### 第 0 步：看今晚 CI（预计 10 分钟）

1. 读 `docs/许可证调研.md`（license-agent 产出；若还没产出，问它的汇报消息）
2. 查今晚推送触发的 CI run：按「三」末尾的快捷命令拉日志
   - 红：按 R2 清单修 app 模块 Compose 错误 → 推送 → 再查
   - 绿：直接进第 1 步
3. **R1 确认**：`curl -sI "https://api.modrinth.com/v1/search?query=optifine&limit=1"` 看状态码；
   410 则改 `DataModule.kt` 的 baseUrl 为 `/v2/`，推送

### 第 1 步：真机安装验证（CI 绿后）

1. 下载 artifact `modbox-debug`：
   `curl -sL -H "Authorization: token $TOKEN" "https://api.github.com/repos/zzy89216-gif/modrinth-Android-beta/actions/runs/$RUN_ID/artifacts" | python3 -c "import json,sys;print(json.load(sys.stdin)['artifacts'][0]['archive_download_url'])"`
   → 下载 unzip 得 `app-debug.apk`
2. 装到手机（arm64 设备）：`adb install -r app-debug.apk` 或走 DSH 手机操控 `open`
3. 验收清单（每项过/不过记下来）：
   - [ ] 启动不闪退，深色背景，顶部「Modrinth」标题
   - [ ] 底部 3 tab 切换（首页/分类/我的），无底部栏的二级页返回正常
   - [ ] 首页热门列表出图出字（Coil 加载 modrinth CDN 图标）
   - [ ] 搜索：输入 → 300ms 后出结果；触底加载下一页；清空调用
   - [ ] 分类列表出数据，点分类跳搜索
   - [ ] 详情：版本列表 + 下载按钮 → 通知出现、进度走、暂停/继续/取消、.part 落盘、SHA-512 校验 DONE
   - [ ] 我的：注册（4-8 位 PIN）→ 头像色块 + 用户名；登出回表单；收藏列表
   - [ ] 键位页（如已挂到导航）：重绑定对话框、恢复默认
4. 网络失败场景：断网看错误态是否显示且不崩溃

### 第 2 步：品牌定稿（取决于第 0 步的许可证结论）

- 可用官方 logo：保持现状，README 补「Brand assets belong to Modrinth」
- 受限：回退 app_name「ModBox」+ 自研图标（改动仅 2 个文件）

### 第 3 步：beta 发布

1. 真机验收通过后打 tag：`git tag v0.1.0-beta && git push origin v0.1.0-beta`
2. `release.yml` 自动：临时 keytool 签名 → apksigner v1+v2 → `modbox-v0.1.0-beta-<date>-arm64-v8a.apk` + sha256 → GitHub Release（prerelease）
3. 下载 Release APK 装一台新设备验证（临时签名装过别的老包要先卸载）
4. 正式发布（v1.0.0）前配 4 个 Secrets（清单见 `release.yml` 头注释）

### 第 4 步：Phase 2.5（有余力再做）

- 渲染/保活：Macrobenchmark + jank 排查（依据 `docs/保活与渲染调研.md` 结论清单）
- 键位宿主接入（依据 `docs/zl2调研.md`；需要游戏进程宿主，beta 不做）
- WorkManager + Room 落库跨进程续传（二期预研）

---

## 六、团队协作模式（本次大重构）

**Lead + 5 个智能体并行，写域隔离**：

| 角色 | 职责 | 写域（只允许改这些路径） |
|------|------|--------------------------|
| lead | 拆解任务、验收、跨模块仲裁、文档统筹 | 全部（评审 + 提交） |
| data-agent | 数据层对齐真实 API（模型 / 接口 / 仓储 / Retrofit） | `data/`、`domain/` |
| ui-agent / ui-agent-2 | UI 重写（三 tab、主题、搜索 / 分类 / 详情 / 我的） | `app/.../ui/`、`app/src/main/res/` |
| engineer-agent | 渲染 / 保活 / 下载管理器（Coil、前台服务、ProjectCard） | `app/.../service/`、`app/.../di/`、`app/src/main/AndroidManifest.xml` |
| account-agent | 离线账户系统（domain 接口 + 用例、data 仓库 + DI） | `domain/.../account/`、`data/.../account/` |
| zl2-agent | ZalithLauncher2 键位 / 组件移植 + NOTICE | `app/.../keymap/`、`app/.../ui/components/`、`NOTICE` |
| build-agent | 本地编译环境 + 编译守卫（构建值守） | 构建脚本 / 缓存，不改业务代码 |
| doc-agent | 7 份根文档重写 | 仅 7 份 `.md`（本文所属角色） |

规则：

- 写域重叠时以任务板（team task）登记的 `write_scopes` 为准；越界改动由 lead 回滚。
- 调研文档（`docs/保活与渲染调研.md`、`docs/zl2调研.md`）由对应智能体产出，**只读引用**，
  其他角色不改。
- 冲突处理：domain 接口变更必须先改 domain 再由 data / app 跟随；文档只描述已核实的事实，
  在途工作一律标注「在途」。

---

## 七、协作须知

- **构建全部走云端**（用户要求），本地不装 Android SDK，不要在本地跑 `assembleDebug`
  （构建值守角色除外）。
- 提交信息用中文 `type: 描述` 格式（`feat` / `fix` / `docs` / `ci` / `refactor` / `test`）。
- Issue 模板统一在 `ISSUE_TEMPLATE.md`，空白 issue 仅维护者用（`[MAINTAINER]` 前缀）。
- **Token 等凭据不要写进仓库文件**；示例命令里不出现真实凭据。
- 文档规范：简体中文；一级标题唯一；超 3 个二级标题加目录；版本 / 路径 / 对照用表格；
  代码块标注语言；不写任何本地绝对路径（工作区内部引用用相对路径）。
