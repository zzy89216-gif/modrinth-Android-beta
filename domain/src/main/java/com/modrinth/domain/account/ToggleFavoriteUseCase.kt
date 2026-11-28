package com.modrinth.domain.account

/**
 * 切换当前登录账户对项目的收藏。
 *
 * 收藏属于当前账户；未登录时返回 Err(NOT_LOGGED_IN)，
 * UI 层应据此引导用户去注册 / 登录页。
 */
class ToggleFavoriteUseCase(private val repository: AccountRepository) {
    suspend fun execute(projectId: String): AccountResult {
        if (projectId.isEmpty()) return AccountResult.Err(AccountResult.Err.Reason.INVALID_INPUT)

        val account = repository.getCurrentAccount() ?: return AccountResult.Err(AccountResult.Err.Reason.NOT_LOGGED_IN)
        return if (repository.toggleFavorite(account.id, projectId) == null) {
            AccountResult.Err(AccountResult.Err.Reason.NOT_LOGGED_IN)
        } else {
            AccountResult.Ok
        }
    }
}
