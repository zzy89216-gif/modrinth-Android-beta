package com.modrinth.domain.model

/**
 * 项目详情领域模型。
 *
 * 字段对齐 Modrinth 官方 API 的 Project 结构（labrinth `models/v2/projects.rs::LegacyProject`，
 * 即 /v1、/v2 兼容层返回的形状；早期 v1 形状见 modrinth-docs openapi.yaml 的
 * BaseProject / ServerRenderedProject / NonSearchProject / Project allOf）。
 *
 * 注意：
 * - 详情接口返回体里**没有** `author` 字段（只有搜索项有），因此这里为可空。
 * - 图标字段是 `icon_url`（以及未压缩的 `raw_icon_url`），不存在 `logo_url`。
 * - `followers` 才是关注数字段；`follows` 是搜索项里的字段名。
 * - 日期为 RFC3339 字符串，纯 JVM 不做解析，交给展示层。
 */
data class Project(
    val id: String,
    val slug: String? = null,
    val projectType: String? = null,
    val team: String? = null,
    val organization: String? = null,
    val title: String = "",
    val description: String = "",
    val body: String? = null,
    val bodyUrl: String? = null,
    val published: String? = null,
    val updated: String? = null,
    val approved: String? = null,
    val queued: String? = null,
    val status: String? = null,
    val requestedStatus: String? = null,
    val license: License? = null,
    val downloads: Long = 0L,
    val followers: Long = 0L,
    /** 兼容旧调用方的作者名；详情接口不返回该字段时为 null。 */
    val author: String? = null,
    val categories: List<String> = emptyList(),
    val additionalCategories: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    /** 项目版本 id 列表（base62），不是游戏版本号；游戏版本看 [gameVersions]。 */
    val versions: List<String> = emptyList(),
    val gameVersions: List<String> = emptyList(),
    val iconUrl: String? = null,
    val rawIconUrl: String? = null,
    val issuesUrl: String? = null,
    val sourceUrl: String? = null,
    val wikiUrl: String? = null,
    val discordUrl: String? = null,
    val donationUrls: List<DonationUrl> = emptyList(),
    val gallery: List<GalleryItem> = emptyList(),
    val color: Long? = null,
    val threadId: String? = null,
    val monetizationStatus: String? = null,
    val clientSide: String? = null,
    val serverSide: String? = null
)
