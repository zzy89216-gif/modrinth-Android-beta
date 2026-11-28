package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/** 捐赠链接，ProjectResponse.donation_urls 的数组项 `{id, platform, url}`。 */
data class DonationUrlResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("platform") val platform: String? = null,
    @SerializedName("url") val url: String? = null
)
