package com.modrinth.domain.model

/**
 * 项目分类领域模型。
 *
 * 字段对齐 labrinth `routes/v2/tags.rs::CategoryData`（`GET /tag/category` 的返回项；
 * 早期 v1 openapi 的 CategoryTag 同形）。
 * `projectCount` 为可选：部分历史版本/镜像的该接口会带 `project_count`，新版不带。
 */
data class Category(
    val name: String = "",
    val icon: String = "",
    val projectType: String? = null,
    val header: String? = null,
    val projectCount: Int? = null
)
