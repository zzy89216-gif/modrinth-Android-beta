package com.modrinth.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.modrinth.domain.model.SearchProject
import com.modrinth.domain.model.SearchResult
import com.modrinth.domain.usecase.SearchProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页：「热门模组」= 空查询词搜索的前 15 条（按 modrinth 默认排序 = 相关度/热度）。
 * 下拉刷新重新拉取。
 */
data class HomeUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val projects: List<SearchProject> = emptyList(),
    val total: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchProjects: SearchProjectsUseCase,
    /** Coil 2.7.0 无自动单例，由 CoilModule 提供 */
    val imageLoader: coil.ImageLoader
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        if (_state.value.projects.isEmpty()) load()
    }

    fun load() {
        if (_state.value.loading || _state.value.refreshing) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val result: SearchResult = searchProjects.execute("", limit = 15)
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        projects = result.hits,
                        total = result.total
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, refreshing = false, error = e.message ?: "加载失败") }
            }
        }
    }

    fun onRefresh() {
        if (_state.value.refreshing || _state.value.loading) return
        _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch {
            try {
                val result = searchProjects.execute("", limit = 15)
                _state.update {
                    it.copy(refreshing = false, projects = result.hits, total = result.total)
                }
            } catch (e: Exception) {
                _state.update { it.copy(refreshing = false, error = e.message ?: "刷新失败") }
            }
        }
    }
}
