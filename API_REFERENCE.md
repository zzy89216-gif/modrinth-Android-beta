# Modrinth API 参考（ModBox 实际使用）

> 官方文档：https://docs.modrinth.com/api/
> 权威字段定义：上游 `apps/labrinth`（Rust，`models/v2/*.rs` 为 `/v1`、`/v2` 兼容层实际序列化形状），
> 工作区内只读克隆，对照时不要修改上游副本。
> 更新：2026-11-28（按真实接口全量重写；旧代码错误见文末对照表）。

## 目录

1. [基础信息](#一基础信息)
2. [本项目已用接口](#二本项目已用接口)
3. [后续会用的接口](#三后续会用的接口)
4. [错误码](#四错误码)
5. [客户端实现建议](#五客户端实现建议)
6. [旧代码错误对照](#六旧代码错误对照)

## 一、基础信息

- Base URL：`https://api.modrinth.com/`，版本前缀 `/v1/`（`/v1` 是 labrinth 对 `/v2` 的兼容层，
  返回形状与 `models/v2` 一致）
- 认证（需要时）：`Authorization: Bearer <token>`，token 为 **PAT** 或 OAuth access token。
  本项目当前版本只做**离线本地账户**（PIN SHA-256），不发起联网认证；
  PAT 支持为 Phase 4（关注 / 通知 / 站内消息）预留
- 响应：JSON（UTF-8）；日期一律 RFC3339 字符串
- 限流：未认证约 60 请求/分钟量级，认证后更高；超限返回 `429`，注意响应头限流信息
- 下载：文件直链由 **302 重定向**到 CDN（`Location` 头），302 响应同时带
  `{"url": "https://cdn.modrinth.com/..."}` JSON 体（上游 `DownloadRedirect` 结构体，
  字段只有 `url`），可用于跟随重定向被禁用时的兜底

## 二、本项目已用接口

### 1. 搜索（含项目发现）

```
GET /v1/search?query={q}&offset=0&limit=20
# 可选：facets（JSON 数组字符串，如 [["project_type:mod"]]）、index、filters、version
```

**关键：返回结构是对象，不是裸数组**（labrinth `models/v2/search.rs::LegacySearchResults`）：

```json
{
  "hits": [
    {
      "project_id": "P7dR8mSH",
      "slug": "fabric-api",
      "title": "Fabric API",
      "author": "somebody",
      "author_id": "AbCdEfGh",
      "description": "短描述（对应 v3 的 summary，无 body）",
      "categories": ["fabric", "library"],
      "versions": ["1.18.1", "1.19"],
      "downloads": 123456789,
      "follows": 4200,
      "icon_url": "https://media.modrinth.com/...",
      "license": "MIT"
    }
  ],
  "offset": 0,
  "limit": 20,
  "total_hits": 12345
}
```

搜索项（`LegacyResultSearchProject`）与详情 Project 的关键差异（本项目
`SearchProjectResponse` 已按此实现）：

| 字段 | 搜索项 | 项目详情 |
|------|--------|----------|
| 项目 id | `project_id` | `id` |
| 作者 | 有 `author` / `author_id` | **没有**（作者要从 `team` 另查） |
| 关注数 | `follows` | `followers` |
| 描述 | `description`（短） | `description`（短）+ `body`（长） |
| `versions` | **游戏版本列表**（如 `["1.18.1"]`） | 版本 id 列表（base62） |
| 图标 | `icon_url`（字符串） | `icon_url` + `raw_icon_url` |
| license | SPDX 字符串 | 对象 `{id, name, url?}` |

> ⚠️ 旧代码把搜索解析成 `List<ProjectResponse>`（裸数组），运行时会解析失败。
> 现在用 `SearchResponse {hits, offset, limit, total_hits}` 包装（字段名 `total_hits` 有下划线）。

### 2. 项目详情

```
GET /v1/project/{id or slug}
```

返回 Project 对象（labrinth `models/v2/projects.rs::LegacyProject`，节选，
本项目 `ProjectResponse` 已全字段实现、均可空/默认）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 项目 ID（base62） |
| `slug` | string | 短链接标识 |
| `title` / `description` | string | 标题 / 短描述 |
| `body` | string? | **长描述**（Markdown，旧模型没有） |
| `project_type` / `status` | string? | 类型（mod/resourcepack/…）/ 状态 |
| `team` | string? | 作者团队 ID（**详情不返回作者名**） |
| `license` | object? | `{id, name, url?}`，不是字符串 |
| `downloads` / `followers` | number | 下载量 / 关注数 |
| `categories` / `additional_categories` | string[] | 分类 |
| `loaders` / `game_versions` / `environment` | string[] | 加载器 / 游戏版本 / 环境 |
| `versions` | string[] | **版本 id 列表**（不是游戏版本号） |
| `icon_url` / `raw_icon_url` | string? | 图标（压缩 / 原图） |
| `gallery` | object[] | 图库项 |
| `donation_urls` | object[]? | 捐赠链接 |
| `published` / `updated` | string | RFC3339 日期 |

### 3. 分类标签

```
GET /v1/tag/category
```

返回**对象数组**（labrinth `routes/v2/tags.rs::CategoryData`）：

```json
[
  {
    "name": "adventure",
    "project_type": "mod",
    "header": "Categories",
    "icon": "https://cdn.modrinth.com/...",
    "project_count": 12345
  }
]
```

> ⚠️ 旧代码路径写的是 `/v1/category` 且按 `List<String>` 解析，**均为错误**；
> 正确是 `/v1/tag/category` + `List<CategoryResponse>`（`project_count` 按可选解析，
> 缺失时 null 不影响解码）。

### 4. 项目版本列表

```
GET /v1/project/{id}/version?loaders=["fabric"]&game_versions=["1.18.1"]&featured=true&limit=&offset=
```

`loaders` / `game_versions` 传 **JSON 数组字符串**，可选。返回**版本对象数组**
（labrinth `models/v2/projects.rs::LegacyVersion`，本项目 `VersionResponse` 已实现）：

```json
[
  {
    "id": "AbCdEfGh",
    "project_id": "P7dR8mSH",
    "name": "1.2.0",
    "version_number": "1.2.0",
    "version_type": "release",
    "game_versions": ["1.18.1"],
    "loaders": ["fabric"],
    "downloads": 12345,
    "date_published": "2026-01-01T00:00:00Z",
    "files": [
      {
        "url": "https://cdn.modrinth.com/data/P7dR8mSH/versions/AbCdEfGh/file.jar",
        "filename": "1.2.0-universal.jar",
        "primary": true,
        "size": 123456,
        "hashes": { "sha1": "...", "sha512": "..." }
      }
    ],
    "dependencies": [
      { "project_id": "QwErTyUi", "version_id": null, "file_name": null,
        "dependency_type": "required", "environment": "all" }
    ]
  }
]
```

文件项（`VersionFile`）关键字段：**`filename`（不是 name）**、**`primary`（不是 pinned）**、
哈希在 **`hashes` 对象**里（key 是算法名 `sha1` / `sha512`，不是平铺字段）。
SHA-512 用于下载后校验（本项目下载管理器做流式校验）。

### 5. 下载直链

```
GET /v1/project/{id}/version/{version_id}/file
```

**302 重定向到 CDN**（`Location` 头），302 响应同时带 JSON 体 `{"url": "https://cdn.modrinth.com/..."}`
（上游 `DownloadRedirect` 结构体）。本项目 `getFileDownloadUrl` 的策略：

1. 请求该路由，跟随重定向取最终 CDN URL；
2. 跟随被禁用 / 拿不到 Location 时，解析 302 的 `{"url": ...}` JSON 体兜底（`ProjectFileResponse`）；
3. 路由 404 时回退：拉版本列表（接口 4）取 `primary == true` 文件的 `url` 字段。

下载器直接取 CDN URL 做 HTTP Range 流式下载（续传）+ 整体 SHA-512 流式校验。

## 三、后续会用的接口（Phase 3/4 预留，尚未接入）

| 用途 | 接口 | 备注 |
|------|------|------|
| 单个版本（按 id 或版本号） | `GET /v1/version/{id}`、`GET /v1/project/{id}/version/{id_or_number}` | 后者按版本号匹配时取**最旧**的匹配版本 |
| 批量项目 | `GET /v1/projects?ids=["..."]` | |
| 项目依赖 | `GET /v1/project/{id}/dependencies` | |
| 当前登录用户 | `GET /v1/user`（需认证） | PAT |
| 用户信息 / 项目 / 关注 | `GET /v1/user/{id_or_username}`、`/projects`、`/follows` | |
| 关注项目 | `POST /v1/project/{id}/follow` / `DELETE` | 需认证，Phase 4 |
| 站内通知 | `GET /v1/notifications` | 需认证，Phase 4 |

## 四、错误码

| 码 | 含义 | 处理建议 |
|----|------|----------|
| 400 | 参数错误 | 检查 query 参数（facets 等要合法 JSON 字符串） |
| 401 | 未认证 / 失效 | 校验 PAT（当前版本不用联网认证，遇到即说明端点选错） |
| 404 | 资源不存在 | 提示项目 / 版本不存在；下载路由 404 时走接口 5 的回退策略 |
| 429 | 触发限流 | 指数退避重试 + 缓存，最多 3 次 |
| 5xx | 服务端错误 | 有限次重试（≤3） |

## 五、客户端实现建议

1. **统一响应包装**：搜索类接口一律用包装类（`SearchResponse`），别用裸列表；
   字段全部 `@SerializedName` 显式映射 + 可空/默认值，防止后端字段缺失导致整响应解析失败。
2. **缓存**：项目详情 / 搜索做内存 + 磁盘缓存（Coil 已缓存图片），降低限流风险。
3. **重试**：对 5xx 与 429 做指数退避，最多 3 次。
4. **User-Agent**：按 Modrinth 规范设置可识别的 UA。
5. **分页**：search 用 `limit` + `offset`（本项目仓储另提供 `page` 从 1 开始的兼容入口，
   内部换算 `offset = (page-1) * limit`）。
6. **字段语义陷阱**（已踩）：详情的 `versions` 是版本 id，搜索项的 `versions` 是游戏版本；
   `follows`（搜索项）≠ `followers`（详情）；详情没有 `author`。

## 六、旧代码错误对照（2026-11-28 重构已修正项）

| # | 旧代码 | 错误 | 修正 |
|---|--------|------|------|
| 1 | `searchProjects` 走 `GET /v1/project/search`，返回 `List<ProjectResponse>` | 端点与结构都不对：真实是 `/v1/search`，返回 `{hits, offset, limit, total_hits}` 包装 | `/v1/search` + `SearchResponse` 包装 + `SearchProjectResponse` 项 |
| 2 | `getCategories` 走 `GET /v1/category`，返回 `List<String>` | 路径不存在、结构不是字符串数组 | `GET /v1/tag/category` + `List<CategoryResponse>` |
| 3 | `ProjectResponse.author` | 详情接口不返回作者（只有搜索项有） | 详情移除；`Project.author` 改可空（兼容旧调用方） |
| 4 | `ProjectResponse.logoUrl` | 字段不存在，图标是 `icon_url` / `raw_icon_url` | `iconUrl` / `rawIconUrl` |
| 5 | 版本假设单一 `file` 字段 `{name, url, size, sha1, sha512, pinned}` | 真实是 `files[]` 数组：`filename` / `primary` / `size` / `hashes.{sha1, sha512}` / `url` | `VersionResponse.files: List<VersionFileResponse>` |
| 6 | 关注数按 `follows` 读详情 | 详情字段名是 `followers`（`follows` 只在搜索项） | `followers` |
| 7 | `license` 按字符串解析 | 详情里是对象 `{id, name, url?}` | `LicenseResponse` 对象（搜索项里才是 SPDX 字符串） |
| 8 | 下载按版本对象的某 url 字段直取 | 真实下载走 302 重定向到 CDN（带 JSON 兜底体） | 接口 5 的三级策略（跟随 302 → JSON 兜底 → 版本列表 primary url） |
