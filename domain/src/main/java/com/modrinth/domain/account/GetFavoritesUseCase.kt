package com.modrinth.domain.account

/** 读取当前登录账户的收藏项目 id 列表；未登录返回空列表（UI 可先判断登录态）。 */
class GetFavoritesUseCase(private val repository: AccountRepository) {
    suspend fun execute(): List<String> {
        val account = repository.getCurrentAccount() ?: return emptyList()
        return repository.getFavorites(account.id)
    }
}
