package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * `GET /v1/search` 的响应包装（不是裸数组！）。
 *
 * 真实形状（labrinth `models/v2/search.rs::LegacySearchResults`）：
 * `{"hits": [...], "offset": 0, "limit": 20, "total_hits": 123}`
 *
 * 总数字段名是 `total_hits`（领域层 SearchResult.total 与之对应）。
 */
data class SearchResponse(
    @SerializedName("hits") val hits: List<SearchProjectResponse> = emptyList(),
    @SerializedName("offset") val offset: Int = 0,
    @SerializedName("limit") val limit: Int = 0,
    @SerializedName("total_hits") val totalHits: Int = 0
)
