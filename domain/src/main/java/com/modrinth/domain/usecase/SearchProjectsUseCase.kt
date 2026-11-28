package com.modrinth.domain.usecase

import com.modrinth.domain.model.Project
import com.modrinth.domain.model.SearchProject
import com.modrinth.domain.model.SearchResult
import com.modrinth.domain.repository.ProjectRepository

class SearchProjectsUseCase(private val repository: ProjectRepository) {

    /** 兼容旧调用方：返回第一页 hits 映射成的 List<Project>。 */
    suspend fun execute(query: String, page: Int = 1, limit: Int = 20): List<Project> =
        executePaged(query, page, limit).hits.map { it.toProject() }

    /** 按 offset 分页搜索，返回完整分页信息（hits / offset / total）。 */
    suspend fun executePaged(query: String, offset: Int = 0, limit: Int = 20): SearchResult =
        repository.searchProjects(query, offset, limit)

    /** 按 page 分页搜索（page 从 1 开始）。 */
    suspend fun executePagedByPage(query: String, page: Int = 1, limit: Int = 20): SearchResult =
        repository.searchProjectsByPage(query, page, limit)

    /** 只取第一页原始搜索项（SearchProject 列表）。 */
    suspend fun executeFirstPage(query: String, limit: Int = 20): List<SearchProject> =
        executePaged(query, offset = 0, limit = limit).hits
}
