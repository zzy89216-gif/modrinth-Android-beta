package com.modrinth.domain.model

/**
 * 版本依赖项，对应 API 的 dependencies 数组项。
 * 字段对齐 labrinth `models/v3/projects.rs::Dependency`（v2 兼容层透传）。
 */
data class VersionDependency(
    val versionId: String? = null,
    val projectId: String? = null,
    val fileName: String? = null,
    val dependencyType: String? = null
)
