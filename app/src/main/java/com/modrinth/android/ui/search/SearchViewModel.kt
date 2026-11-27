package com.modrinth.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.modrinth.domain.model.Project
import com.modrinth.domain.usecase.SearchProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchProjectsUseCase: SearchProjectsUseCase
) : ViewModel() {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun search(query: String) {
        if (query.isBlank()) {
            _projects.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _projects.value = searchProjectsUseCase.execute(query.trim())
            } catch (e: Exception) {
                _error.value = e.message ?: "搜索失败"
                _projects.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
