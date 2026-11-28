// Ported from ZalithLauncher2 (GPL-3.0), see NOTICE

package com.modrinth.android.ui.components.cardgrid.model

import androidx.compose.ui.unit.IntOffset

/**
 * 网格卡片的布局矩形。
 * 坐标与尺寸均以网格单元格为单位，锚点为卡片左上角，
 * y 轴向下为正，纵向（行数）不设上限。
 */
data class CardRect(
    val id: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    val right: Int get() = x + width
    val bottom: Int get() = y + height

    fun positionAt(position: IntOffset): CardRect = copy(x = position.x, y = position.y)

    fun intersects(other: CardRect): Boolean =
        x < other.right && other.x < right && y < other.bottom && other.y < bottom
}
