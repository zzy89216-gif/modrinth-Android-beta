package com.modrinth.domain.model

/**
 * 搜索分页结果，对应 API 的 SearchResults 包装：
 * `{"hits": [...], "offset": 0, "limit": 20, "total_hits": 123}`。
 *
 * 真实响应里的总数字段名是 `total_hits`（labrinth `models/v2/search.rs::LegacySearchResults`）；
 * 领域层按任务约定命名为 [total]。
 */
data class SearchResult(
    val hits: List<SearchProject> = emptyList(),
    val offset: Int = 0,
    val total: Int = 0
) {
    /** 是否还有下一页。 */
    fun hasMore(): Boolean = hits.isNotEmpty() && offset + hits.size < total
}
