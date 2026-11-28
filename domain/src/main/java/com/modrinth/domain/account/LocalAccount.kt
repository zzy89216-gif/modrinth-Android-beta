package com.modrinth.domain.account

/**
 * 纯离线本地账户。
 *
 * 数据全部保存在本机（账户列表在 filesDir/accounts.json，
 * 当前登录 id 在 DataStore Preferences），无任何网络与服务器交互。
 */
data class LocalAccount(
    val id: String,
    val username: String,
    val displayName: String = username,
    val avatarColor: Int = 0xFF6750A4.toInt(),
    val createdAt: Long = System.currentTimeMillis(),
    val favorites: List<String> = emptyList()
)
