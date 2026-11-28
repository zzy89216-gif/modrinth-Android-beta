// Ported from ZalithLauncher2 (GPL-3.0), see NOTICE

package com.modrinth.android.keymap

import android.view.KeyEvent

/**
 * Android KeyEvent -> GLFW 键码 / 修饰位的映射。
 *
 * 移植自 ZalithLauncher2 的 EfficientAndroidLWJGLKeycode（其本身修改自 PojavLauncher），
 * 供键盘捕获对话框使用：物理键盘 / 蓝牙手柄的 KeyEvent 由此转换为可持久化的 GLFW 绑定。
 */
object AndroidKeyMapper {

    /** 可绑定的修饰键码（单独按下时只记入修饰位，不产生绑定） */
    val MODIFIER_KEYCODES = setOf(
        KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
        KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
        KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
        KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT,
    )

    private val androidToGlfw: Map<Int, Int> = buildMap {
        put(KeyEvent.KEYCODE_UNKNOWN, GlKeycodes.UNKNOWN)
        // 0-9
        for (i in 0..9) put(KeyEvent.KEYCODE_0 + i, GlKeycodes.KEY_0 + i)
        // A-Z
        for (i in 0..25) put(KeyEvent.KEYCODE_A + i, GlKeycodes.KEY_A + i)
        // 符号
        put(KeyEvent.KEYCODE_GRAVE, GlKeycodes.KEY_GRAVE_ACCENT)
        put(KeyEvent.KEYCODE_MINUS, GlKeycodes.KEY_MINUS)
        put(KeyEvent.KEYCODE_EQUALS, GlKeycodes.KEY_EQUAL)
        put(KeyEvent.KEYCODE_LEFT_BRACKET, GlKeycodes.KEY_LEFT_BRACKET)
        put(KeyEvent.KEYCODE_RIGHT_BRACKET, GlKeycodes.KEY_RIGHT_BRACKET)
        put(KeyEvent.KEYCODE_BACKSLASH, GlKeycodes.KEY_BACKSLASH)
        put(KeyEvent.KEYCODE_SEMICOLON, GlKeycodes.KEY_SEMICOLON)
        put(KeyEvent.KEYCODE_APOSTROPHE, GlKeycodes.KEY_APOSTROPHE)
        put(KeyEvent.KEYCODE_COMMA, GlKeycodes.KEY_COMMA)
        put(KeyEvent.KEYCODE_PERIOD, GlKeycodes.KEY_PERIOD)
        put(KeyEvent.KEYCODE_SLASH, GlKeycodes.KEY_SLASH)
        // 功能键
        put(KeyEvent.KEYCODE_SPACE, GlKeycodes.KEY_SPACE)
        put(KeyEvent.KEYCODE_ENTER, GlKeycodes.KEY_ENTER)
        put(KeyEvent.KEYCODE_TAB, GlKeycodes.KEY_TAB)
        put(KeyEvent.KEYCODE_DEL, GlKeycodes.KEY_BACKSPACE)
        put(KeyEvent.KEYCODE_FORWARD_DEL, GlKeycodes.KEY_DELETE)
        put(KeyEvent.KEYCODE_DPAD_UP, GlKeycodes.KEY_UP)
        put(KeyEvent.KEYCODE_DPAD_DOWN, GlKeycodes.KEY_DOWN)
        put(KeyEvent.KEYCODE_DPAD_LEFT, GlKeycodes.KEY_LEFT)
        put(KeyEvent.KEYCODE_DPAD_RIGHT, GlKeycodes.KEY_RIGHT)
        put(KeyEvent.KEYCODE_PAGE_UP, GlKeycodes.KEY_PAGE_UP)
        put(KeyEvent.KEYCODE_PAGE_DOWN, GlKeycodes.KEY_PAGE_DOWN)
        put(KeyEvent.KEYCODE_MOVE_HOME, GlKeycodes.KEY_HOME)
        put(KeyEvent.KEYCODE_MOVE_END, GlKeycodes.KEY_END)
        put(KeyEvent.KEYCODE_INSERT, GlKeycodes.KEY_INSERT)
        put(KeyEvent.KEYCODE_CAPS_LOCK, GlKeycodes.KEY_CAPS_LOCK)
        put(KeyEvent.KEYCODE_SCROLL_LOCK, GlKeycodes.KEY_SCROLL_LOCK)
        put(KeyEvent.KEYCODE_NUM_LOCK, GlKeycodes.KEY_NUM_LOCK)
        put(KeyEvent.KEYCODE_SYSRQ, GlKeycodes.KEY_PRINT_SCREEN)
        put(KeyEvent.KEYCODE_BREAK, GlKeycodes.KEY_PAUSE)
        put(KeyEvent.KEYCODE_ESCAPE, GlKeycodes.KEY_ESCAPE)
        put(KeyEvent.KEYCODE_BACK, GlKeycodes.KEY_ESCAPE)
        put(KeyEvent.KEYCODE_MENU, GlKeycodes.KEY_MENU)
        // 修饰键（左右区分）
        put(KeyEvent.KEYCODE_SHIFT_LEFT, GlKeycodes.KEY_LEFT_SHIFT)
        put(KeyEvent.KEYCODE_SHIFT_RIGHT, GlKeycodes.KEY_RIGHT_SHIFT)
        put(KeyEvent.KEYCODE_CTRL_LEFT, GlKeycodes.KEY_LEFT_CONTROL)
        put(KeyEvent.KEYCODE_CTRL_RIGHT, GlKeycodes.KEY_RIGHT_CONTROL)
        put(KeyEvent.KEYCODE_ALT_LEFT, GlKeycodes.KEY_LEFT_ALT)
        put(KeyEvent.KEYCODE_ALT_RIGHT, GlKeycodes.KEY_RIGHT_ALT)
        put(KeyEvent.KEYCODE_META_LEFT, GlKeycodes.KEY_LEFT_SUPER)
        put(KeyEvent.KEYCODE_META_RIGHT, GlKeycodes.KEY_RIGHT_SUPER)
        // F1-F12
        for (i in 0..11) put(KeyEvent.KEYCODE_F1 + i, GlKeycodes.KEY_F1 + i)
        // 小键盘
        for (i in 0..9) put(KeyEvent.KEYCODE_NUMPAD_0 + i, GlKeycodes.KEY_KP_0 + i)
        put(KeyEvent.KEYCODE_NUMPAD_ADD, GlKeycodes.KEY_KP_ADD)
        put(KeyEvent.KEYCODE_NUMPAD_SUBTRACT, GlKeycodes.KEY_KP_SUBTRACT)
        put(KeyEvent.KEYCODE_NUMPAD_MULTIPLY, GlKeycodes.KEY_KP_MULTIPLY)
        put(KeyEvent.KEYCODE_NUMPAD_DIVIDE, GlKeycodes.KEY_KP_DIVIDE)
        put(KeyEvent.KEYCODE_NUMPAD_DOT, GlKeycodes.KEY_KP_DECIMAL)
        put(KeyEvent.KEYCODE_NUMPAD_COMMA, GlKeycodes.KEY_COMMA)
        put(KeyEvent.KEYCODE_NUMPAD_ENTER, GlKeycodes.KEY_KP_ENTER)
        put(KeyEvent.KEYCODE_NUMPAD_EQUALS, GlKeycodes.KEY_KP_EQUAL)
        put(KeyEvent.KEYCODE_PLUS, GlKeycodes.KEY_KP_ADD)
    }

    /**
     * 把 Android 键码映射为 GLFW 键码；无对应键返回 null。
     * 字母数字键不区分大小写，Shift 状态体现在 [modsOf] 中。
     */
    fun toGlfwKeycode(androidKeyCode: Int): Int? = androidToGlfw[androidKeyCode]

    /** 由 KeyEvent 构建 GLFW_MOD_* 修饰位掩码 */
    fun modsOf(event: KeyEvent): Int {
        var mods = 0
        if (event.isShiftPressed) mods = mods or GlKeycodes.MOD_SHIFT
        if (event.isCtrlPressed) mods = mods or GlKeycodes.MOD_CONTROL
        if (event.isAltPressed) mods = mods or GlKeycodes.MOD_ALT
        if (event.isMetaPressed) mods = mods or GlKeycodes.MOD_SUPER
        return mods
    }

    /** 该键是否应视为组合键（主键 + 修饰键同时按下） */
    fun isChord(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        return keyCode in MODIFIER_KEYCODES ||
            (event.isShiftPressed || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed)
    }
}
