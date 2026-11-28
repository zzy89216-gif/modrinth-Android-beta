package com.modrinth.domain.model

/** 项目画廊图片，对应 API 的 gallery 数组项（/v1、/v2 兼容层的 LegacyGalleryItem）。 */
data class GalleryItem(
    val url: String? = null,
    val rawUrl: String? = null,
    val featured: Boolean = false,
    val title: String? = null,
    val description: String? = null,
    val created: String? = null,
    val ordering: Long? = null
)
