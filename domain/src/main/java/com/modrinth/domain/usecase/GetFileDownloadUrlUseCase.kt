package com.modrinth.domain.usecase

import com.modrinth.domain.repository.ProjectRepository

class GetFileDownloadUrlUseCase(private val repository: ProjectRepository) {
    suspend fun execute(projectId: String, versionId: String): String? =
        repository.getFileDownloadUrl(projectId, versionId)
}
