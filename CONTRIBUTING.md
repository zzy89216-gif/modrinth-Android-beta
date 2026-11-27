# 贡献指南

## 环境要求

- JDK 17
- Android SDK（platforms;android-34、build-tools;34.0.0）——仅当你要本地构建
- 建议：日常开发以云端 CI 构建为准

## 开发流程

1. Fork 仓库并克隆
2. 建分支：`git checkout -b feature/xxx`
3. 开发，提交信息用中文 `type: 描述`（`feat` / `fix` / `docs` / `ci` / `refactor` / `test`）
4. 推送到自己的 fork
5. 提 Pull Request 到 `main`（会自动触发 `PR Check` 工作流，只做编译检查）

## 代码规范

- Kotlin 官方风格；Compose 优先无状态 + 状态提升
- 分层约束：`app`（UI/VM）→ `domain`（UseCase/接口）→ `data`（实现）
  - ViewModel 不直接依赖 Retrofit
  - `domain` 模块不引入 Android 依赖
- 新增依赖注入：`@Provides`/`@Binds` 放对应模块，必须 `@InstallIn`
- 提交前自查：
  - `git status` 确认改动范围
  - 构建类改动务必确认 CI `Build APK` 通过

## 提交 PR 检查清单

- [ ] 能编译（PR Check 通过）
- [ ] 未提交无关文件（如 `local.properties`、keystore）
- [ ] 涉及构建/依赖变更时说明原因
- [ ] 更新了相关文档（CHANGELOG 等）

## 版本与发布

- 版本语义化：`v主.次.修`
- 打 tag 即触发 Release 工作流：`git tag v0.1.0 && git push origin v0.1.0`

## 文档

新增或修改功能时，请同步更新：

| 文档 | 何时更新 |
|------|----------|
| `CHANGELOG.md` | 每次功能/修复提交 |
| `HANDOFF.md` | 当前状态、卡点、下一步有变化时 |
| `PROJECT_PLAN.md` | 阶段任务状态变化 |
| `TECHNICAL_DOC.md` | 架构、版本矩阵、依赖变化 |
| `API_REFERENCE.md` | 接口调用/模型变化 |

## 提 Issue

见 [ISSUE_TEMPLATE.md](ISSUE_TEMPLATE.md)：
普通用户用模板 A（Bug）/ 模板 B（功能建议）；开发者用模板 C（任务）；空白 issue 仅维护者（`[MAINTAINER]` 前缀）。
