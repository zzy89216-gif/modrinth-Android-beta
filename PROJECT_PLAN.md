# Modrinth Android 移植项目规划

## 一、项目目标

构建一个功能完整的 Modrinth Android 客户端，支持：
1. 模组浏览与搜索
2. 模组下载与管理
3. 用户账户系统
4. 模组评论与评分
5. 离线安装支持

## 二、阶段性规划

### Phase 1: 基础框架搭建 (预计 2 周)
- [ ] 初始化 Android 项目结构
- [ ] 配置 Gradle 构建系统
- [ ] 实现基础 Navigation 导航
- [ ] 搭建 MVVM 架构模板
- [ ] 配置网络层 (Retrofit)

### Phase 2: 核心功能实现 (预计 4 周)
- [ ] Modrinth API 封装
- [ ] 模组列表页面
- [ ] 模组详情页面
- [ ] 搜索功能
- [ ] 下载管理器

### Phase 3: 用户体验优化 (预计 2 周)
- [ ] 主题切换 (暗色/亮色)
- [ ] 离线缓存
- [ ] 通知系统
- [ ] 性能优化

### Phase 4: 高级功能 (预计 3 周)
- [ ] 用户账户登录
- [ ] 模组管理 (安装/卸载)
- [ ] 评论与评分系统
- [ ] 文件验证 (SHA-512)

## 三、API 参考

Modrinth 官方 API 文档: https://docs.modrinth.com/api/

主要接口:
- `GET /v1/project/{project_id}` - 获取模组详情
- `GET /v1/project/search` - 搜索模组
- `GET /v1/category` - 获取分类列表
- `POST /v1/auth/token` - 认证

## 四、依赖库选择

| 用途 | 推荐库 |
|------|--------|
| UI | Jetpack Compose |
| 网络 | Retrofit + OkHttp |
| 图片加载 | Coil |
| 依赖注入 | Hilt |
| 本地存储 | Room |
| 异步处理 | Kotlin Coroutines |
| 事件总线 | Flow/StateFlow |

## 五、风险评估

1. **API 限制**: Modrinth API 有 rate limiting，需实现合理的缓存策略
2. **性能**: 大量模组列表加载需优化，使用分页加载
3. **兼容性**: 需覆盖 Android 8.0+ 版本
4. **安全**: 下载文件需验证签名，防止恶意模组
