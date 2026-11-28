package com.modrinth.domain.model

/** 捐赠链接，对应 API 的 donation_urls 数组项 `{id, platform, url}`。 */
data class DonationUrl(
    val id: String? = null,
    val platform: String? = null,
    val url: String? = null
)
