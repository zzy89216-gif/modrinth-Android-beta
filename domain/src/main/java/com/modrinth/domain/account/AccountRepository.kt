package com.modrinth.domain.account

import kotlinx.coroutines.flow.Flow

/**
 * 纯离线本地账户仓库。
 *
 * 登录态（当前账户 id）存 DataStore Preferences，
 * 账户列表（含收藏）存 JSON 文件，全部本机读写、无网络。
 */
interface AccountRepository {

    /** 注册本地账户；用户名已存在时返回 null（由上层判定 ALREADY_EXISTS）。 */
    suspend fun register(username: String, pin: String): LocalAccount?

    /** 按用户名 + PIN 登录；PIN 错误或用户不存在返回 null。 */
    suspend fun login(username: String, pin: String): LocalAccount?

    /** 退出当前登录（清除当前账户 id，不删除账户与收藏）。 */
    suspend fun logout()

    /** 当前登录账户，未登录返回 null。 */
    suspend fun getCurrentAccount(): LocalAccount?

    /** 当前登录账户流，未登录时发射 null；登录后自动切换。 */
    fun observeCurrentAccount(): Flow<LocalAccount?>

    /** 切换收藏（已收藏则移除，未收藏则添加）；未登录返回 null。返回更新后的账户。 */
    suspend fun toggleFavorite(accountId: String, projectId: String): LocalAccount?

    /** 指定账户的收藏项目 id 列表；账户不存在返回空列表。 */
    suspend fun getFavorites(accountId: String): List<String>
}
