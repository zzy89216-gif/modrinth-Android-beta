// Ported from ZalithLauncher2 (GPL-3.0), see NOTICE

package com.modrinth.android.keymap

import androidx.compose.foundation.focusProperties
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 键位映射设置屏（移植自 ZalithLauncher2 的 InputMap + KeyEventEdit/Keyboard 能力）。
 *
 * 功能：
 * - 列表展示当前所有动作绑定；
 * - 点击进入重新绑定对话框，用物理键盘（onKeyEvent 捕获，支持 Ctrl/Shift/Alt/Win 组合键）
 *   或对话框内虚拟按键重新绑定；
 * - 长按模式：为该键标记「长按 = 持续动作」（如 MC 的长按前进持续移动）；
 * - 修改即时持久化（见 [KeymapRepository]），可一键重置为 Minecraft 默认键位。
 */
@Composable
fun KeymapScreen(
    viewModel: KeymapViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()

    var bindingTarget by remember { mutableStateOf<KeyBinding?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("键位映射") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
                actions = {
                    TextButton(onClick = { viewModel.reset() }) { Text("恢复默认") }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        }
    ) { padding ->
        if (!loaded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = "点击一行重新绑定该动作；支持物理键盘组合键（如 Ctrl + S）与长按动作。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(config.bindings, key = { it.actionId }) { binding ->
                ListItem(
                    headlineContent = { Text(binding.label) },
                    trailingContent = {
                        Text(
                            text = binding.displayKey(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (binding.isBound) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    },
                    onClick = { bindingTarget = binding },
                )
            }
        }

        bindingTarget?.let { target ->
            KeymapBindDialog(
                binding = target,
                onDismiss = { bindingTarget = null },
                onApply = { updated ->
                    viewModel.updateBinding(updated)
                    bindingTarget = null
                },
            )
        }
    }
}

/**
 * 重新绑定对话框：捕获物理键盘（含组合键）或选择虚拟按键，并支持长按动作标记。
 *
 * 捕获方式：对话框内容持有焦点，[androidx.compose.ui.input.key.onKeyEvent] 收到
 * 物理键盘事件后经 [AndroidKeyMapper] 转成 GLFW 键码；修饰键（Shift/Ctrl/Alt/Win）
 * 单独按下只更新组合位，组合键 = 修饰键 + 主键。
 */
@Composable
private fun KeymapBindDialog(
    binding: KeyBinding,
    onDismiss: () -> Unit,
    onApply: (KeyBinding) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // 组合修饰位（芯片选择；物理真实组合键优先于芯片）
    var chipMods by remember { mutableIntStateOf(binding.modifiers and MOD_LONG_PRESS.inv()) }
    // 长按动作开关
    var longPress by remember { mutableStateOf(binding.modifiers and MOD_LONG_PRESS != 0) }
    // 已捕获、待确认的键（再按同一键确认）
    var pending by remember { mutableStateOf<CapturedKey?>(null) }
    // 捕获提示
    var hint by remember { mutableStateOf("等待按键…（物理键盘，或选下方虚拟键）") }

    DisposableEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.hide()
        onDispose { focusManager.clearFocus() }
    }

    /** 把「主键 + 修饰位」拼成人类可读文本，如 "Ctrl + S" */
    fun keyDisplay(keyCode: Int, mods: Int): String {
        if (keyCode < 0) return "未绑定"
        val parts = buildList {
            if (mods and GlKeycodes.MOD_CONTROL != 0) add("Ctrl")
            if (mods and GlKeycodes.MOD_SHIFT != 0) add("Shift")
            if (mods and GlKeycodes.MOD_ALT != 0) add("Alt")
            if (mods and GlKeycodes.MOD_SUPER != 0) add("Win")
            add(GlKeyNames.nameOf(keyCode) ?: "键$keyCode")
        }
        return parts.joinToString(" + ")
    }

    fun pendingDisplay(): String {
        val p = pending ?: return "未绑定"
        return keyDisplay(p.keyCode, p.modifiers)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("重新绑定「${binding.label}」")
        },
        text = {
            // 焦点捕获区：整个对话框根节点拦截物理键盘事件，
            // 子节点（芯片/虚拟键）未处理的事件会向上冒泡到这里
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusProperties(canFocus = true)
                    .onKeyEvent { event ->
                        // 只处理非重复的下键（长按/连击不重复捕获）
                        if (event.type != KeyEventType.Down || event.repeatCount != 0) {
                            return@onKeyEvent false
                        }
                        val ak = event.keyCode
                        val glfw = AndroidKeyMapper.toGlfwKeycode(ak)
                        when {
                            glfw == null -> false
                            ak in AndroidKeyMapper.MODIFIER_KEYCODES -> {
                                // 修饰键单独按下：只提示，不产生绑定
                                hint = "再按一个主键…"
                                true
                            }
                            else -> {
                                val realMods = AndroidKeyMapper.modsOf(event)
                                // 真实组合键（物理 Ctrl/Shift 同时按下）优先于芯片选择的修饰
                                val effectiveMods = if (realMods != 0) realMods else chipMods
                                val p = pending
                                if (p != null && p.keyCode == glfw && p.modifiers == effectiveMods) {
                                    // 第二次按同一组合键：确认并应用
                                    pending = null
                                    hint = "已确认 ${keyDisplay(glfw, effectiveMods)}"
                                    onApply(
                                        binding.copy(
                                            keyCode = glfw,
                                            modifiers = effectiveMods or (if (longPress) MOD_LONG_PRESS else 0),
                                        )
                                    )
                                } else {
                                    // 第一次按：进入待确认状态
                                    pending = CapturedKey(glfw, effectiveMods)
                                    hint = "已捕获 ${keyDisplay(glfw, effectiveMods)}，再按一次同一键确认"
                                }
                                true
                            }
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                ) {
                    Text(
                        text = hint,
                        modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 当前捕获结果（待确认的键 + 长按标记）
                Text(
                    text = "当前：${pendingDisplay()}" +
                        if (longPress) "（长按）" else "",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                // 修饰键芯片
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModifierChip(
                        label = "Shift",
                        selected = chipMods and GlKeycodes.MOD_SHIFT != 0,
                        onToggle = { on ->
                            chipMods = if (on) chipMods or GlKeycodes.MOD_SHIFT
                            else chipMods and GlKeycodes.MOD_SHIFT.inv()
                        },
                    )
                    ModifierChip(
                        label = "Ctrl",
                        selected = chipMods and GlKeycodes.MOD_CONTROL != 0,
                        onToggle = { on ->
                            chipMods = if (on) chipMods or GlKeycodes.MOD_CONTROL
                            else chipMods and GlKeycodes.MOD_CONTROL.inv()
                        },
                    )
                    ModifierChip(
                        label = "Alt",
                        selected = chipMods and GlKeycodes.MOD_ALT != 0,
                        onToggle = { on ->
                            chipMods = if (on) chipMods or GlKeycodes.MOD_ALT
                            else chipMods and GlKeycodes.MOD_ALT.inv()
                        },
                    )
                    ModifierChip(
                        label = "Win",
                        selected = chipMods and GlKeycodes.MOD_SUPER != 0,
                        onToggle = { on ->
                            chipMods = if (on) chipMods or GlKeycodes.MOD_SUPER
                            else chipMods and GlKeycodes.MOD_SUPER.inv()
                        },
                    )
                }

                // 长按动作开关
                FilterChip(
                    selected = longPress,
                    onClick = { longPress = !longPress },
                    label = { Text("长按 = 持续动作") },
                )

                HorizontalDivider()

                // 虚拟按键（无键盘设备也可用）：点一下即设为待确认键，再点「应用」
                Text(
                    text = "或选择虚拟按键（组合键需先点上方修饰）：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VirtualKeys(
                        selected = pending?.keyCode ?: -1,
                        onPick = { k ->
                            pending = CapturedKey(k, chipMods)
                            hint = "已选 ${GlKeyNames.nameOf(k) ?: "键$k"}，点「应用」生效"
                        },
                    )
                }
            }
        },
        confirmButton = {
            // 物理键盘路径：再次按同一键已置 pending=null 并提示「已确认」；
            // 此处按钮供「虚拟按键 / 芯片」路径或漏按确认时兜底
            Button(
                onClick = {
                    val p = pending
                    if (p != null) {
                        onApply(
                            binding.copy(
                                keyCode = p.keyCode,
                                modifiers = p.modifiers or (if (longPress) MOD_LONG_PRESS else 0),
                            )
                        )
                    }
                },
                enabled = pending != null,
            ) {
                Text("应用")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onApply(binding.copy(keyCode = -1, modifiers = 0)) },
                    enabled = binding.isBound,
                ) {
                    Text("清除")
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        },
    )
}

@Composable
private fun ModifierChip(
    label: String,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = { onToggle(!selected) },
        label = { Text(label) },
    )
}

/** 待确认的键：GLFW 主键 + 修饰位（不含 MOD_LONG_PRESS） */
private data class CapturedKey(val keyCode: Int, val modifiers: Int)

/** 虚拟按键行（MC 常用键 + 数字 1-9） */
@Composable
private fun VirtualKeys(
    selected: Int,
    onPick: (Int) -> Unit,
) {
    val keys = buildList {
        add("W" to GlKeycodes.KEY_W)
        add("A" to GlKeycodes.KEY_A)
        add("S" to GlKeycodes.KEY_S)
        add("D" to GlKeycodes.KEY_D)
        add("Space" to GlKeycodes.KEY_SPACE)
        add("T" to GlKeycodes.KEY_T)
        add("Q" to GlKeycodes.KEY_Q)
        add("F" to GlKeycodes.KEY_F)
        add("E" to GlKeycodes.KEY_E)
        for (i in 1..9) add(i.toString() to GlKeycodes.KEY_1 + (i - 1))
    }
    keys.forEach { (label, keyCode) ->
        Surface(
            onClick = { onPick(keyCode) },
            shape = MaterialTheme.shapes.small,
            color = if (selected == keyCode) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
