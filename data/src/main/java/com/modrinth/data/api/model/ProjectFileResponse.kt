package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * 文件下载重定向的响应体。
 *
 * 真实接口是 `GET /v1/version_file/{file_hash}/download`
 * （labrinth 历史 `src/routes/version_file.rs` 与现行 `routes/v3/version_file.rs::download_version`）：
 * 返回 302 TemporaryRedirect，`Location` 头指向 CDN 文件，
 * 且 302 同时带 JSON 体 `{"url": "https://cdn.modrinth.com/..."}`
 * （后端 `DownloadRedirect` 结构体：字段只有 url）。
 * 这里用于跟随重定向被禁用时从 body 兜底取直链。
 */
data class ProjectFileResponse(
    @SerializedName("url") val url: String? = null
)
