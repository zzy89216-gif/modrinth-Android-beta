# ModBox（模组浏览器）

基于 [Modrinth](https://modrinth.com) 官方 REST API 的原生 Android 客户端（Kotlin + Jetpack Compose）。
浏览、搜索、下载 Minecraft 模组，配纯离线本地账户、收藏与（移植自 [ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2) 的）键位映射系统。

> ⚠️ **beta 阶段**：核心功能已完成并持续迭代，部分体验能力（断点续传的网络恢复、键位宿主接入等）见下文「已知限制」。

## 功能一览

| 功能 | 说明 | 状态 |
|------|------|------|
| 浏览 / 搜索 | 首页 + 搜索页，走 `GET /v1/search`（`{hits, offset, limit, total_hits}` 包装结构） | ✅ |
| 分类 | `GET /v1/tag/category` 分类标签页 | ✅ |
| 项目详情 | 字段对齐上游 labrinth Rust 结构体（`models/v2/*.rs`） | ✅ |
| 下载 | 版本列表 + 下载直链（302 到 CDN），`DownloadService`（dataSync 前台服务）执行，进度通知、暂停 / 续传（Range + `.part`）/ 取消、SHA-512 流式校验 | ✅ |
| 离线账户 | 纯本地多账户，PIN 以 SHA-256 摘要存储，DataStore + JSON 持久化 | ✅ |
| 收藏 | 本地账户内项目收藏（`accounts.json`），`ToggleFavoriteUseCase` 等用例 | ✅ |
| 键位映射 | 移植自 ZalithLauncher2 InputMap 模块（GPL-3.0），键位配置 / 持久化 / 重绑定 UI | ✅（暂无宿主游戏） |
| 主题 | 深色主题，主色 `#6C5CE7`（见 `res/values/colors.xml`），Coil 2.7.0 加载图标 | ✅ |

## 技术栈

| 层 | 选型 | 版本 |
|----|------|------|
| 语言 | Kotlin | 2.0.0 |
| UI | Jetpack Compose + Material3 | BOM 2024.09.00 |
| 注解处理 | KSP（kapt 在 Kotlin 2.0 下崩溃，已全量切换） | 2.0.0-1.0.22 |
| DI | Hilt | 2.56.2 |
| 网络 | Retrofit + OkHttp + Gson | Retrofit 2.11.0 / OkHttp 4.12.0 |
| 图片 | Coil（compose 集成） | 2.7.0 |
| 本地存储 | Room + DataStore Preferences | Room 2.6.1 / DataStore 1.1.1 |
| 架构 | MVVM + 分层（UI → UseCase → Repository → API） | — |
| 构建 | AGP 8.6.0 + Gradle 8.7 + JDK 17，**仅 arm64-v8a** | — |
| CI | GitHub Actions（build / pr-check / release 三个工作流，纯云端构建） | — |

依赖版本以根 `build.gradle.kts` 与各模块 `build.gradle.kts` 为准。

## 模块结构

```
modrinth-Android-beta/
├── app/        # UI 层：Compose 页面、Navigation、ViewModel、DI、
│               #   service（下载前台服务）、keymap（键位）、ui/components（CardGrid/ColorPicker）
├── data/       # 数据层：Retrofit API、DTO、Repository 实现、
│               #   account（离线账户仓库）、Hilt 模块
├── domain/     # 领域层（纯 JVM）：模型、Repository 接口、UseCase
├── core/       # 通用组件（基本为空，预留）
├── docs/       # 调研文档：保活与渲染调研.md、zl2调研.md
└── .github/workflows/  # build.yml / pr-check.yml / release.yml
```

依赖方向：`app → data → domain`，`domain` 不反向依赖任何层。

## 快速开始

**推荐：下载云端构建的 APK**

1. 推送 `main` 分支触发 `build.yml`（也可在 Actions 页手动触发 Build APK）。
2. 构建成功后在 Run 详情页下载 artifact `modrinth-android-debug`，解压得到
   `app-debug.apk`，安装到 **arm64-v8a** 设备（minSdk 26 / Android 8.0+）。

**手动构建（可选，仅当你本地有 Android SDK）**

```bash
# JDK 17 + Android SDK（platforms;android-34、build-tools;34.0.0）
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk（仅 arm64-v8a）
```

> 本项目默认**只走云端构建**，本地不装 Android SDK；「构建值守」角色除外。
> Release 版打 tag（`v*`）触发 `release.yml`，当前 beta 阶段为 debug 签名 + prerelease。

## 已知限制（beta）

1. **无断点续传的网络恢复**：进程存活期间切后台 / 锁屏可继续下载（dataSync 前台服务），
   进程被杀后任务状态丢失；跨进程死亡的续传（WorkManager + Room 落库）留二期。
2. **键位暂无宿主游戏**：键位系统（映射 / 持久化 / 重绑定 UI）已完整移植，
   但本 App 内没有运行游戏的进程可消费键位；宿主接入见 PROJECT_PLAN 的 Phase 2.5。
3. **单架构**：只构建 arm64-v8a（`ndk.abiFilters`），x86 / x86_64 模拟器与 32 位设备不可用。
4. 数据层的 Retrofit 接口与 `ProjectRepositoryImpl` 尚有一处收尾在途（见 HANDOFF.md），
   期间搜索 / 分类的旧路径行为以 HANDOFF 为准。

## 文档索引

| 文档 | 用途 |
|------|------|
| [HANDOFF.md](HANDOFF.md) | **交接文档：当前状态、CI 踩坑全记录、下一步计划** |
| [PROJECT_PLAN.md](PROJECT_PLAN.md) | 阶段规划（Phase 1–4 + Phase 2.5）与任务清单 |
| [TECHNICAL_DOC.md](TECHNICAL_DOC.md) | 架构、模块图、技术选型与版本矩阵、保活策略摘要 |
| [API_REFERENCE.md](API_REFERENCE.md) | Modrinth API 真实接口说明与旧代码错误对照 |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | CI 工作流、产物位置、排错方法 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 贡献流程与代码规范 |
| [CHANGELOG.md](CHANGELOG.md) | 变更日志 |
| [ISSUE_TEMPLATE.md](ISSUE_TEMPLATE.md) | 议题模板与协作规范 |
| [docs/保活与渲染调研.md](docs/保活与渲染调研.md) | 前台服务 / 厂商自启动 / Compose 渲染调研（调研结论） |
| [docs/zl2调研.md](docs/zl2调研.md) | ZalithLauncher2 键位 / 渲染器移植可行性调研 |

## GPL 代码来源说明

本仓库包含从 **ZalithLauncher2**（GPL-3.0）移植的代码：键位映射（InputMap 模块）、
CardGrid 组件、ColorPicker 组件。完整移植清单、源文件对照与许可条款见根目录
[NOTICE](NOTICE)，移植可行性调研见 [docs/zl2调研.md](docs/zl2调研.md)。
每个移植文件头均带 `Ported from ZalithLauncher2 (GPL-3.0), see NOTICE` 注释。

## 许可

- **ModBox 本体代码**：MIT License，见 [LICENSE](LICENSE)。
- **移植自 ZalithLauncher2 的部分**：GPL-3.0 或更高版本（见源仓库 LICENSE 与根目录 NOTICE）。

两者并存：MIT 仅约束本项目自身代码，GPL 移植文件按其原条款分发；
二次开发分发时请同时遵守两类条款（详见 NOTICE 的修改版处理说明）。
