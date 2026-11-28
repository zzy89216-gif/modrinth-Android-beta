package com.modrinth.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * 审核员留言对象，ProjectResponse.moderator_message 的取值。
 *
 * 字段对齐 labrinth `models/v3/projects.rs::ModeratorMessage`（v2 兼容层透传）：
 * `{message, body?}`。经线上 /v2/project/{id} 真实响应核对（2026-11-28）。
 */
data class ModeratorMessageResponse(
    @SerializedName("message") val message: String = "",
    @SerializedName("body") val body: String? = null
)
