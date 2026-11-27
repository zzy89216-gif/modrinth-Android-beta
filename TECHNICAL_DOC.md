# 技术文档

## 架构设计

### 分层架构

```
┌─────────────────┐
│   Presentation  │  (UI, Compose, ViewModels)
├─────────────────┤
│     Domain      │  (Business Logic, Entities)
├─────────────────┤
│      Data       │  (API, Database, Repository)
└─────────────────┘
```

### 模块划分

| 模块 | 职责 |
|------|------|
| `app` | 应用入口，Activity/Fragment，UI 组装 |
| `core` | 共享工具类，扩展函数，基础组件 |
| `domain` | 业务逻辑，实体定义，UseCase |
| `data` | 数据源实现，API 调用，本地存储 |

## API 集成

### Modrinth API 配置

```kotlin
// 基础 URL
const val BASE_URL = "https://api.modrinth.com/"

// Retrofit 配置
val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(JsonxConverterFactory.create())
    .build()
```

### 认证方式

Modrinth 使用 Bearer Token 认证:
```kotlin
headers {
    add("Authorization", "Bearer $token")
}
```

## 数据库设计

### Room 实体

- `ModrinthProject` - 模组信息
- `DownloadTask` - 下载任务
- `UserAccount` - 用户账户

## 网络层

使用 Retrofit + OkHttp 实现:
- 自动重试机制
- 请求日志拦截
- JSON 序列化

## 依赖注入

使用 Hilt 进行依赖注入:
- Application 级注入
- ViewModel 注入
- Repository 注入
