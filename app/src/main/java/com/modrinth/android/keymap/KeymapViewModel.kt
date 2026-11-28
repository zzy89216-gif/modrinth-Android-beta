/*
 * Part of ModBox. New code that consumes the ZalithLauncher2-ported keymap
 * modules (see NOTICE). Project files are under the repository LICENSE.
 */



package com.modrinth.android.keymap

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 键位设置屏 ViewModel：订阅仓库的 [KeymapRepository.observe] 流，
 * 改绑 / 重置后即时持久化（每次变更自动推回 UI）。
 *
 * 依赖通过 Hilt 注入（@ApplicationContext 取 Context 建仓库），与项目里
 * SearchViewModel 的写法保持一致；最小解耦：keymap 包不依赖 app 的任何其他 ViewModel。
 */
@HiltViewModel
class KeymapViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {

    private val repository = KeymapRepository(context)

    private val _config = MutableStateFlow(KeymapConfig(emptyList()))
    val config: StateFlow<KeymapConfig> = _config.asStateFlow()

    /** 是否完成过初始加载 */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** 最近一次变更是否已写盘 */
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        // observe() 冷流：首个值来自磁盘（或默认键位），其后每次 save/reset 推送
        viewModelScope.launch {
            repository.observe().collect { value ->
                _config.value = value
                if (!_loaded.value) _loaded.value = true
            }
        }
    }

    /** 更新单个动作的绑定并立即持久化 */
    fun updateBinding(binding: KeyBinding) {
        val current = _config.value
        if (current.bindings.isEmpty()) return
        val next = current.copy(
            bindings = current.bindings.map { if (it.actionId == binding.actionId) binding else it }
        )
        _config.value = next
        persist(next)
    }

    /** 重置为 Minecraft 默认键位并持久化 */
    fun reset() {
        viewModelScope.launch {
            repository.reset()
            _saved.value = true
        }
    }

    private fun persist(config: KeymapConfig) {
        viewModelScope.launch {
            repository.save(config)
            _saved.value = true
        }
    }
}
