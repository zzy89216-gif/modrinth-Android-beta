package com.modrinth.domain.repository

import com.modrinth.domain.model.Category
import com.modrinth.domain.model.Project
import com.modrinth.domain.model.SearchResult
import com.modrinth.domain.model.Version

/**
 * 项目数据仓储。所有方法在网络/解析失败时降级为 null / emptyList / 空 [SearchResult]，
 * 不向调用方抛异常（保持既有风格）。
 */
interface ProjectRepository {
    /** GET /v1/project/{id}，404 或异常时返回 null。 */
    suspend fun getProject(projectId: String): Project?

    /**
     * GET /v1/search（query + offset + limit）。
     * 真实响应是 {hits, offset, limit, total_hits} 包装，不是裸数组。
     */
    suspend fun searchProjects(query: String, offset: Int = 0, limit: Int = 20): SearchResult

    /** 兼容旧的 page 分页风格（page 从 1 开始）。 */
    suspend fun searchProjectsByPage(query: String, page: Int = 1, limit: Int = 20): SearchResult {
        return searchProjects(query, offset = (page - 1) * limit, limit = limit)
    }

    /**
     * GET /v1/project/{id}/version，返回版本列表（Version 对象数组）。
     * loaders / gameVersions 需传 JSON 数组字符串（如 ["fabric"]），可选。
     */
    suspend fun getProjectVersions(
        projectId: String,
        loaders: String? = null,
        gameVersions: String? = null,
        featured: Boolean? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<Version>

    /**
     * GET /v1/tag/category，返回分类对象列表
     *（{name, icon, project_type, header, project_count?}）。
     */
    suspend fun getCategories(): List<Category>

    /**
     * 兼容旧调用方：返回分类名列表（[getCategories] 的 name 投影）。
     */
    suspend fun getCategoryNames(): List<String> {
        return getCategories().map { it.name }
    }

    /**
     * 版本主文件的下载直链。
     * 优先用 GET /v1/project/{id}/version/{version_id}/file（302 重定向到 CDN，
     * 跟随重定向后返回最终 URL；部分部署带 {"url": ...} JSON 体，一并兜底）。
     * 该路由 404 时回退：拉版本列表取 primary 文件的 url 字段。
     */
    suspend fun getFileDownloadUrl(projectId: String, versionId: String): String?
}
