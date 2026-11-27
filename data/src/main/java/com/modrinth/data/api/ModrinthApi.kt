package com.modrinth.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface ModrinthApi {
    @GET("v1/project/{project_id}")
    suspend fun getProject(@Path("project_id") projectId: String)

    @GET("v1/project/search")
    suspend fun searchProjects(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    )

    @GET("v1/category")
    suspend fun getCategories()
}
