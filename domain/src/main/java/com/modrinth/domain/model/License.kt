package com.modrinth.domain.model

/** 项目许可证，对应 API 的 license 对象 `{id, name, url?}`（SPDX id）。 */
data class License(
    val id: String? = null,
    val name: String? = null,
    val url: String? = null
)
