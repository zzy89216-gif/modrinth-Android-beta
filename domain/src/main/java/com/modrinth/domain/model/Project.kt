package com.modrinth.domain.model

data class Project(
    val id: String,
    val title: String,
    val description: String,
    val categories: List<String>,
    val versions: List<String>,
    val author: String,
    val downloads: Long,
    val logoUrl: String?
)
