package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * `GET /v1/project/{id}/version` 响应数组的单个版本。
 *
 * 字段对齐 labrinth `models/v2/projects.rs::LegacyVersion`
 * （/v1、/v2 兼容层实际序列化形状；早期 v1 形状见 modrinth-docs openapi.yaml
 * 的 BaseVersion + Version allOf）。
 *
 * 注意：
 * - 文件列表字段名是 `files`，每项是 [VersionFileResponse]
 * - `changelog_url` 官方标注 always null（仅为兼容保留）
 * - 没有单一的 `file` 字段（旧代码假设的 {name,url,size,sha1,sha512,pinned} 不是真实形状）
 */
data class VersionResponse(
    @SerializedName("id") val id: String = "",
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("author_id") val authorId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("version_number") val versionNumber: String? = null,
    @SerializedName("changelog") val changelog: String? = null,
    @SerializedName("changelog_url") val changelogUrl: String? = null,
    @SerializedName("date_published") val datePublished: String? = null,
    @SerializedName("downloads") val downloads: Long = 0L,
    @SerializedName("version_type") val versionType: String? = null,
    @SerializedName("featured") val featured: Boolean = false,
    @SerializedName("status") val status: String? = null,
    @SerializedName("requested_status") val requestedStatus: String? = null,
    @SerializedName("game_versions") val gameVersions: List<String> = emptyList(),
    @SerializedName("loaders") val loaders: List<String> = emptyList(),
    @SerializedName("environment") val environment: String? = null,
    @SerializedName("files") val files: List<VersionFileResponse> = emptyList(),
    @SerializedName("dependencies") val dependencies: List<VersionDependencyResponse> = emptyList()
)
