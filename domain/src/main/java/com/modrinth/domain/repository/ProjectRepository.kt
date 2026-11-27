package com.modrinth.domain.repository

import com.modrinth.domain.model.Project

interface ProjectRepository {
    suspend fun getProject(projectId: String): Project?
    suspend fun searchProjects(query: String, page: Int = 1, limit: Int = 20): List<Project>
    suspend fun getCategories(): List<String>
}
