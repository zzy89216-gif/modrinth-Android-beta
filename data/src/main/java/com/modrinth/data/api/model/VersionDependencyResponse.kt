package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * 版本依赖项，VersionResponse.dependencies 的数组项。
 * 字段对齐 labrinth `models/v3/projects.rs::Dependency`（v2 兼容层透传；
 * attribution 字段较新，这里按可选 Map 忽略内容即可，不影响其余字段解码）。
 */
data class VersionDependencyResponse(
    @SerializedName("version_id") val versionId: String? = null,
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("dependency_type") val dependencyType: String? = null,
    @SerializedName("attribution") val attribution: Map<String, Any?>? = null
)
