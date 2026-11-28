package com.modrinth.data.repository

import com.modrinth.data.api.ModrinthApiImpl
import com.modrinth.data.api.model.CategoryResponse
import com.modrinth.data.api.model.DonationUrlResponse
import com.modrinth.data.api.model.GalleryResponse
import com.modrinth.data.api.model.LicenseResponse
import com.modrinth.data.api.model.ProjectFileResponse
import com.modrinth.data.api.model.ProjectResponse
import com.modrinth.data.api.model.SearchProjectResponse
import com.modrinth.data.api.model.SearchResponse
import com.modrinth.data.api.model.VersionDependencyResponse
import com.modrinth.data.api.model.VersionFileResponse
import com.modrinth.data.api.model.VersionResponse
import com.modrinth.domain.model.Category
import com.modrinth.domain.model.DonationUrl
import com.modrinth.domain.model.File
import com.modrinth.domain.model.GalleryItem
import com.modrinth.domain.model.License
import com.modrinth.domain.model.Project
import com.modrinth.domain.model.SearchProject
import com.modrinth.domain.model.SearchResult
import com.modrinth.domain.model.Version
import com.modrinth.domain.model.VersionDependency
import com.modrinth.domain.repository.ProjectRepository
import retrofit2.HttpException
import retrofit2.Response

/**
 * [ProjectRepository] 的 Modrinth 后端实现。
 *
 * 所有网络调用 try/catch：失败时返回 null / emptyList / 空 [SearchResult]，不向上抛异常。
 * 响应模型字段与线上 API 一致（2026-11-28 经 api.modrinth.com/v2 真实响应
 * + 线上 openapi/v2.json 逐字段核对，见 /tmp/api-verification.md）。
 */
class ProjectRepositoryImpl(
    private val api: ModrinthApiImpl
) : ProjectRepository {

    override suspend fun getProject(projectId: String): Project? {
        return try {
            api.getProject(projectId).toProject()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun searchProjects(query: String, offset: Int, limit: Int): SearchResult {
        return try {
            api.searchProjects(query = query, offset = offset, limit = limit).toSearchResult()
        } catch (e: Exception) {
            SearchResult()
        }
    }

    override suspend fun getProjectVersions(
        projectId: String,
        loaders: String?,
        gameVersions: String?,
        featured: Boolean?,
        limit: Int?,
        offset: Int?
    ): List<Version> {
        return try {
            api.getProjectVersions(projectId, loaders, gameVersions, featured, limit, offset)
                .map { it.toVersion() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCategories(): List<Category> {
        return try {
            api.getCategories().map { it.toCategory() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getFileDownloadUrl(projectId: String, versionId: String): String? {
        // 主路径：按 versionId 取版本元数据，files[].url 本身就是 CDN 直链
        //（线上核对：https://cdn.modrinth.com/data/{project}/versions/{version}/{filename}）。
        try {
            val version = api.getVersion(projectId, versionId)
            val url = version.toVersion().primaryFile?.url
            if (!url.isNullOrBlank()) return url
        } catch (e: Exception) {
            // 落到兜底
        }

        // 兜底：GET version_file/{hash}/download 的 302（TemporaryRedirect）。
        // 客户端 followRedirects(false)：Retrofit 对 3xx 返回 code 在 300..399 的 Response，
        // 优先读 Location 头，其次解析 JSON 体 {"url": ...}（DownloadRedirect 结构体）。
        return try {
            val version = api.getVersion(projectId, versionId)
            val file = version.files.firstOrNull { it.primary } ?: version.files.firstOrNull()
                ?: return null
            val hash = file.hashes.sha512 ?: file.hashes.sha1 ?: return null
            val response: Response<ProjectFileResponse> = api.downloadFile(hash)
            try {
                parseRedirect(response)
            } finally {
                response.raw().close()
            }
        } catch (e: HttpException) {
            // 个别 OkHttp/Retrofit 版本对 3xx 抛 HttpException：从异常里读
            val r = e.response() as? Response<ProjectFileResponse> ?: return null
            try {
                parseRedirect(r)
            } finally {
                r.raw().close()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 从 302 响应里取 CDN 直链：优先 Location 头，其次 JSON 体 {"url": ...}。 */
    private fun parseRedirect(response: Response<ProjectFileResponse>): String? {
        if (response.code() !in 300..399) return null
        return response.headers().get("Location")?.takeIf { it.isNotBlank() }
            ?: response.body()?.url?.takeIf { it.isNotBlank() }
    }

    // ---------- 映射（data 响应 -> domain 模型） ----------

    private fun ProjectResponse.toProject(): Project = Project(
        id = id,
        slug = slug,
        projectType = projectType,
        team = team,
        organization = organization,
        title = title,
        description = description,
        body = body,
        bodyUrl = bodyUrl,
        published = published,
        updated = updated,
        approved = approved,
        queued = queued,
        status = status,
        requestedStatus = requestedStatus,
        license = license?.toLicense(),
        downloads = downloads,
        followers = followers,
        author = null, // 详情接口不返回 author（线上核对）
        categories = categories,
        additionalCategories = additionalCategories,
        loaders = loaders,
        versions = versions,
        gameVersions = gameVersions,
        iconUrl = iconUrl,
        rawIconUrl = rawIconUrl,
        issuesUrl = issuesUrl,
        sourceUrl = sourceUrl,
        wikiUrl = wikiUrl,
        discordUrl = discordUrl,
        donationUrls = donationUrls.orEmpty().map { it.toDonationUrl() },
        gallery = gallery.map { it.toGalleryItem() },
        color = color,
        threadId = threadId,
        monetizationStatus = monetizationStatus,
        clientSide = clientSide,
        serverSide = serverSide
    )

    private fun LicenseResponse.toLicense(): License = License(
        id = id,
        name = name,
        url = url
    )

    private fun DonationUrlResponse.toDonationUrl(): DonationUrl = DonationUrl(
        id = id,
        platform = platform,
        url = url
    )

    private fun GalleryResponse.toGalleryItem(): GalleryItem = GalleryItem(
        url = url,
        rawUrl = rawUrl,
        featured = featured,
        title = title,
        description = description,
        created = created,
        ordering = ordering
    )

    private fun SearchProjectResponse.toSearchProject(): SearchProject = SearchProject(
        projectId = projectId,
        projectType = projectType,
        allProjectTypes = allProjectTypes.orEmpty(),
        slug = slug,
        author = author,
        authorId = authorId,
        organization = organization,
        organizationId = organizationId,
        title = title,
        description = description,
        categories = categories,
        displayCategories = displayCategories,
        versions = versions,
        downloads = downloads,
        follows = follows,
        iconUrl = iconUrl,
        dateCreated = dateCreated,
        dateModified = dateModified,
        latestVersion = latestVersion,
        license = license,
        clientSide = clientSide,
        serverSide = serverSide,
        environment = environment.orEmpty(),
        disclosureTypes = disclosureTypes.orEmpty(),
        gallery = gallery,
        featuredGallery = featuredGallery,
        color = color
    )

    private fun SearchResponse.toSearchResult(): SearchResult = SearchResult(
        hits = hits.map { it.toSearchProject() },
        offset = offset,
        total = totalHits
    )

    private fun CategoryResponse.toCategory(): Category = Category(
        name = name,
        icon = icon.orEmpty(),
        projectType = projectType,
        header = header,
        projectCount = projectCount
    )

    private fun VersionResponse.toVersion(): Version = Version(
        id = id,
        projectId = projectId,
        authorId = authorId,
        name = name,
        versionNumber = versionNumber,
        changelog = changelog,
        changelogUrl = changelogUrl,
        datePublished = datePublished,
        downloads = downloads,
        versionType = versionType,
        featured = featured,
        status = status,
        requestedStatus = requestedStatus,
        gameVersions = gameVersions,
        loaders = loaders,
        environment = environment,
        files = files.map { it.toFile() },
        dependencies = dependencies.map { it.toDependency() }
    )

    private fun VersionFileResponse.toFile(): File = File(
        url = url,
        filename = filename,
        size = size,
        sha1 = hashes.sha1,
        sha512 = hashes.sha512,
        primary = primary,
        fileType = fileType
    )

    private fun VersionDependencyResponse.toDependency(): VersionDependency = VersionDependency(
        versionId = versionId,
        projectId = projectId,
        fileName = fileName,
        dependencyType = dependencyType
    )
}
