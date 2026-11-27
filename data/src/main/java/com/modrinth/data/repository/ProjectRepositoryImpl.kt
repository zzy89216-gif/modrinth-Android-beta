package com.modrinth.data.repository

import com.modrinth.data.api.ModrinthApiImpl
import com.modrinth.data.api.model.ProjectResponse
import com.modrinth.domain.model.Project
import com.modrinth.domain.repository.ProjectRepository

class ProjectRepositoryImpl(
    private val api: ModrinthApiImpl
) : ProjectRepository {
    override suspend fun getProject(projectId: String): Project? {
        return try {
            val response = api.getProject(projectId)
            mapToProject(response)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun searchProjects(query: String, page: Int, limit: Int): List<Project> {
        return try {
            val responses = api.searchProjects(query, page, limit)
            responses.map { mapToProject(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCategories(): List<String> {
        return try {
            api.getCategories()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapToProject(response: ProjectResponse): Project {
        return Project(
            id = response.id,
            title = response.title,
            description = response.description,
            categories = response.categories,
            versions = response.versions,
            author = response.author,
            downloads = response.downloads,
            logoUrl = response.logoUrl
        )
    }
}
