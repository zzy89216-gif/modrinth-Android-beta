# 贡献指南

欢迎参与 Modrinth Android 移植项目！

## 开发环境要求

- Android Studio Hedgehog 或更高版本
- JDK 17 或更高版本
- Android SDK API 34+

## 开发流程

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/your-feature`)
3. 提交更改 (`git commit -m 'Add some feature'`)
4. 推送到分支 (`git push origin feature/your-feature`)
5. 提交 Pull Request

## 代码规范

- 遵循 Kotlin 编码规范
- 使用 Jetpack Compose 进行 UI 开发
- 提交信息使用英文，格式: `type: description`
- 类型: feat(新功能), fix(修复), docs(文档), refactor(重构), test(测试)

## 测试

- 为新增功能编写单元测试
- 确保现有测试通过
