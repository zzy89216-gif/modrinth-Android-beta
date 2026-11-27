# API 参考文档

## Modrinth API 集成

本项目使用 Modrinth 官方 API 获取模组数据。

### 基础配置

- **Base URL**: `https://api.modrinth.com/`
- **版本**: v1
- **认证**: Bearer Token (需要时)

### 核心接口

#### 1. 获取模组详情

```
GET /v1/project/{project_id}
```

**响应示例**:
```json
{
  "id": "modrinth-project-id",
  "title": "模组名称",
  "description": "模组描述",
  "categories": ["category1", "category2"],
  "versions": ["version1", "version2"]
}
```

#### 2. 搜索模组

```
GET /v1/project/search
```

**查询参数**:
- `query`: 搜索关键词
- `categories`: 分类过滤
- `facets`: 结果过滤
- `page`: 页码
- `limit`: 每页数量

#### 3. 获取分类列表

```
GET /v1/category
```

#### 4. 用户认证

```
POST /v1/auth/token
```

**请求体**:
```json
{
  "username": "用户名",
  "password": "密码"
}
```

### 错误处理

- `400 Bad Request`: 请求参数错误
- `401 Unauthorized`: 未认证或认证失效
- `404 Not Found`: 资源不存在
- `429 Too Many Requests`: 请求频率过高
- `500 Internal Server Error`: 服务器错误

### Rate Limiting

- 未认证用户: 60 请求/分钟
- 认证用户: 300 请求/分钟

实现建议:
- 使用缓存减少重复请求
- 实现指数退避重试机制
- 监控响应头中的 `RateLimit-Remaining`
