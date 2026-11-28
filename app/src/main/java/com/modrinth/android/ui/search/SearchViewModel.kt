package com.modrinth.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.modrinth.domain.model.SearchProject
import com.modrinth.domain.usecase.SearchProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SearchPhase {
    data object Idle : SearchPhase
    data object Loading : SearchPhase
    data class Done(val items: List<SearchProject>, val total: Int, val hasMore: Boolean) : SearchPhase
    data class Error(val message: String) : SearchPhase
}

data class SearchUiState(
    val query: String = "",
    val phase: SearchPhase = SearchPhase.Idle,
    val loadingMore: Boolean = false
)

/**
 * 搜索：300ms 防抖 + offset 分页（每页 20）。
 * [loadMore] 由列表触底调用，仅在有下一页时生效。
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchProjects: SearchProjectsUseCase,
    /** Coil 2.7.0 无自动单例，由 CoilModule 提供 */
    val imageLoader: coil.ImageLoader
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var page: Int = 0
    private var total: Int = 0
    private var latestQuery: String? = null

    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery) }
        if (newQuery == latestQuery) return
        latestQuery = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _state.update { it.copy(phase = SearchPhase.Idle) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            page = 0
            runSearch()
        }
    }

    fun onInitialQuery(query: String) {
        if (query.isBlank()) return
        _state.update { it.copy(query = query) }
        latestQuery = query
        page = 0
        viewModelScope.launch { runSearch() }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore) return
        val p = s.phase as? SearchPhase.Done ?: return
        if (!p.hasMore) return
        if (s.query.isBlank()) return
        _state.update { it.copy(loadingMore = true) }
        val offset = p.items.size
        viewModelScope.launch {
            try {
                val result = searchProjects.execute(s.query.trim(), offset, PAGE_SIZE)
                _state.update { st ->
                    val cur = st.phase as? SearchPhase.Done ?: st.phase
                    st.copy(
                        loadingMore = false,
                        phase = SearchPhase.Done(
                            items = (cur as? SearchPhase.Done)?.items.orEmpty() + result.hits,
                            total = result.total,
                            hasMore = result.hasMore()
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loadingMore = false) }
            }
        }
    }

    private suspend fun runSearch() {
        val q = _state.value.query.trim()
        if (q.isBlank()) return
        _state.update { it.copy(phase = SearchPhase.Loading) }
        try {
            val result = searchProjects.execute(q, 0, PAGE_SIZE)
            total = result.total
            _state.update {
                it.copy(phase = SearchPhase.Done(result.hits, result.total, result.hasMore()))
            }
        } catch (e: Exception) {
            _state.update { it.copy(phase = SearchPhase.Error(e.message ?: "搜索失败")) }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
