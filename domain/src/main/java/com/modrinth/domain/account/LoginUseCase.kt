package com.modrinth.domain.account

/** 登录本地账户；区分 PIN 错误与用户不存在。 */
class LoginUseCase(private val repository: AccountRepository) {
    suspend fun execute(username: String, pin: String): AccountResult {
        val name = username.trim()
        if (name.isEmpty()) return AccountResult.Err(AccountResult.Err.Reason.INVALID_INPUT)
        if (pin.isEmpty()) return AccountResult.Err(AccountResult.Err.Reason.INVALID_INPUT)

        val account = repository.login(name, pin)
        return when (account) {
            null -> AccountResult.Err(AccountResult.Err.Reason.PIN_ERROR)
            else -> AccountResult.Ok
        }
    }
}
