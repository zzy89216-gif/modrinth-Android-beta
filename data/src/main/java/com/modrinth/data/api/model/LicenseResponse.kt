package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/** 项目 license 对象 `{id, name, url?}`（SPDX id），对应 ProjectResponse.license。 */
data class LicenseResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null
)
