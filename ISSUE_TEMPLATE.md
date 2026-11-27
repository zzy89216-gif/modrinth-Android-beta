# Modrinth Android 移植项目 - 议题与工作规划

## 📌 项目简介

将 Modrinth（Minecraft 模组平台）移植到 Android 平台，打造原生移动客户端。

**仓库**: https://github.com/zzy89216-gif/modrinth-Android-beta
**CI/CD**: https://github.com/zzy89216-gif/modrinth-Android-beta/actions

> ⚠️ 超早期阶段：功能骨架已搭好，核心 API 联调、下载、账户功能尚未实现。

---

## 📋 Issue 提交规范

### 规则

| 角色 | 提交方式 |
|------|----------|
| 普通用户 | 使用下方模板 A（Bug）或模板 B（功能建议）填表提交 |
| 贡献者 / 维护人员 | 可用模板 C（开发任务）；需要时可用空白 issue（标注 `[MAINTAINER]` 前缀） |
| 非 bug/建议的讨论 | 走 GitHub Discussions，不开 issue |

- 标题前缀约定：`[BUG]` `[FEATURE]` `[TASK]` `[MAINTAINER]`
- 标签约定：`triage`（外部 issue 默认）、`P1/P2/P3`（优先级）、`good first issue`

### 模板 A：🐛 Bug 报告（普通用户）

```
标题: [BUG] 一句话描述

App 版本:        （设置页可见，如 1.0.0-beta）
设备 / 系统:      （如 Pixel 8 / Android 15）
问题模块:        启动闪退 / 首页 / 搜索 / 模组详情 / 分类 / 网络 / 其他

问题描述:
（发生了什么，期望的正确行为是什么）

复现步骤:
1.
2.
3.

出现频率:        每次 / 偶尔 / 仅一次
日志 / 截图:     （Logcat 片段或链接，闪退请附崩溃堆栈，可选）
```

### 模板 B：💡 功能建议（普通用户）

```
标题: [FEATURE] 一句话描述

要解决的问题 / 痛点:
（现在缺什么？为什么需要？）

建议方案:
（希望这个功能长什么样？如何工作？）

替代方案:        （可选）
补充说明:        （可选，截图 / 参考链接）
优先级建议:      P1 必须 / P2 重要 / P3 锦上添花
```

### 模板 C：🔨 开发任务（开发者 / 贡献者）

```
标题: [TASK] 一句话描述

所属阶段:        P1 核心完善 / P2 用户功能 / P3 高级功能
涉及模块:        app / data / domain / core，或 UI / 网络 / 存储

任务描述:
（要做什么，为什么做）

验收标准:
- [ ]
- [ ]

分支 / 关联:     （功能分支、相关 issue、API 文档，可选）
工作量预估:      S（<半天）/ M（1-2天）/ L（3天以上）
```

### 空白模板（仅维护人员）

标题加 `[MAINTAINER]` 前缀，正文随意。普通用户请勿提交空白 issue。

---

## ✅ 已完成

### 基础设施
- [x] Gradle 多模块架构（app, core, domain, data）
- [x] Kotlin + Jetpack Compose UI
- [x] MVVM + Clean Architecture
- [x] Hilt 依赖注入
- [x] Navigation 导航系统
- [x] GitHub Actions 自动构建
- [x] Issue 模板体系（本文件）

### UI 页面
- [x] Home 首页
- [x] 搜索页面（UI 骨架 + 真实 API 调用）
- [x] 模组详情页（UI 骨架）
- [x] 分类页面（UI 骨架）

### API 集成
- [x] Modrinth API 封装（Retrofit）
- [x] 数据模型与映射

### 文档
- [x] README.md / PROJECT_PLAN.md / CHANGELOG.md
- [x] CONTRIBUTING.md / TECHNICAL_DOC.md
- [x] DEPLOYMENT_GUIDE.md / API_REFERENCE.md

---

## 🚧 待完成

### P1 - 核心功能完善（当前阶段）
- [ ] 分类页面接真实 API 数据
- [ ] 下拉刷新 & 分页加载
- [ ] 图片加载（Coil）
- [ ] 错误处理 & 空状态页面
- [ ] CI 构建产物 APK 稳定产出

### P2 - 用户功能
- [ ] 用户登录/注册
- [ ] 收藏/关注
- [ ] 下载管理器
- [ ] 离线缓存

### P3 - 高级功能
- [ ] 模组安装/卸载
- [ ] 评论与评分
- [ ] 通知系统
- [ ] 多语言支持

---

## 📊 进度统计

| 模块 | 进度 |
|------|------|
| 基础架构 | ✅ 100% |
| UI 页面 | ✅ 80%（骨架，部分已联调） |
| API 集成 | ✅ 70% |
| 文档 | ✅ 100% |
| CI/CD | 🔄 调试中 |
| 用户功能 | ⏳ 0% |

---

## 🔧 构建说明

```bash
# 本地构建
./gradlew assembleDebug

# 输出位置
app/build/outputs/apk/debug/app-debug.apk
```

## 📚 相关资源

- Modrinth API: https://docs.modrinth.com/api/
- Android Docs: https://developer.android.com/
- Compose Docs: https://compose.android.org/
