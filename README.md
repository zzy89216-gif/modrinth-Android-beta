# Modrinth Android Beta

基于 Modrinth 官方 REST API 的 Android 原生客户端（Kotlin + Jetpack Compose）。

> ⚠️ 超早期阶段：功能骨架已完成，云端 APK 构建仍在调试中，尚无可安装版本。

## 项目定位

Modrinth 官方代码（[modrinth/code](https://github.com/modrinth/code)）是 Rust + TypeScript 的 monorepo
（labrinth 后端 + Web/桌面/移动端前端），**没有现成 Android 原生代码**。

本项目是基于其公开 API 的**原生 Android 重写**，不是逐行移植。
功能对齐参考上游 `apps/labrinth`（接口）与 `apps/app-frontend`（交互）。

## 文档索引

| 文档 | 用途 |
|------|------|
| [HANDOFF.md](HANDOFF.md) | **交接文档：当前状态、卡点、下一步计划** |
| [PROJECT_PLAN.md](PROJECT_PLAN.md) | 阶段规划与任务清单 |
| [TECHNICAL_DOC.md](TECHNICAL_DOC.md) | 架构、模块、技术选型与版本 |
| [API_REFERENCE.md](API_REFERENCE.md) | Modrinth API 接口说明与已知坑 |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | 三个 CI 工作流、产物位置、排错方法 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 贡献流程与代码规范 |
| [CHANGELOG.md](CHANGELOG.md) | 变更日志 |
| [ISSUE_TEMPLATE.md](ISSUE_TEMPLATE.md) | 议题模板与协作规范 |

## 技术栈

| 层 | 选型 |
|----|------|
| 语言 | Kotlin 2.0.0 |
| UI | Jetpack Compose（BOM 2024.09.00） |
| 架构 | MVVM + Clean Architecture（4 模块） |
| DI | Hilt 2.52 + kapt |
| 网络 | Retrofit 2.11 + OkHttp 4.12 + Gson |
| 异步 | Kotlin Coroutines + StateFlow |
| 构建 | AGP 8.6.0 + Gradle 8.7 + JDK 17 |
| CI | GitHub Actions（3 个工作流） |

## 目录结构

```
modrinth-Android-beta/
├── app/          # UI 层：Compose 页面、Navigation、ViewModel、DI
├── data/         # 数据层：Retrofit API、Repository 实现、Hilt 模块
├── domain/       # 领域层：Model、Repository 接口、UseCase（纯 JVM）
├── core/         # 通用组件（预留）
└── .github/workflows/  # build / pr-check / release
```

## 快速开始

```bash
# 构建（需本地 Android SDK；当前项目以云端构建为主）
./gradlew assembleDebug

# 产物
app/build/outputs/apk/debug/app-debug.apk
```

云端构建：推送 main 分支即触发，产物在 Actions 页面 artifact 中下载。

## 许可

MIT License，详见 [LICENSE](LICENSE)。
