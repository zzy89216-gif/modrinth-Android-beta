package com.modrinth.domain.usecase

import com.modrinth.domain.model.Project
import com.modrinth.domain.repository.ProjectRepository

class SearchProjectsUseCase(private val repository: ProjectRepository) {
    suspend fun execute(query: String, page: Int = 1, limit: Int = 20): List<Project> =
        repository.searchProjects(query, page, limit)
}
