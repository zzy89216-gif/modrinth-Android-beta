package com.modrinth.domain.usecase

import com.modrinth.domain.model.Version
import com.modrinth.domain.repository.ProjectRepository

class GetProjectVersionsUseCase(private val repository: ProjectRepository) {
    suspend fun execute(
        projectId: String,
        loaders: List<String>? = null,
        gameVersions: List<String>? = null,
        featured: Boolean? = null
    ): List<Version> =
        repository.getProjectVersions(
            projectId = projectId,
            loaders = toJsonArray(loaders),
            gameVersions = toJsonArray(gameVersions),
            featured = featured
        )

    /**
     * 后端 loaders / game_versions 查询参数要的是 JSON 数组字符串（如 `["fabric"]`）。
     * 纯 JVM 不引第三方 JSON 库，手工拼（转义 \ 和 "）。
     */
    private fun toJsonArray(items: List<String>?): String? =
        items?.joinToString(prefix = "[", postfix = "]", separator = ", ") {
            "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }
}
