package com.modrinth.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.modrinth.domain.model.Project
import com.modrinth.domain.usecase.GetProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getProjectUseCase: GetProjectUseCase
) : ViewModel() {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun search(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // TODO: 实现搜索逻辑
            _isLoading.value = false
        }
    }
}
