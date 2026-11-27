# Modrinth API 参考

官方文档：https://docs.modrinth.com/api/
上游实现（权威字段定义）：`/root/workspace/upstream/modrinth-code/apps/labrinth`（Rust）

## 一、基础信息

- Base URL：`https://api.modrinth.com/`
- 版本前缀：`/v1/`
- 认证（需要时）：`Authorization: Bearer <token>`（PAT 或 OAuth access token）
- 响应：JSON（UTF-8）
- 限流：未认证约 60 请求/分钟量级，认证后更高；超限返回 `429`，注意响应头限流信息

## 二、本项目已用接口

### 1. 项目详情

```
GET /v1/project/{id or slug}
```

返回 Project 对象（节选字段）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 项目 ID（base62） |
| `slug` | string | 短链接标识 |
| `title` | string | 标题 |
| `description` | string | 简介 |
| `categories` | string[] | 分类标签 |
| `downloads` | number | 下载量 |
| `icon_url` | string? | 图标地址 |
| `team` | string | 作者团队 ID |
| `versions` | string[] | 版本 ID 列表 |
| `game_versions` | string[] | 支持的游戏版本 |

> ⚠️ 当前 DTO `ProjectResponse` 里的 `author` 字段并非该接口直接返回（作者需从 `team` 再查），需调整。

### 2. 搜索

```
GET /v1/search?query={q}&limit=20&offset=0&facets=[["project_type:mod"]]
```

**关键：返回结构是对象，不是数组**

```json
{
  "hits": [
    {
      "project_id": "P7dR8mSH",
      "slug": "fabric-api",
      "title": "Fabric API",
      "description": "...",
      "categories": ["fabric", "library"],
      "downloads": 123456789,
      "icon_url": "..."
    }
  ],
  "offset": 0,
  "limit": 20,
  "total_hits": 12345
}
```

> ⚠️ 当前代码把搜索返回解析成 `List<ProjectResponse>`，**运行时会解析失败**，需新增包装类：
> `data class SearchResult(val hits: List<ProjectSearchHit>, val offset: Int, val limit: Int, val total_hits: Int)`

### 3. 分类标签

```
GET /v1/tag/category
```

返回对象数组：

```json
[
  { "name": "adventure", "project_type": "mod", "header": "Categories", "icon": "...", "svg": "..." }
]
```

> ⚠️ 当前代码解析为 `List<String>` 且路径写的是 `/v1/category`，均需改为 `/v1/tag/category` + `List<CategoryTag>`。

## 三、后续会用到的接口

| 用途 | 接口 |
|------|------|
| 项目版本列表 | `GET /v1/project/{id}/version` |
| 单个版本 | `GET /v1/version/{id}` |
| 版本文件下载 | 版本对象内 `files[].url` |
| 批量项目 | `GET /v1/projects?ids=["..."]` |
| 用户信息 | `GET /v1/user/{id}` |
| 当前登录用户 | `GET /v1/user`（需认证） |
| 收藏（用户项目） | `GET /v1/user/{id}/projects` |

## 四、错误码

| 码 | 含义 | 处理建议 |
|----|------|----------|
| 400 | 参数错误 | 检查 query 参数 |
| 401 | 未认证/失效 | 重新登录或校验 PAT |
| 404 | 资源不存在 | 提示项目不存在 |
| 429 | 触发限流 | 退避重试 + 缓存 |
| 5xx | 服务端错误 | 有限次重试 |

## 五、客户端实现建议

1. **统一响应包装**：搜索类接口一律用 `xxxResult` 包装类，别用裸列表
2. **缓存**：项目详情/搜索可做内存 + 磁盘缓存，降低限流风险
3. **重试**：对 5xx 与 429 做指数退避，最多 3 次
4. **User-Agent**：建议按 Modrinth 规范设置可识别的 UA
5. **分页**：search 用 `limit` + `offset`，UI 侧做增量加载

## 六、待修正清单（对应代码）

- [ ] `ProjectResponse`：`author` 字段来源修正；补 `slug`、`icon_url` 命名对齐
- [ ] 新增 `SearchResult` 包装类，改 `searchProjects` 返回类型
- [ ] 新增 `CategoryTag` 模型，改 `getCategories` 路径与返回类型
- [ ] `Project` 领域模型补 `slug`、`iconUrl` 字段
