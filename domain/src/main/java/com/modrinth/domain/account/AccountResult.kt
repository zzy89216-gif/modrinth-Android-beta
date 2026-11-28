package com.modrinth.domain.account

/** 账户操作结果（本地轻量校验，无网络）。 */
sealed class AccountResult {
    object Ok : AccountResult()

    data class Err(
        val reason: Reason
    ) : AccountResult() {
        enum class Reason {
            /** 用户名已被占用 */
            ALREADY_EXISTS,

            /** PIN 不正确（用户不存在时同样返回此错误，避免泄露用户名） */
            PIN_ERROR,

            /** 未登录（需要登录后才能操作） */
            NOT_LOGGED_IN,

            /** 输入不合法（用户名 / PIN 为空或超长等） */
            INVALID_INPUT
        }

        val userMessage: String
            get() = when (reason) {
                Reason.ALREADY_EXISTS -> "该用户名已存在"
                Reason.PIN_ERROR -> "用户名或 PIN 不正确"
                Reason.NOT_LOGGED_IN -> "请先登录或注册账户"
                Reason.INVALID_INPUT -> "输入不合法"
            }
    }
}
