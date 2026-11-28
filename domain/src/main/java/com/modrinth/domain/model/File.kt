package com.modrinth.domain.model

/**
 * 版本文件领域模型。
 *
 * 字段对齐 labrinth `models/v3/projects.rs::VersionFile`（v2 兼容层原样透传）：
 * `{id?, hashes: {sha1, sha512}, url, filename, primary, size, file_type?}`。
 * 注意真实字段名是 `filename`（不是 `name`），`primary`（不是 `pinned`）。
 */
data class File(
    val url: String? = null,
    val filename: String? = null,
    val size: Long = 0L,
    val sha1: String? = null,
    val sha512: String? = null,
    val primary: Boolean = false,
    val fileType: String? = null
)
