package com.modrinth.domain.account

/** 退出当前登录（不删除账户与收藏数据）。 */
class LogoutUseCase(private val repository: AccountRepository) {
    suspend fun execute() {
        repository.logout()
    }
}
