# 贡献指南

> 更新：2026-11-28（构建说明对齐 KSP / Hilt 2.56.2 / 单架构新现实）。

## 环境要求

| 项 | 版本 / 说明 |
|----|-------------|
| JDK | 17 |
| Android SDK | platforms;android-34、build-tools;34.0.0 —— **仅当你需要本地构建** |
| Gradle | wrapper 固定 8.7，无需单独安装 |
| 推荐方式 | **日常开发以云端 CI 构建为准**，本地不装 Android SDK（构建值守角色除外） |

## 构建说明

**云端构建（默认）**

| 触发 | 工作流 | 产物 |
|------|--------|------|
| 推送 `main`（或 Actions 手动触发） | `build.yml` | artifact `modrinth-android-debug`（debug APK，仅 arm64-v8a） |
| 提 PR 到 `main` | `pr-check.yml` | 只做 `compileDebugKotlin` 编译检查 |
| 打 tag（`v*`） | `release.yml` | Release APK（当前 beta 阶段为 debug 签名）+ sha256 + GitHub Release（prerelease） |

读 CI 日志比网页快（方法见 HANDOFF.md「三、CI 踩坑全记录」末尾；token 用环境变量，不写进仓库）。

**手动构建（可选，仅本地有 Android SDK 时）**

```bash
# JDK 17 + Android SDK 就绪后
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk（仅 arm64-v8a，装 arm64 设备，minSdk 26）
```

> 注意：
> - 项目只发 **arm64-v8a** 单架构（`app/build.gradle.kts` 的 `ndk.abiFilters`），
>   x86 / x86_64 模拟器装不了，本地调试请用 arm64 模拟器或真机
> - 注解处理用 **KSP**（2.0.0-1.0.22），不是 kapt；新加需要注解处理的依赖
>   （如 Hilt / Room）一律用 `ksp(...)` 声明，并把 `com.google.devtools.ksp` 插件加到模块
> - Hilt 固定 **2.56.2**（app 与 data 模块版本必须一致，且 ≥ 2.56.x 才兼容 Kotlin 2.0 工具链）

## 开发流程

1. Fork 仓库并克隆
2. 建分支：`git checkout -b feature/xxx`
3. 开发，提交信息用中文 `type: 描述`（`feat` / `fix` / `docs` / `ci` / `refactor` / `test`）
4. 推送到自己的 fork
5. 提 Pull Request 到 `main`（自动触发 `PR Check` 编译检查）
6. 合并后推送 `main` 自动产出 debug APK

## 代码规范

- Kotlin 官方风格；Compose 优先无状态 + 状态提升
- 分层约束：`app`（UI/VM/子系统）→ `domain`（UseCase/接口/模型）→ `data`（实现）
  - ViewModel 不直接依赖 Retrofit
  - `domain` 模块不引入 Android 依赖
- 新增 Hilt 依赖注入：`@Provides` / `@Binds` 放对应模块，必须 `@InstallIn`；
  模块是 abstract class + companion `@Provides`
- **API 模型**：字段以真实接口 / 上游 labrinth Rust 结构体为准（对照方法见
  `API_REFERENCE.md`），统一 `@SerializedName` + 可空/默认值；搜索类接口用包装类，不用裸列表
- **GPL 移植代码**（`keymap/`、`ui/components/cardgrid|colorpicker/`，见根目录 NOTICE）：
  保留文件头的 `Ported from ZalithLauncher2 (GPL-3.0), see NOTICE` 注释；
  新增移植文件必须同步更新 NOTICE 清单
- 提交前自查：
  - `git status` 确认改动范围
  - 构建类改动务必确认 CI `Build APK` 通过

## 提交 PR 检查清单

- [ ] 能编译（PR Check 通过）
- [ ] 未提交无关文件（如 `local.properties`、keystore）
- [ ] 涉及构建 / 依赖变更时说明原因（尤其 KSP / Hilt / Compose BOM 版本）
- [ ] 更新了相关文档（CHANGELOG 等，见下表）
- [ ] 新增 / 修改 GPL 移植文件时更新了 NOTICE

## 版本与发布

- 版本语义化：`v主.次.修`
- 打 tag 即触发 Release 工作流：`git tag v1.0.0-beta && git push origin v1.0.0-beta`
- beta 阶段 release 为 debug 签名 + prerelease；正式签名密钥（secrets）上线前再配置

## 文档

新增或修改功能时，请同步更新：

| 文档 | 何时更新 |
|------|----------|
| `CHANGELOG.md` | 每次功能 / 修复提交（未发布改动进 `[Unreleased]`） |
| `HANDOFF.md` | 当前状态、卡点、下一步有变化时 |
| `PROJECT_PLAN.md` | 阶段任务状态变化 |
| `TECHNICAL_DOC.md` | 架构、版本矩阵、依赖变化 |
| `API_REFERENCE.md` | 接口调用 / 模型变化 |

## 提 Issue

见 [ISSUE_TEMPLATE.md](ISSUE_TEMPLATE.md)：
普通用户用模板 A（Bug）/ 模板 B（功能建议）；开发者用模板 C（任务）；
空白 issue 仅维护者（`[MAINTAINER]` 前缀）。
