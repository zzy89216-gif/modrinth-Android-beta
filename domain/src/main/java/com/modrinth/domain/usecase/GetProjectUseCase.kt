package com.modrinth.domain.usecase

import com.modrinth.domain.model.Project
import com.modrinth.domain.repository.ProjectRepository

class GetProjectUseCase(private val repository: ProjectRepository) {
    suspend fun execute(projectId: String): Project? = repository.getProject(projectId)
}
