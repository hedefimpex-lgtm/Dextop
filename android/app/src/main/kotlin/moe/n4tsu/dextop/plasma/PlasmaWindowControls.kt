package moe.n4tsu.dextop.plasma

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/**
 * Windows-style caption buttons (minimize, maximize/restore, close) drawn
 * over the top-right corner of the active window.
 *
 * The platform caption keeps dragging and resizing; this strip only replaces
 * its buttons so every app gets the same controls regardless of the vendor
 * decoration underneath.
 */
internal class WindowControlsView(
    context: Context,
    private val palette: PlasmaPalette,
    private val onMinimize: () -> Unit,
    private val onMaximize: () -> Unit,
    private val onClose: () -> Unit,
    private val onMenu: (rawX: Float, rawY: Float) -> Unit,
) : View(context) {

    enum class Button { MINIMIZE, MAXIMIZE, CLOSE }

    companion object {
        const val BUTTON_WIDTH_DP = 46f
        const val HEIGHT_DP = 32f
        private const val CLOSE_HOVER = 0xFFC42B1C.toInt()
        private const val CLOSE_PRESSED = 0xFF9E2316.toInt()
        private const val CLOSE_GLYPH = 0xFFFFFFFF.toInt()
    }

    private val density = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density
        strokeCap = Paint.Cap.SQUARE
    }
    private val box = RectF()
    private var hovered: Button? = null
    private var pressed: Button? = null

    /** Shows the restore glyph (two stacked squares) instead of maximize. */
    var maximized = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    fun desiredWidth(): Int = (BUTTON_WIDTH_DP * 3 * density + .5f).toInt()
    fun desiredHeight(): Int = (HEIGHT_DP * density + .5f).toInt()

    private fun buttonAt(x: Float, y: Float): Button? {
        if (y < 0f || y > height || x < 0f || x > width) return null
        val slot = (x / (width / 3f)).toInt().coerceIn(0, 2)
        return Button.entries[slot]
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        val next = if (event.actionMasked == MotionEvent.ACTION_HOVER_EXIT) null else buttonAt(event.x, event.y)
        if (next != hovered) {
            hovered = next
            invalidate()
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)) {
                    onMenu(event.rawX, event.rawY)
                    return true
                }
                pressed = buttonAt(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val over = buttonAt(event.x, event.y)
                if (pressed != null && over != pressed) {
                    pressed = null
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val target = pressed?.takeIf { it == buttonAt(event.x, event.y) }
                pressed = null
                hovered = null
                invalidate()
                when (target) {
                    Button.MINIMIZE -> onMinimize()
                    Button.MAXIMIZE -> onMaximize()
                    Button.CLOSE -> onClose()
                    null -> Unit
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                pressed = null
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        // An opaque strip so the platform's own caption buttons never show through.
        fill.color = palette.headerActive
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        val slot = width / 3f
        Button.entries.forEachIndexed { index, button ->
            val left = index * slot
            val active = button == pressed || button == hovered
            if (active) {
                fill.color = when {
                    button == Button.CLOSE && button == pressed -> CLOSE_PRESSED
                    button == Button.CLOSE -> CLOSE_HOVER
                    button == pressed -> palette.pressed
                    else -> palette.hover
                }
                canvas.drawRect(left, 0f, left + slot, height.toFloat(), fill)
            }
            stroke.color = if (button == Button.CLOSE && active) CLOSE_GLYPH else palette.text
            drawGlyph(canvas, button, left + slot / 2f, height / 2f)
        }
    }

    private fun drawGlyph(canvas: Canvas, button: Button, cx: Float, cy: Float) {
        val half = 5f * density
        when (button) {
            Button.MINIMIZE -> canvas.drawLine(cx - half, cy, cx + half, cy, stroke)
            Button.MAXIMIZE -> if (maximized) {
                val offset = 2f * density
                box.set(cx - half, cy - half + offset, cx + half - offset, cy + half)
                canvas.drawRect(box, stroke)
                // Back square: only the parts not hidden by the front one.
                canvas.drawLine(cx - half + offset, cy - half + offset, cx - half + offset, cy - half, stroke)
                canvas.drawLine(cx - half + offset, cy - half, cx + half, cy - half, stroke)
                canvas.drawLine(cx + half, cy - half, cx + half, cy + half - offset, stroke)
                canvas.drawLine(cx + half, cy + half - offset, cx + half - offset, cy + half - offset, stroke)
            } else {
                box.set(cx - half, cy - half, cx + half, cy + half)
                canvas.drawRect(box, stroke)
            }
            Button.CLOSE -> {
                canvas.drawLine(cx - half, cy - half, cx + half, cy + half, stroke)
                canvas.drawLine(cx + half, cy - half, cx - half, cy + half, stroke)
            }
        }
    }

    /** Bounds of the strip inside [frame], or null when the window is too small for it. */
    fun boundsFor(frame: Rect, displayWidth: Int, displayHeight: Int): Rect? {
        val w = desiredWidth()
        val h = desiredHeight()
        if (frame.width() < w + h || frame.height() < h * 2) return null
        val right = frame.right.coerceAtMost(displayWidth)
        val top = frame.top.coerceAtLeast(0)
        val bounds = Rect(right - w, top, right, top + h)
        if (bounds.left < 0 || bounds.bottom > displayHeight) return null
        return bounds
    }
}
