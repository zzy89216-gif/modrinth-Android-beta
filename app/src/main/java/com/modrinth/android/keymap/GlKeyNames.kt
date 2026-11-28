// Ported from ZalithLauncher2 (GPL-3.0), see NOTICE

package com.modrinth.android.keymap

/**
 * GLFW 键码 -> 显示名称（移植自 ZalithLauncher2 的 ControlEventKeyName.getNameByKey，
 * 键码常量取自 GLFW 3.4，zlib 许可）。
 */
object GlKeyNames {

    private val names: Map<Int, String> = buildMap {
        put(GlKeycodes.KEY_SPACE, "Space")
        put(GlKeycodes.KEY_APOSTROPHE, "'")
        put(GlKeycodes.KEY_COMMA, ",")
        put(GlKeycodes.KEY_MINUS, "-")
        put(GlKeycodes.KEY_PERIOD, ".")
        put(GlKeycodes.KEY_SLASH, "/")
        for (i in 0..9) put(GlKeycodes.KEY_0 + i, i.toString())
        put(GlKeycodes.KEY_SEMICOLON, ";")
        put(GlKeycodes.KEY_EQUAL, "+")
        for (i in 0..25) put(GlKeycodes.KEY_A + i, Char('A' + i).toString())
        put(GlKeycodes.KEY_LEFT_BRACKET, "[")
        put(GlKeycodes.KEY_BACKSLASH, "\\")
        put(GlKeycodes.KEY_RIGHT_BRACKET, "]")
        put(GlKeycodes.KEY_GRAVE_ACCENT, "`")
        put(GlKeycodes.KEY_ESCAPE, "Esc")
        put(GlKeycodes.KEY_ENTER, "Enter")
        put(GlKeycodes.KEY_TAB, "Tab")
        put(GlKeycodes.KEY_BACKSPACE, "Backspace")
        put(GlKeycodes.KEY_INSERT, "Insert")
        put(GlKeycodes.KEY_DELETE, "Delete")
        put(GlKeycodes.KEY_RIGHT, "→")
        put(GlKeycodes.KEY_LEFT, "←")
        put(GlKeycodes.KEY_DOWN, "↓")
        put(GlKeycodes.KEY_UP, "↑")
        put(GlKeycodes.KEY_PAGE_UP, "Page Up")
        put(GlKeycodes.KEY_PAGE_DOWN, "Page Down")
        put(GlKeycodes.KEY_HOME, "Home")
        put(GlKeycodes.KEY_END, "End")
        put(GlKeycodes.KEY_CAPS_LOCK, "Caps Lock")
        put(GlKeycodes.KEY_SCROLL_LOCK, "Scroll Lock")
        put(GlKeycodes.KEY_NUM_LOCK, "Num Lock")
        put(GlKeycodes.KEY_PRINT_SCREEN, "Print Screen")
        put(GlKeycodes.KEY_PAUSE, "Pause")
        for (i in 1..25) put(GlKeycodes.KEY_F1 + (i - 1), "F$i")
        for (i in 0..9) put(GlKeycodes.KEY_KP_0 + i, "Num $i")
        put(GlKeycodes.KEY_KP_DECIMAL, "Num .")
        put(GlKeycodes.KEY_KP_DIVIDE, "Num /")
        put(GlKeycodes.KEY_KP_MULTIPLY, "Num *")
        put(GlKeycodes.KEY_KP_SUBTRACT, "Num -")
        put(GlKeycodes.KEY_KP_ADD, "Num +")
        put(GlKeycodes.KEY_KP_ENTER, "Num Enter")
        put(GlKeycodes.KEY_KP_EQUAL, "Num =")
        put(GlKeycodes.KEY_LEFT_SHIFT, "Left Shift")
        put(GlKeycodes.KEY_LEFT_CONTROL, "Left Ctrl")
        put(GlKeycodes.KEY_LEFT_ALT, "Left Alt")
        put(GlKeycodes.KEY_LEFT_SUPER, "Left Win")
        put(GlKeycodes.KEY_RIGHT_SHIFT, "Right Shift")
        put(GlKeycodes.KEY_RIGHT_CONTROL, "Right Ctrl")
        put(GlKeycodes.KEY_RIGHT_ALT, "Right Alt")
        put(GlKeycodes.KEY_RIGHT_SUPER, "Right Win")
        put(GlKeycodes.KEY_MENU, "Menu")
        put(GlKeycodes.MOUSE_BUTTON_LEFT, "鼠标左键")
        put(GlKeycodes.MOUSE_BUTTON_RIGHT, "鼠标右键")
        put(GlKeycodes.MOUSE_BUTTON_MIDDLE, "鼠标中键")
    }

    /** 键码对应的显示名；未知键码返回 null */
    fun nameOf(keyCode: Int): String? = names[keyCode]
}
