package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * 版本文件，VersionResponse.files 的数组项。
 *
 * 字段对齐 labrinth `models/v3/projects.rs::VersionFile`（v2 兼容层原样透传）：
 * `{id?, hashes: {"sha1": ..., "sha512": ...}, url, filename, primary, size, file_type?}`
 *
 * 注意真实字段名是 `filename`（不是 name），`primary`（不是 pinned），
 * 哈希在 `hashes` 对象里（不是平铺的 sha1/sha512 字段）。
 */
data class VersionFileResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("hashes") val hashes: FileHashesResponse = FileHashesResponse(),
    @SerializedName("url") val url: String? = null,
    @SerializedName("filename") val filename: String? = null,
    @SerializedName("primary") val primary: Boolean = false,
    @SerializedName("size") val size: Long = 0L,
    @SerializedName("file_type") val fileType: String? = null
)

/** 文件哈希对象：`{"sha1": "...", "sha512": "..."}`（key 是算法名）。 */
data class FileHashesResponse(
    @SerializedName("sha1") val sha1: String? = null,
    @SerializedName("sha512") val sha512: String? = null
)
