package com.modrinth.data.repository

import com.modrinth.domain.repository.ProjectRepository

class ProjectRepositoryImpl(
    private val api: ModrinthApi
) : ProjectRepository {
    override suspend fun getProject(projectId: String) = TODO()
    override suspend fun searchProjects(query: String, page: Int, limit: Int) = TODO()
    override suspend fun getCategories() = TODO()
}
