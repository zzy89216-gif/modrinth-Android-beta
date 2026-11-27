package com.modrinth.data.api

import com.modrinth.data.api.model.ProjectResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ModrinthApiImpl {
    @GET("v1/project/{project_id}")
    suspend fun getProject(@Path("project_id") projectId: String): ProjectResponse

    @GET("v1/project/search")
    suspend fun searchProjects(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<ProjectResponse>

    @GET("v1/category")
    suspend fun getCategories(): List<String>
}
