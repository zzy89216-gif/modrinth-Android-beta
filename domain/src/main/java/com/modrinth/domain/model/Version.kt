package com.modrinth.domain.model

/**
 * 项目版本领域模型。
 *
 * 字段对齐 labrinth `models/v2/projects.rs::LegacyVersion`
 * （/v1、/v2 的 `GET /project/{id}/version` 返回项；早期 v1 形状见 modrinth-docs
 * openapi.yaml 的 BaseVersion + Version allOf）。
 */
data class Version(
    val id: String = "",
    val projectId: String? = null,
    val authorId: String? = null,
    val name: String? = null,
    val versionNumber: String? = null,
    val changelog: String? = null,
    /** 官方标注 always null（仅为兼容保留）。 */
    val changelogUrl: String? = null,
    val datePublished: String? = null,
    val downloads: Long = 0L,
    val versionType: String? = null,
    val featured: Boolean = false,
    val status: String? = null,
    val requestedStatus: String? = null,
    val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val environment: String? = null,
    val files: List<File> = emptyList(),
    val dependencies: List<VersionDependency> = emptyList()
) {
    /** 主文件（files 里 primary=true 的那个，取第一个）。 */
    val primaryFile: File?
        get() = files.firstOrNull { it.primary } ?: files.firstOrNull()
}
