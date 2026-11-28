package com.modrinth.domain.usecase

import com.modrinth.domain.model.Category
import com.modrinth.domain.repository.ProjectRepository

class GetCategoriesUseCase(private val repository: ProjectRepository) {
    suspend fun execute(): List<Category> = repository.getCategories()
}
