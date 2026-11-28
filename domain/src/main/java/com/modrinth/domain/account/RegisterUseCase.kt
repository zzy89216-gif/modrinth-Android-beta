package com.modrinth.domain.account

/** 注册本地账户。输入不合法 → Err(INVALID_INPUT)；用户名已存在 → Err(ALREADY_EXISTS)。 */
class RegisterUseCase(private val repository: AccountRepository) {
    suspend fun execute(username: String, pin: String): AccountResult {
        val name = username.trim()
        if (name.isEmpty() || name.length > 20) return AccountResult.Err(AccountResult.Err.Reason.INVALID_INPUT)
        if (pin.length !in 4..8) return AccountResult.Err(AccountResult.Err.Reason.INVALID_INPUT)

        val account = repository.register(name, pin) ?: return AccountResult.Err(AccountResult.Err.Reason.ALREADY_EXISTS)
        // 注册即登录
        return AccountResult.Ok
    }
}
