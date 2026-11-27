package com.modrinth.android.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.modrinth.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            _categories.value = repository.getCategories()
            _isLoading.value = false
        }
    }
}
