package com.modrinth.android.ui.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.modrinth.android.service.DownloadManager
import com.modrinth.domain.model.Project
import com.modrinth.domain.model.SearchProject
import com.modrinth.domain.model.Version
import com.modrinth.domain.usecase.GetProjectUseCase
import com.modrinth.domain.usecase.GetProjectVersionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val loading: Boolean = true,
    val loadingVersions: Boolean = true,
    val error: String? = null,
    val project: Project? = null,
    val versions: List<Version> = emptyList()
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProject: GetProjectUseCase,
    private val getProjectVersions: GetProjectVersionsUseCase,
    val downloadManager: DownloadManager,
    /** Coil 2.7.0 无自动单例，由 CoilModule 提供 */
    val imageLoader: coil.ImageLoader
) : ViewModel() {

    private val projectId: String =
        savedStateHandle.get<String>("projectId").orEmpty()

    private val _state = MutableStateFlow(ProjectDetailUiState())
    val state: StateFlow<ProjectDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val project = getProject.execute(projectId)
                if (project == null) {
                    _state.update {
                        it.copy(loading = false, error = "项目不存在或已被删除")
                    }
                    return@launch
                }
                _state.update { it.copy(loading = false, project = project) }
                loadVersions()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "加载失败") }
            }
        }
    }

    private fun loadVersions() {
        _state.update { it.copy(loadingVersions = true) }
        viewModelScope.launch {
            try {
                val versions = getProjectVersions.execute(projectId)
                _state.update { it.copy(loadingVersions = false, versions = versions) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loadingVersions = false, error = "版本列表加载失败：${e.message}")
                }
            }
        }
    }

    /** 下载某个版本的主文件 */
    fun downloadVersion(version: Version) {
        val file = version.primaryFile ?: return
        val url = file.url ?: return
        val title = _state.value.project?.title ?: "Modrinth 模组"
        downloadManager.enqueue(
            projectTitle = title,
            fileName = file.filename ?: "mod-${version.versionNumber ?: "latest"}.jar",
            url = url,
            size = file.size,
            sha512Expected = file.sha512
        )
    }
}
