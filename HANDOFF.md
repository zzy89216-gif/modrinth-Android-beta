# 交接文档 / Handoff

> 最后更新：2026-11-27
> 本文档用于交接项目当前状态、已知问题与下一步计划。

---

## 一、项目当前状态

| 项目 | 状态 |
|------|------|
| 代码骨架 | ✅ 完成（4 模块 + 4 页面 + API 封装 + Hilt + Navigation） |
| 文档体系 | ✅ 完成（8 份文档） |
| CI 工作流 | ✅ 已拆分 3 个（build / pr-check / release） |
| **云端构建 APK** | ❌ **尚未成功**（当前主要卡点） |
| 真机运行验证 | ⏳ 未开始 |
| 上游代码对齐 | ⏳ 已克隆，未开始对照 |

仓库：https://github.com/zzy89216-gif/modrinth-Android-beta

---

## 二、本机环境与已克隆的上游仓库

```
/root/workspace/
├── modrinth-Android-beta/     # 我们的项目（已接 GitHub）
└── upstream/
    ├── modrinth-code/         # 上游官方仓库全量克隆（约 1GB）
    │   ├── apps/labrinth/         # Rust 后端（API 服务端）
    │   ├── apps/frontend/         # Web 前端
    │   ├── apps/app/              # Tauri 桌面端
    │   ├── apps/app-frontend/     # 移动端前端（React + Vite）
    │   ├── apps/docs/
    │   └── packages/              # 共享 TS 包
    └── modrinth-docs/         # 官方 API 文档（Docusaurus）
```

**重要认知**：上游 `modrinth/code` 是 **Rust + TypeScript 混合 monorepo**，没有现成的 Kotlin/Android 代码。
我们的 Android 版是**基于 Modrinth 官方 REST API 的原生重写**，不是逐行移植。
如需对齐行为/字段，参考 `apps/labrinth`（后端接口定义）和 `apps/app-frontend`（移动端交互逻辑）。

---

## 三、当前 CI 卡点（下次第一件事）

### 已修复的坑（按时间顺序，均已提交）

| # | 问题 | 修复 |
|---|------|------|
| 1 | `android-actions/setup-android@v3` 步骤失败 | 移除该步骤，ubuntu-latest 自带 SDK |
| 2 | 根 `build.gradle.kts` 的 `allprojects.repositories` 与 settings 的 `FAIL_ON_PROJECT_REPOS` 冲突 | 删除根项目 repositories 块 |
| 3 | `proguardFiles getDefaultProguardFile(...)` 缺外层括号（Groovy 语法） | 改为 Kotlin 正确写法 |
| 4 | domain 模块误用 `kotlin.android` 插件 | 改为 `kotlin.jvm` |
| 5 | Navigation 导入包名错误（`androidx.compose.navigation`） | 改为 `androidx.navigation.compose` |
| 6 | `DataModule` 用不存在的 `dagger.provisioning.ProvidesBinding`，且 object 含 abstract 方法 | 改标准 Hilt abstract 模块 + companion `@Provides` |
| 7 | `ModrinthApiImpl` 缺 `retrofit2.http.Path` 导入 | 已补 |
| 8 | Hilt 缺 UseCase 绑定（kapt 会报无绑定） | 新增 `AppUseCaseModule` |
| 9 | Compose + Kotlin 2.0 旧 `composeOptions` 配置 | 改用 `org.jetbrains.kotlin.plugin.compose` |
| 10 | data 模块用 `kapt()` 但未应用 kapt 插件 | 已补 `org.jetbrains.kotlin.kapt` |
| 11 | **AAPT 资源链接失败：`mipmap/ic_launcher_round` 不存在**（最近一次报错） | 已补 `res/mipmap-anydpi-v26/ic_launcher_round.xml` |

> 进度里程碑：第 11 项失败时构建已执行 42 个任务、通过 Kotlin 编译，进入资源链接阶段，说明配置类错误已基本扫清。

### 下次操作

1. 打开 GitHub Actions 最新一次运行，确认 #11 修复后 `Build APK` 是否变绿。
   - 已通过资源链接的话，后面大概率只剩 Kotlin 源码编译错误（类型/导入）或 Hilt 注解处理错误。
2. 若仍失败，**用 API 直接读日志**（比网页快）：
   ```bash
   TOKEN=你的token
   RUN_ID=$(curl -s -H "Authorization: token $TOKEN" \
     "https://api.github.com/repos/zzy89216-gif/modrinth-Android-beta/actions/runs?per_page=1" \
     | python3 -c "import json,sys;print(json.load(sys.stdin)['workflow_runs'][0]['id'])")
   curl -sL -H "Authorization: token $TOKEN" \
     "https://api.github.com/repos/zzy89216-gif/modrinth-Android-beta/actions/runs/$RUN_ID/logs" \
     -o /tmp/logs.zip && cd /tmp && unzip -o logs.zip && \
     sed -n '/What went wrong/,/Try:/p' build/*.txt
   ```
3. 预计后续剩余错误类型：Kotlin 源码编译错误（类型/导入）、AAPT2 资源错误（图标/主题引用）、Hilt 注解处理错误。

---

## 四、代码现状与已知问题

### 已实现

- 4 个页面：Home / Search / ProjectDetail / Categories
- Navigation 路由：`home`、`search`、`project_detail/{projectId}`、`categories`
- 搜索页已接通真实 API（含加载态、错误提示）
- 分类页走 `GET /v1/category`（**注意：该接口返回结构可能不是纯字符串数组，待核对**）
- Hilt 依赖注入全链路：`DataModule`（API + Repository 绑定）→ `AppUseCaseModule`（UseCase）

### 待验证/已知不完善

| 问题 | 说明 |
|------|------|
| API 响应模型 | `ProjectResponse` 字段是按文档手写的，需对照 `apps/labrinth` 或真实响应核对 |
| 搜索结果结构 | Modrinth `/v1/project/search` 返回 `{hits:[...], offset, total}`，当前代码按 `List<ProjectResponse>` 解析，**很可能运行时报错** |
| 分类接口 | `/v1/category` 返回对象含 `category` 数组，当前按 `List<String>` 解析，**待修** |
| ProjectDetail | 详情页 UI 可用，但列表页尚无跳转入口 |
| 图片加载 | 未接入 Coil，logo 未显示 |
| 分页/下拉刷新 | 未实现 |

---

## 五、下次计划（建议顺序）

### 第一步：让 APK 构建成功（最高优先级）
- 看最新 CI 日志 → 修错误 → 推送 → 重复，直到 `Build APK` 变绿
- 绿了之后在 Actions 页面下载 `modrinth-android-debug` artifact 安装到手机验证

### 第二步：跑通真机
- 安装 APK，确认能启动、四个页面能进
- 看 Logcat 修运行时问题（预计：API 解析崩溃、Hilt 注入问题）

### 第三步：数据层对齐真实 API
- 用 `/root/workspace/upstream/modrinth-code/apps/labrinth` 里的 Rust 结构体核对字段
- 修正 `ProjectResponse`、搜索响应包装、分类响应
- 参考 `apps/app-frontend` 的请求逻辑（搜索参数、分页参数）

### 第四步：体验补齐
- 首页展示热门/推荐列表（`/v1/search?...`）
- 列表项跳转详情
- Coil 加载图标
- 下拉刷新 + 分页
- 空状态 / 错误状态页

### 第五步：发第一个 beta
- 打 tag `v0.1.0` → 触发 `release.yml` → 自动生成 Release + APK + sha256

---

## 六、协作须知

- **构建全部走云端**（用户要求），本地不装 Android SDK，不要在本地跑 `assembleDebug`
- 推送前建议先 `git status` 确认改动范围
- 提交信息用中文 `type: 描述` 格式
- Issue 模板已统一在 `ISSUE_TEMPLATE.md`，空白 issue 仅维护者用（`[MAINTAINER]` 前缀）
- Token 等凭据不要写进仓库文件
