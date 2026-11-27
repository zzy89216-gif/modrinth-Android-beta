package com.modrinth.android.ui.project

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
class ProjectDetailViewModel @Inject constructor(
    private val getProjectUseCase: GetProjectUseCase
) : ViewModel() {

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _project.value = getProjectUseCase.execute(projectId)
            _isLoading.value = false
        }
    }
}
