# 变更日志

本项目所有重要变更记录于此。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [未发布]

### 待办
- 云端 APK 构建通过
- 真机运行验证
- 数据层字段对齐真实 API

---

## [0.2.0] - 2026-11-27

### 新增
- 搜索页接通真实 Modrinth API（含加载态、错误提示、搜索按钮）
- `SearchProjectsUseCase` 领域用例
- `AppUseCaseModule`：Hilt 绑定 UseCase
- CI 拆分为 3 个工作流：`build.yml`（main 构建产 APK）、`pr-check.yml`（PR 编译检查）、`release.yml`（tag 触发 Release）
- 交接文档 `HANDOFF.md`
- 议题模板统一进 `ISSUE_TEMPLATE.md`（模板 A/B/C + 空白仅维护者）
- 上游仓库克隆：`/root/workspace/upstream/modrinth-code`、`modrinth-docs`

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
  - Navigation 全部导入改为 `androidx.navigation.compose.*`
  - `MainActivity` 加 `@AndroidEntryPoint`，改用 `rememberNavController`
  - `DataModule` 改为标准 Hilt abstract 模块 + companion `@Provides`（原写法 `dagger.provisioning.ProvidesBinding` 不存在）
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
- `Project` 领域模型与 `ProjectRepository` 接口
- 首批文档：README、PROJECT_PLAN、CHANGELOG、CONTRIBUTING、TECHNICAL_DOC、DEPLOYMENT_GUIDE、API_REFERENCE
- GitHub Actions 基础构建工作流
- 应用图标（自适应图标）与主题资源
