package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * `GET /v1/tag/category` 的响应项。
 *
 * 字段对齐 labrinth `routes/v2/tags.rs::CategoryData`（icon/name/project_type/header）。
 * 早期部分版本的该接口还会带 `project_count`（各分类下项目数），这里按可选解析，
 * 缺失时为 null，不影响解码。
 */
data class CategoryResponse(
    @SerializedName("name") val name: String = "",
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("project_type") val projectType: String? = null,
    @SerializedName("header") val header: String? = null,
    @SerializedName("project_count") val projectCount: Int? = null
)
