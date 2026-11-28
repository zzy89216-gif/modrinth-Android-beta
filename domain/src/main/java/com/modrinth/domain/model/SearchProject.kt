package com.modrinth.domain.model

/**
 * 搜索结果里的简化项目（raw 搜索形状）。
 *
 * 字段对齐 labrinth `models/v2/search.rs::LegacyResultSearchProject`
 * （/v1、/v2 搜索兼容层实际序列化形状；早期 v1 形状见 modrinth-docs openapi.yaml 的 ProjectResult）。
 *
 * 与 [Project]（详情）的区别：
 * - 有 `author`（作者名）/ `authorId` / `organization`，详情接口没有
 * - 关注数是 `follows`（详情里叫 `followers`）
 * - `versions` 是**游戏版本列表**（如 "1.18.1"），不是版本 id
 * - `license` 是 SPDX 字符串，`iconUrl`/`gallery` 是字符串/URL 列表
 * - 项目 id 字段名是 `projectId`（JSON 里 `project_id`）
 * - 字段可能缺失，全部可空/默认值
 */
data class SearchProject(
    val projectId: String = "",
    val projectType: String? = null,
    val allProjectTypes: List<String> = emptyList(),
    val slug: String? = null,
    val author: String? = null,
    val authorId: String? = null,
    val organization: String? = null,
    val organizationId: String? = null,
    val title: String = "",
    val description: String = "",
    val categories: List<String> = emptyList(),
    val displayCategories: List<String> = emptyList(),
    /** 该搜索项支持的游戏版本列表（如 "1.18.1"）。 */
    val versions: List<String> = emptyList(),
    val downloads: Long = 0L,
    val follows: Long = 0L,
    val iconUrl: String? = null,
    val dateCreated: String? = null,
    val dateModified: String? = null,
    val latestVersion: String? = null,
    val license: String? = null,
    val clientSide: String? = null,
    val serverSide: String? = null,
    val environment: List<String> = emptyList(),
    val disclosureTypes: List<String> = emptyList(),
    /** 搜索项的 gallery 是图片 URL 字符串列表（不是对象数组）。 */
    val gallery: List<String> = emptyList(),
    val featuredGallery: String? = null,
    val color: Long? = null
) {
    /**
     * 映射成 [Project]（供只消费 Project 的旧调用方使用）。
     * 注意：[follows] 映射到 [Project.followers]；搜索项没有详情字段（body/status/license 对象等），
     * 映射后这些字段保持默认值；[versions] 在两边含义不同，搜索项里是游戏版本，映射到 [Project.gameVersions]。
     */
    fun toProject(): Project = Project(
        id = projectId,
        slug = slug,
        projectType = projectType,
        title = title,
        description = description,
        published = dateCreated,
        updated = dateModified,
        license = License(id = license),
        downloads = downloads,
        followers = follows,
        author = author,
        categories = categories,
        additionalCategories = displayCategories,
        gameVersions = versions,
        iconUrl = iconUrl,
        color = color,
        clientSide = clientSide,
        serverSide = serverSide
    )
}
