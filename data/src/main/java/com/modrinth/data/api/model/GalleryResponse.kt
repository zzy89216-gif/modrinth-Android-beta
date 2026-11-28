package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * 项目画廊图片，ProjectResponse.gallery 的数组项。
 * 字段对齐 labrinth `models/v2/projects.rs::LegacyGalleryItem`
 * （/v1、/v2 兼容层：url/raw_url/featured/title/description/created/ordering；
 * 早期 v1 openapi 的 GalleryImage 没有 raw_url，这里按可选处理）。
 */
data class GalleryResponse(
    @SerializedName("url") val url: String? = null,
    @SerializedName("raw_url") val rawUrl: String? = null,
    @SerializedName("featured") val featured: Boolean = false,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("created") val created: String? = null,
    @SerializedName("ordering") val ordering: Long? = null
)
