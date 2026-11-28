package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * `GET /v1/project/{id}` 的响应体。
 *
 * 字段对齐 labrinth `models/v2/projects.rs::LegacyProject`（/v1、/v2 兼容层实际序列化形状；
 * 早期 v1 形状见 modrinth-docs openapi.yaml 的 BaseProject/ServerRenderedProject/
 * NonSearchProject/Project allOf 组合）。
 *
 * 与旧版代码的差异（均为真实 API 修正）：
 * - 没有 `author` / `logo_url` 字段（详情接口不返回）；图标是 `icon_url` / `raw_icon_url`
 * - 关注数是 `followers`（`follows` 只出现在搜索项）
 * - `license` 是对象 `{id, name, url?}`，不是字符串
 * - `body` 才是长描述，`description` 是短描述
 * - 新增 slug/team/project_type/status/loaders/gallery/links 等
 * - 所有字段可空/默认，防止后端字段缺失导致整个响应解析失败
 */
data class ProjectResponse(
    @SerializedName("id") val id: String = "",
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("project_type") val projectType: String? = null,
    @SerializedName("team") val team: String? = null,
    @SerializedName("organization") val organization: String? = null,
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("body") val body: String? = null,
    @SerializedName("body_url") val bodyUrl: String? = null,
    @SerializedName("published") val published: String? = null,
    @SerializedName("updated") val updated: String? = null,
    @SerializedName("approved") val approved: String? = null,
    @SerializedName("queued") val queued: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("requested_status") val requestedStatus: String? = null,
    @SerializedName("moderator_message") val moderatorMessage: ModeratorMessageResponse? = null,
    @SerializedName("license") val license: LicenseResponse? = null,
    @SerializedName("downloads") val downloads: Long = 0L,
    @SerializedName("followers") val followers: Long = 0L,
    @SerializedName("categories") val categories: List<String> = emptyList(),
    @SerializedName("additional_categories") val additionalCategories: List<String> = emptyList(),
    @SerializedName("loaders") val loaders: List<String> = emptyList(),
    @SerializedName("versions") val versions: List<String> = emptyList(),
    @SerializedName("game_versions") val gameVersions: List<String> = emptyList(),
    @SerializedName("environment") val environment: List<String> = emptyList(),
    @SerializedName("icon_url") val iconUrl: String? = null,
    @SerializedName("raw_icon_url") val rawIconUrl: String? = null,
    @SerializedName("issues_url") val issuesUrl: String? = null,
    @SerializedName("source_url") val sourceUrl: String? = null,
    @SerializedName("wiki_url") val wikiUrl: String? = null,
    @SerializedName("discord_url") val discordUrl: String? = null,
    @SerializedName("donation_urls") val donationUrls: List<DonationUrlResponse>? = null,
    @SerializedName("gallery") val gallery: List<GalleryResponse> = emptyList(),
    @SerializedName("color") val color: Long? = null,
    @SerializedName("thread_id") val threadId: String? = null,
    @SerializedName("monetization_status") val monetizationStatus: String? = null,
    @SerializedName("client_side") val clientSide: String? = null,
    @SerializedName("server_side") val serverSide: String? = null
)
