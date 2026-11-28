package com.modrinth.data.api

import com.modrinth.data.api.model.CategoryResponse
import com.modrinth.data.api.model.ProjectFileResponse
import com.modrinth.data.api.model.ProjectResponse
import com.modrinth.data.api.model.SearchResponse
import com.modrinth.data.api.model.VersionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Modrinth 后端 REST 接口。
 *
 * 路径与字段以官方后端为准（labrinth 源码 + 线上 `https://api.modrinth.com/openapi/v2.json`
 * 2026-11-28 实际拉取核对）：
 * - 搜索是 `{hits, offset, limit, total_hits}` 包装，不是裸数组
 * - 分类是 `GET tag/category`（`[{icon,name,project_type,header}]`），不是 `/category` 字符串数组
 * - 版本是 `project/{id}/version` 的 `LegacyVersion` 对象数组
 * - 文件下载是 `GET version_file/{file_hash}/download`：302 重定向到 CDN，
 *   响应体是 `{"url": "https://cdn.modrinth.com/..."}`（DownloadRedirect）
 *
 * 版本前缀（v1）集中在 [com.modrinth.data.di.DataModule] 的 baseUrl 里；
 * 若线上对 v1 前缀的路由返回 410（upstream 当前 main 已把 /api/v1 标记 api_deprecated），
 * 只需把 baseUrl 换成 "https://api.modrinth.com/v2/"（/v2 提供完全相同的 Legacy 形状，已线上验证）。
 */
interface ModrinthApiImpl {

    /** 项目详情，`GET project/{project_id}`（id 或 slug 均可）。 */
    @GET("project/{project_id}")
    suspend fun getProject(@Path("project_id") projectId: String): ProjectResponse

    /**
     * 搜索，`GET search`。
     * 真实响应是 [SearchResponse]（hits/offset/limit/total_hits 包装）。
     * facets / index 可选；offset+limit 分页。
     */
    @GET("search")
    suspend fun searchProjects(
        @Query("query") query: String,
        @Query("facets") facets: String? = null,
        @Query("index") index: String = "relevance",
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): SearchResponse

    /**
     * 项目版本列表，`GET project/{project_id}/version`，返回 Version 对象数组。
     * loaders / gameVersions 是 JSON 数组字符串（如 `["fabric"]`），可选。
     */
    @GET("project/{project_id}/version")
    suspend fun getProjectVersions(
        @Path("project_id") projectId: String,
        @Query("loaders") loaders: String? = null,
        @Query("game_versions") gameVersions: String? = null,
        @Query("featured") featured: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): List<VersionResponse>

    /**
     * 单个版本，`GET project/{project_id}/version/{version_id}`（version id 或版本号）。
     * 用于按 versionId 取主文件下载直链。
     */
    @GET("project/{project_id}/version/{version_id}")
    suspend fun getVersion(
        @Path("project_id") projectId: String,
        @Path("version_id") versionId: String
    ): VersionResponse

    /**
     * 分类列表，`GET tag/category`。
     * 返回 `[{icon, name, project_type, header}]`（2026-11-28 线上实际为对象数组；
     * 更老的 v1 曾返回纯字符串数组，[CategoryResponse] 的 name 字段可兜底解析字符串项时不兼容，
     * 仓库层按对象数组解析并 try/catch 兜底）。
     */
    @GET("tag/category")
    suspend fun getCategories(): List<CategoryResponse>

    /**
     * 文件下载，`GET version_file/{file_hash}/download`。
     * 真实行为：302 TemporaryRedirect，`Location` 头指向 CDN 文件，
     * 302 体是 [ProjectFileResponse]（`{"url": "https://cdn.modrinth.com/..."}`）。
     * 客户端 `followRedirects(false)`，Retrofit 对 302 抛 [retrofit2.HttpException]，
     * 由仓库层从异常里读 Location / 解析 JSON 体取直链（绝不下载文件本体）。
     */
    @GET("version_file/{file_hash}/download")
    suspend fun downloadFile(
        @Path("file_hash") fileHash: String
    ): Response<ProjectFileResponse>
}
