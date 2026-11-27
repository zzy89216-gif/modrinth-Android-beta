package com.modrinth.domain.usecase

import com.modrinth.domain.repository.ProjectRepository

class GetProjectUseCase(private val repository: ProjectRepository) {
    suspend fun execute(projectId: String) = repository.getProject(projectId)
}
