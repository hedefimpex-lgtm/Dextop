package moe.n4tsu.dextop.plasma

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.SystemClock

internal enum class TileZone { NONE, MAXIMIZE, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/** Where a window dropped against a work-area edge snaps to, Windows style. */
internal object EdgeSnap {
    /**
     * Touching the left or right edge (within [edge] px) snaps to that half,
     * touching the top edge maximizes, anything else stays floating.  A
     * window as wide as the work area touches both sides and never snaps
     * sideways.
     */
    fun zone(left: Int, top: Int, right: Int, areaLeft: Int, areaTop: Int, areaRight: Int, edge: Int): TileZone? {
        if (right - left >= areaRight - areaLeft) return null
        val nearLeft = left <= areaLeft + edge
        val nearRight = right >= areaRight - edge
        return when {
            nearLeft && !nearRight -> TileZone.LEFT
            nearRight && !nearLeft -> TileZone.RIGHT
            top <= areaTop -> TileZone.MAXIMIZE
            else -> null
        }
    }
}

/** Shell-side state of one platform freeform window. */
internal class ManagedWindow(var task: TaskSnapshot) {
    val taskId: Int get() = task.taskId
    /** Task bounds as last reported by, or requested from, the platform. */
    val frame = Rect()
    var restoreFrame: Rect? = null
    var maximized = false
    var tile = TileZone.NONE
    var minimized = false
    var desktop = 0
    var onAllDesktops = false
    var label: String = ""
    var icon: Drawable? = null
    var lastActivated = 0L
    var thumbnail: Bitmap? = null
    val openedAt = SystemClock.uptimeMillis()

    val packageName: String get() = task.packageName
    val title: String get() = task.label?.takeIf { it.isNotBlank() } ?: label
}
