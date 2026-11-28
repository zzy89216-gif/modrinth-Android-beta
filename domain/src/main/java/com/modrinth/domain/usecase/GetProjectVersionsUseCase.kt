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
        repository.getProjectVersions(projectId, loaders, gameVersions, featured)
}
