package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

data class ProjectResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("categories") val categories: List<String>,
    @SerializedName("versions") val versions: List<String>,
    @SerializedName("author") val author: String,
    @SerializedName("downloads") val downloads: Long,
    @SerializedName("logo_url") val logoUrl: String?
)
