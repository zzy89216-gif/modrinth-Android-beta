// Ported from ZalithLauncher2 (GPL-3.0), see NOTICE

package com.modrinth.android.keymap

/**
 * 单个键位绑定：动作 id、GLFW 键码（Int 表示，-1 表示未绑定）、修饰键位掩码（GLFW_MOD_*）。
 *
 * 组合键 = keyCode + modifiers，例如 Ctrl+S 即 keyCode=KEY_S, modifiers=MOD_CONTROL。
 * 长按产生的动作以 modifierBit 表达（见 [KeymapScreen] 的捕获对话框）。
 */
/** 长按动作修饰位（keymap 自有语义，非 GLFW 位：该键按住 = 持续动作） */
const val MOD_LONG_PRESS = 1 shl 6

data class KeyBinding(
    /** 动作 id，如 "move_forward"、"chat" */
    val actionId: String,
    /** 显示名，如「前进」 */
    val label: String,
    /** GLFW 键码，-1 表示未绑定 */
    val keyCode: Int = -1,
    /** 修饰键位掩码（GLFW_MOD_*） */
    val modifiers: Int = 0,
) {
    /** 是否已绑定有效按键 */
    val isBound: Boolean get() = keyCode != -1

    /** 人类可读的按键描述，如 "Ctrl + S" / "S" */
    fun displayKey(): String {
        if (!isBound) return "未绑定"
        val parts = buildList {
            if (modifiers and GlKeycodes.MOD_CONTROL != 0) add("Ctrl")
            if (modifiers and GlKeycodes.MOD_SHIFT != 0) add("Shift")
            if (modifiers and GlKeycodes.MOD_ALT != 0) add("Alt")
            if (modifiers and GlKeycodes.MOD_SUPER != 0) add("Win")
            add(GlKeyNames.nameOf(keyCode) ?: "键$keyCode")
        }
        return parts.joinToString(" + ") + if (modifiers and MOD_LONG_PRESS != 0) "（长按）" else ""
    }
}

/**
 * 键位映射配置（持久化模型）。
 *
 * 移植自 ZalithLauncher2 的 InputMap / _MinecraftKeyBindingMapper：
 * 按键列表（[KeyBinding]）、MC 默认绑定值、整体持久化为 JSON。
 */
data class KeymapConfig(
    val bindings: List<KeyBinding>,
    /** 保存时的屏幕宽度（dp），跨机型迁移时按比例调整网格等相对布局用；ModBox 暂仅存档 */
    val savedWidthDp: Float = 0f,
    /** 配置版本号，用于将来迁移 */
    val version: Int = 1,
) {
    fun bindingOf(actionId: String): KeyBinding? = bindings.firstOrNull { it.actionId == actionId }
}

/**
 * 默认键位（移植自 ZalithLauncher2 的 _MinecraftKeyBindingMapper.kt 中 Minecraft 的默认绑定）。
 */
object DefaultKeymap {
    fun defaults(): List<KeyBinding> = listOf(
        // WASD 移动
        KeyBinding("move_forward", "前进", GlKeycodes.KEY_W),
        KeyBinding("move_left", "左移", GlKeycodes.KEY_A),
        KeyBinding("move_back", "后退", GlKeycodes.KEY_S),
        KeyBinding("move_right", "右移", GlKeycodes.KEY_D),
        // 功能
        KeyBinding("sprint", "疾跑", GlKeycodes.KEY_LEFT_CONTROL),
        KeyBinding("jump", "跳跃", GlKeycodes.KEY_SPACE),
        KeyBinding("open_chat", "聊天", GlKeycodes.KEY_T),
        KeyBinding("drop", "丢弃", GlKeycodes.KEY_Q),
        KeyBinding("swap_offhand", "副手交换", GlKeycodes.KEY_F),
        // 快捷栏 1-9
        KeyBinding("hotbar_1", "快捷栏 1", GlKeycodes.KEY_1),
        KeyBinding("hotbar_2", "快捷栏 2", GlKeycodes.KEY_2),
        KeyBinding("hotbar_3", "快捷栏 3", GlKeycodes.KEY_3),
        KeyBinding("hotbar_4", "快捷栏 4", GlKeycodes.KEY_4),
        KeyBinding("hotbar_5", "快捷栏 5", GlKeycodes.KEY_5),
        KeyBinding("hotbar_6", "快捷栏 6", GlKeycodes.KEY_6),
        KeyBinding("hotbar_7", "快捷栏 7", GlKeycodes.KEY_7),
        KeyBinding("hotbar_8", "快捷栏 8", GlKeycodes.KEY_8),
        KeyBinding("hotbar_9", "快捷栏 9", GlKeycodes.KEY_9),
    )

    /** 默认配置 */
    fun defaultConfig() = KeymapConfig(bindings = defaults())
}
