# Modrinth Android 移植项目 - 工作规划

## 📌 项目简介

将 Modrinth（Minecraft 模组平台）移植到 Android 平台，打造原生移动客户端。

**仓库**: https://github.com/zzy89216-gif/modrinth-Android-beta
**CI/CD**: https://github.com/zzy89216-gif/modrinth-Android-beta/actions

> ⚠️ 超早期阶段：功能骨架已搭好，核心 API 联调、下载、账户功能尚未实现。

---

## 📋 Issue 与协作规范

### 仓库 Issue 模板体系

| 模板 | 标签 | 面向 | 说明 |
|------|------|------|------|
| 🐛 Bug 报告 | `bug` `triage` | 普通用户 | 强制填写版本/设备/复现步骤 |
| 💡 功能建议 | `enhancement` `triage` | 普通用户 | 问题 + 建议方案 + 优先级 |
| 🔨 开发任务 | `task` | 开发者/贡献者 | 关联规划阶段，明确验收标准 |

- 普通用户在网页上**必须走模板**（`blank_issues_enabled: false`）
- 开发者/维护者可用 `gh issue create` 或 API 创建无模板 issue
- 讨论类内容请走 GitHub Discussions，不开 issue

### 标签约定

- `triage`：待分诊（所有外部 issue 默认带）
- `P1` `P2` `P3`：优先级
- `good first issue`：适合新贡献者的任务

---

## ✅ 已完成

### 基础设施
- [x] Gradle 多模块架构（app, core, domain, data）
- [x] Kotlin + Jetpack Compose UI
- [x] MVVM + Clean Architecture
- [x] Hilt 依赖注入
- [x] Navigation 导航系统
- [x] GitHub Actions 自动构建
- [x] Issue 模板体系 + 协作规范

### UI 页面
- [x] Home 首页
- [x] 搜索页面（UI 骨架）
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
- [ ] 搜索功能实际 API 调用实现（当前 ViewModel 为 TODO）
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
| UI 页面 | ✅ 80%（骨架，待联调） |
| API 集成 | ✅ 70%（已封装，未联调） |
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
