package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * `GET /v1/search` 响应里 hits 数组的单个搜索项。
 *
 * 字段对齐 labrinth `models/v2/search.rs::LegacyResultSearchProject`
 * （/v1、/v2 兼容层实际序列化形状；早期 v1 形状见 modrinth-docs openapi.yaml 的 ProjectResult）。
 *
 * 注意与 [ProjectResponse] 的区别：
 * - 有 `author`（字符串，作者名）/ `author_id` / `organization`
 * - 关注数是 `follows`（不是 `followers`）
 * - `description` 是短描述（对应 v3 的 summary），没有 `body`
 * - `versions` 在这里是**游戏版本列表**（如 ["1.18.1"]），不是版本 id
 * - `license` 是 SPDX 字符串
 * - `icon_url` 是字符串，gallery 是图片 URL 字符串列表
 * - 无 slug 以外的 id，项目 id 字段名是 `project_id`
 * - 全部可空/默认：搜索项字段可能缺失
 */
data class SearchProjectResponse(
    @SerializedName("project_id") val projectId: String = "",
    @SerializedName("project_type") val projectType: String? = null,
    @SerializedName("all_project_types") val allProjectTypes: List<String>? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("author_id") val authorId: String? = null,
    @SerializedName("organization") val organization: String? = null,
    @SerializedName("organization_id") val organizationId: String? = null,
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("categories") val categories: List<String> = emptyList(),
    @SerializedName("display_categories") val displayCategories: List<String> = emptyList(),
    @SerializedName("versions") val versions: List<String> = emptyList(),
    @SerializedName("downloads") val downloads: Long = 0L,
    @SerializedName("follows") val follows: Long = 0L,
    @SerializedName("icon_url") val iconUrl: String? = null,
    @SerializedName("date_created") val dateCreated: String? = null,
    @SerializedName("date_modified") val dateModified: String? = null,
    @SerializedName("latest_version") val latestVersion: String? = null,
    @SerializedName("license") val license: String? = null,
    @SerializedName("client_side") val clientSide: String? = null,
    @SerializedName("server_side") val serverSide: String? = null,
    @SerializedName("environment") val environment: List<String>? = null,
    @SerializedName("disclosure_types") val disclosureTypes: List<String>? = null,
    @SerializedName("gallery") val gallery: List<String> = emptyList(),
    @SerializedName("featured_gallery") val featuredGallery: String? = null,
    @SerializedName("color") val color: Long? = null
)
