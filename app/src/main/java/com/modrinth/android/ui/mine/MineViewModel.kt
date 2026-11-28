package com.modrinth.android.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.modrinth.domain.account.AccountRepository
import com.modrinth.domain.account.AccountResult
import com.modrinth.domain.account.GetFavoritesUseCase
import com.modrinth.domain.account.LocalAccount
import com.modrinth.domain.account.LoginUseCase
import com.modrinth.domain.account.LogoutUseCase
import com.modrinth.domain.account.RegisterUseCase
import com.modrinth.domain.model.Project
import com.modrinth.domain.usecase.GetProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MineUiState(
    val username: String = "",
    val pin: String = "",
    val error: String? = null,
    val submitting: Boolean = false,
    /** 当前账户的收藏项目 */
    val favorites: List<Project> = emptyList(),
    val loadingFavorites: Boolean = false
)

/**
 * 离线本地账户页：
 * - [currentAccount] 为 null → 显示注册/登录表单
 * - 非 null → 显示头像色块 + 用户名 + 收藏列表 + 登出
 */
@HiltViewModel
class MineViewModel @Inject constructor(
    private val register: RegisterUseCase,
    private val login: LoginUseCase,
    private val logout: LogoutUseCase,
    private val getFavorites: GetFavoritesUseCase,
    private val getProject: GetProjectUseCase,
    accountRepository: AccountRepository
) : ViewModel() {

    /** 冷 Flow 各 VM 各自 stateIn（account-agent 已验证多 VM 安全） */
    val currentAccount: StateFlow<LocalAccount?> =
        accountRepository.observeCurrentAccount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _state = MutableStateFlow(MineUiState())
    val state: StateFlow<MineUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            currentAccount.collectLatest { account ->
                if (account == null) {
                    _state.update { it.copy(favorites = emptyList()) }
                } else {
                    refreshFavorites()
                }
            }
        }
    }

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPinChange(v: String) = _state.update { it.copy(pin = v, error = null) }

    /** 注册（注册即登录） */
    fun onRegister() {
        val s = _state.value
        if (s.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = register.execute(s.username, s.pin)
            _state.update {
                it.copy(
                    submitting = false,
                    error = (result as? AccountResult.Err)?.userMessage
                )
            }
        }
    }

    fun onLogin() {
        val s = _state.value
        if (s.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = login.execute(s.username, s.pin)
            _state.update {
                it.copy(
                    submitting = false,
                    error = (result as? AccountResult.Err)?.userMessage
                )
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            logout.execute()
            _state.update { it.copy(favorites = emptyList(), username = "", pin = "") }
        }
    }

    private fun refreshFavorites() {
        viewModelScope.launch {
            _state.update { it.copy(loadingFavorites = true) }
            try {
                val ids = getFavorites.execute()
                // 逐个查项目详情（beta 阶段串行即可，收藏量小）
                val projects = buildList {
                    for (id in ids) {
                        try {
                            getProject.execute(id)?.let { add(it) }
                        } catch (e: Exception) {
                            // 单项失败跳过，不影响整体
                        }
                    }
                }
                _state.update { it.copy(favorites = projects, loadingFavorites = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loadingFavorites = false) }
            }
        }
    }
}
