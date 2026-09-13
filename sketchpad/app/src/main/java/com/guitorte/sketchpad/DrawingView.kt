package com.guitorte.sketchpad

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * A finger-painting surface.
 *
 * Finished strokes are baked into an offscreen bitmap so that redrawing costs the
 * same no matter how much has been drawn; the stroke list is kept only so undo and
 * redo can rebuild that bitmap.
 */
class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private class Stroke(val path: Path, val color: Int, val width: Float, val eraser: Boolean)

    private val strokes = mutableListOf<Stroke>()
    private val undone = mutableListOf<Stroke>()

    private var baked: Bitmap? = null
    private var bakedCanvas: Canvas? = null

    private var activePath: Path? = null
    private var lastX = 0f
    private var lastY = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val eraseMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    /** Colour used for new strokes and for the exported image background's contrast. */
    var strokeColor: Int = Color.BLACK

    /** Stroke width in pixels. */
    var strokeWidth: Float = 12f

    /** When true, new strokes rub out what is underneath instead of adding ink. */
    var eraserEnabled: Boolean = false

    /** Colour the canvas is cleared to, and the background of exported images. */
    var canvasColor: Int = Color.WHITE

    /** Invoked whenever undo/redo/clear availability may have changed. */
    var onHistoryChanged: (() -> Unit)? = null

    val canUndo: Boolean get() = strokes.isNotEmpty()
    val canRedo: Boolean get() = undone.isNotEmpty()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        baked?.recycle()
        baked = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bakedCanvas = Canvas(baked!!)
        rebake()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(canvasColor)

        // The in-progress stroke shares a layer with the baked strokes so that an
        // eraser in flight rubs out ink without punching a hole in the background.
        val layer = canvas.saveLayer(null, null)
        baked?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        activePath?.let { drawStroke(canvas, it, strokeColor, strokeWidth, eraserEnabled) }
        canvas.restoreToCount(layer)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                activePath = Path().apply {
                    moveTo(x, y)
                    // A tap with no movement should still leave a dot.
                    lineTo(x, y)
                }
                lastX = x
                lastY = y
            }

            MotionEvent.ACTION_MOVE -> {
                val path = activePath ?: return true
                // Quadratic segments through the midpoints smooth out the jitter
                // that raw touch samples would otherwise show as hard corners.
                val midX = (lastX + x) / 2f
                val midY = (lastY + y) / 2f
                path.quadTo(lastX, lastY, midX, midY)
                lastX = x
                lastY = y
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val path = activePath ?: return true
                path.lineTo(x, y)
                commit(Stroke(path, strokeColor, strokeWidth, eraserEnabled))
                activePath = null
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
            }

            else -> return super.onTouchEvent(event)
        }

        invalidate()
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    fun undo() {
        if (strokes.isEmpty()) return
        undone.add(strokes.removeAt(strokes.lastIndex))
        rebake()
        invalidate()
        onHistoryChanged?.invoke()
    }

    fun redo() {
        if (undone.isEmpty()) return
        val stroke = undone.removeAt(undone.lastIndex)
        strokes.add(stroke)
        bakedCanvas?.let { drawStroke(it, stroke.path, stroke.color, stroke.width, stroke.eraser) }
        invalidate()
        onHistoryChanged?.invoke()
    }

    fun clear() {
        if (strokes.isEmpty() && undone.isEmpty()) return
        strokes.clear()
        undone.clear()
        rebake()
        invalidate()
        onHistoryChanged?.invoke()
    }

    /** Snapshot of the drawing, flattened onto [canvasColor], ready to export. */
    fun exportBitmap(): Bitmap? {
        val source = baked ?: return null
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(canvasColor)
            drawBitmap(source, 0f, 0f, null)
        }
        return out
    }

    private fun commit(stroke: Stroke) {
        strokes.add(stroke)
        // A new stroke forks the history, so anything that was undone is gone.
        undone.clear()
        bakedCanvas?.let { drawStroke(it, stroke.path, stroke.color, stroke.width, stroke.eraser) }
        onHistoryChanged?.invoke()
    }

    private fun rebake() {
        val canvas = bakedCanvas ?: return
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        strokes.forEach { drawStroke(canvas, it.path, it.color, it.width, it.eraser) }
    }

    private fun drawStroke(canvas: Canvas, path: Path, color: Int, width: Float, eraser: Boolean) {
        paint.color = color
        paint.strokeWidth = width
        paint.xfermode = if (eraser) eraseMode else null
        canvas.drawPath(path, paint)
        paint.xfermode = null
    }
}
