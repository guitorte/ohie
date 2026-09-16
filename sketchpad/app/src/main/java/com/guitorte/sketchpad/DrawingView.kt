package com.guitorte.sketchpad

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * An unbounded canvas holding an optional photo, with ink drawn on top of it.
 *
 * Everything — the photo, every stroke — lives in *world* coordinates, and a single
 * matrix maps that world onto the screen. Panning and zooming therefore only touch
 * the matrix, and ink stays welded to the spot on the photo where it was drawn, at
 * any zoom level. When a photo is loaded, one world unit is one photo pixel, which
 * also makes exporting at the photo's native resolution a straight copy.
 */
class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** What a one-finger gesture does. */
    enum class Mode { DRAW, NAVIGATE }

    private class Stroke(val path: Path, val color: Int, val width: Float, val eraser: Boolean)

    private val strokes = mutableListOf<Stroke>()
    private val undone = mutableListOf<Stroke>()

    private var photo: Bitmap? = null

    /** World space to screen space. Scale and translation only, never rotation. */
    private val viewMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private val mappedPoint = FloatArray(2)
    private val matrixValues = FloatArray(9)
    private val scratchRect = RectF()

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val photoPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val photoEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(60, 0, 0, 0)
    }
    private val gridPaint = Paint().apply { color = Color.argb(22, 0, 0, 0) }
    private val eraseMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    private var activePath: Path? = null
    private var activeWidth = 0f
    private var lastWorldX = 0f
    private var lastWorldY = 0f
    private var drawing = false

    private var panning = false
    private var lastFocusX = 0f
    private var lastFocusY = 0f

    /** Whether a one-finger drag draws or pans. Two fingers always pan and zoom. */
    var mode: Mode = Mode.DRAW
        set(value) {
            if (field == value) return
            // Switching away mid-stroke would leave a half-finished path behind.
            cancelStroke()
            field = value
            invalidate()
        }

    var strokeColor: Int = Color.BLACK

    /** Brush width in screen pixels; converted to world units when a stroke starts. */
    var brushWidth: Float = 12f

    var eraserEnabled: Boolean = false

    /** The endless backdrop the photo floats on. */
    var surfaceColor: Int = ContextCompat.getColor(context, R.color.canvas_surface)

    /** Background of exported images, seen only where the photo does not reach. */
    var paperColor: Int = ContextCompat.getColor(context, R.color.canvas_paper)

    var onHistoryChanged: (() -> Unit)? = null

    val canUndo: Boolean get() = strokes.isNotEmpty()
    val canRedo: Boolean get() = undone.isNotEmpty()
    val hasPhoto: Boolean get() = photo != null

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val current = currentScale()
                // Clamp against the resulting scale so a pinch stops at the limit
                // instead of being ignored outright.
                val factor = (current * detector.scaleFactor)
                    .coerceIn(MIN_SCALE, MAX_SCALE) / current
                viewMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
                invalidate()
                return true
            }
        }
    )

    private val tapDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                fitContent()
                return true
            }
        }
    ).apply { setIsLongpressEnabled(false) }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // The first time we learn our size, frame whatever is already loaded.
        if (oldw == 0 && oldh == 0 && w > 0 && h > 0) fitContent()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(surfaceColor)

        val scale = currentScale()

        canvas.save()
        canvas.concat(viewMatrix)
        drawGrid(canvas, scale)
        photo?.let {
            canvas.drawBitmap(it, 0f, 0f, photoPaint)
            photoEdgePaint.strokeWidth = 1f / scale
            canvas.drawRect(0f, 0f, it.width.toFloat(), it.height.toFloat(), photoEdgePaint)
        }
        canvas.restore()

        // Ink gets its own layer so that an eraser stroke rubs out ink only, leaving
        // the photo and the backdrop underneath untouched.
        val needsLayer = strokes.any { it.eraser } || (drawing && eraserEnabled)
        val layer = if (needsLayer) canvas.saveLayer(null, null) else canvas.save()
        canvas.concat(viewMatrix)
        strokes.forEach { drawStroke(canvas, it.path, it.color, it.width, it.eraser) }
        activePath?.let { drawStroke(canvas, it, strokeColor, activeWidth, eraserEnabled) }
        canvas.restoreToCount(layer)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (mode == Mode.NAVIGATE) tapDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                if (mode == Mode.DRAW) startStroke(event.x, event.y) else beginPan(event, SKIP_NONE)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // A second finger always means "move the canvas", even mid-stroke.
                cancelStroke()
                beginPan(event, SKIP_NONE)
            }

            MotionEvent.ACTION_MOVE -> {
                if (drawing) {
                    extendStroke(event.x, event.y)
                } else if (panning) {
                    val fx = focusOf(event, SKIP_NONE, horizontal = true)
                    val fy = focusOf(event, SKIP_NONE, horizontal = false)
                    viewMatrix.postTranslate(fx - lastFocusX, fy - lastFocusY)
                    lastFocusX = fx
                    lastFocusY = fy
                }
            }

            MotionEvent.ACTION_POINTER_UP ->
                // Re-anchor on the remaining fingers, or the canvas jumps.
                beginPan(event, event.actionIndex)

            MotionEvent.ACTION_UP -> {
                if (drawing) {
                    finishStroke(event.x, event.y)
                    performClick()
                }
                panning = false
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelStroke()
                panning = false
            }

            else -> return super.onTouchEvent(event)
        }

        invalidate()
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    fun setPhoto(bitmap: Bitmap) {
        photo?.recycle()
        photo = bitmap
        fitContent()
        invalidate()
    }

    fun undo() {
        if (strokes.isEmpty()) return
        undone.add(strokes.removeAt(strokes.lastIndex))
        invalidate()
        onHistoryChanged?.invoke()
    }

    fun redo() {
        if (undone.isEmpty()) return
        strokes.add(undone.removeAt(undone.lastIndex))
        invalidate()
        onHistoryChanged?.invoke()
    }

    /** Clears the ink. The photo stays where it is. */
    fun clear() {
        if (strokes.isEmpty() && undone.isEmpty()) return
        strokes.clear()
        undone.clear()
        invalidate()
        onHistoryChanged?.invoke()
    }

    /** Frames the photo — or, with no photo, the ink — in the middle of the view. */
    fun fitContent() {
        if (width == 0 || height == 0) return

        val target = photoBounds() ?: inkBounds()
        if (target == null) {
            viewMatrix.reset()
            invalidate()
            return
        }

        val scale = (min(width / target.width(), height / target.height()) * FIT_PADDING)
            .coerceIn(MIN_SCALE, MAX_SCALE)
        viewMatrix.reset()
        viewMatrix.postTranslate(-target.centerX(), -target.centerY())
        viewMatrix.postScale(scale, scale)
        viewMatrix.postTranslate(width / 2f, height / 2f)
        invalidate()
    }

    /**
     * The drawing flattened for export: the photo at its native resolution with the
     * ink composited on top. The frame covers the photo *and* any ink drawn beyond
     * its edges, so nothing on the canvas is silently cropped away.
     */
    fun exportBitmap(): Bitmap? {
        val photoRect = photoBounds()
        val inkRect = inkBounds()
        val bounds = when {
            photoRect != null && inkRect != null -> photoRect.apply { union(inkRect) }
            else -> photoRect ?: inkRect ?: return null
        }
        if (bounds.width() < 1f || bounds.height() < 1f) return null

        val longest = max(bounds.width(), bounds.height())
        val scale = if (longest > MAX_EXPORT_PX) MAX_EXPORT_PX / longest else 1f
        val outWidth = (bounds.width() * scale).toInt().coerceAtLeast(1)
        val outHeight = (bounds.height() * scale).toInt().coerceAtLeast(1)

        val out = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(paperColor)
        canvas.scale(scale, scale)
        canvas.translate(-bounds.left, -bounds.top)
        photo?.let { canvas.drawBitmap(it, 0f, 0f, photoPaint) }

        // Same reasoning as onDraw: only pay for a layer when something erases.
        val needsLayer = strokes.any { it.eraser }
        val layer = if (needsLayer) canvas.saveLayer(null, null) else canvas.save()
        strokes.forEach { drawStroke(canvas, it.path, it.color, it.width, it.eraser) }
        canvas.restoreToCount(layer)
        return out
    }

    private fun startStroke(screenX: Float, screenY: Float) {
        val world = toWorld(screenX, screenY)
        // Stored in world units so the stroke keeps its on-screen weight as drawn.
        activeWidth = (brushWidth / currentScale()).coerceAtLeast(MIN_WORLD_WIDTH)
        activePath = Path().apply {
            moveTo(world[0], world[1])
            // A tap that never moves should still leave a dot.
            lineTo(world[0], world[1])
        }
        lastWorldX = world[0]
        lastWorldY = world[1]
        drawing = true
    }

    private fun extendStroke(screenX: Float, screenY: Float) {
        val path = activePath ?: return
        val world = toWorld(screenX, screenY)
        // Quadratic segments through the midpoints smooth out the jitter that raw
        // touch samples would otherwise show as hard corners.
        val midX = (lastWorldX + world[0]) / 2f
        val midY = (lastWorldY + world[1]) / 2f
        path.quadTo(lastWorldX, lastWorldY, midX, midY)
        lastWorldX = world[0]
        lastWorldY = world[1]
    }

    private fun finishStroke(screenX: Float, screenY: Float) {
        val path = activePath ?: return
        val world = toWorld(screenX, screenY)
        path.lineTo(world[0], world[1])
        strokes.add(Stroke(path, strokeColor, activeWidth, eraserEnabled))
        // A new stroke forks the history, so anything that was undone is gone.
        undone.clear()
        activePath = null
        drawing = false
        onHistoryChanged?.invoke()
    }

    private fun cancelStroke() {
        if (!drawing) return
        activePath = null
        drawing = false
        invalidate()
    }

    private fun beginPan(event: MotionEvent, skipIndex: Int) {
        panning = true
        lastFocusX = focusOf(event, skipIndex, horizontal = true)
        lastFocusY = focusOf(event, skipIndex, horizontal = false)
    }

    /**
     * Midpoint of the fingers still on screen. Following the midpoint rather than a
     * single pointer keeps a two-finger pinch from sliding the canvas around.
     */
    private fun focusOf(event: MotionEvent, skipIndex: Int, horizontal: Boolean): Float {
        var sum = 0f
        var count = 0
        for (i in 0 until event.pointerCount) {
            if (i == skipIndex) continue
            sum += if (horizontal) event.getX(i) else event.getY(i)
            count++
        }
        return if (count == 0) 0f else sum / count
    }

    private fun toWorld(screenX: Float, screenY: Float): FloatArray {
        viewMatrix.invert(inverseMatrix)
        mappedPoint[0] = screenX
        mappedPoint[1] = screenY
        inverseMatrix.mapPoints(mappedPoint)
        return mappedPoint
    }

    private fun currentScale(): Float {
        viewMatrix.getValues(matrixValues)
        val scale = matrixValues[Matrix.MSCALE_X]
        return if (scale <= 0f) 1f else scale
    }

    /** World-space extent of the photo. One world unit is one photo pixel. */
    private fun photoBounds(): RectF? =
        photo?.let { RectF(0f, 0f, it.width.toFloat(), it.height.toFloat()) }

    /** World-space extent of every stroke, widened to cover the ink itself. */
    private fun inkBounds(): RectF? {
        if (strokes.isEmpty()) return null

        val bounds = RectF()
        var first = true
        strokes.forEach { stroke ->
            stroke.path.computeBounds(scratchRect, true)
            // computeBounds ignores stroke width, so grow by half of it plus a margin.
            val pad = stroke.width / 2f + EXPORT_MARGIN
            scratchRect.inset(-pad, -pad)
            if (first) {
                bounds.set(scratchRect)
                first = false
            } else {
                bounds.union(scratchRect)
            }
        }
        return bounds
    }

    private fun drawGrid(canvas: Canvas, scale: Float) {
        // Without some texture, panning an empty backdrop looks like nothing moved.
        var step = GRID_STEP
        var guard = 0
        while (step * scale < MIN_GRID_PX && guard++ < GRID_GUARD) step *= 4f
        guard = 0
        while (step * scale > MAX_GRID_PX && guard++ < GRID_GUARD) step /= 4f

        viewMatrix.invert(inverseMatrix)
        scratchRect.set(0f, 0f, width.toFloat(), height.toFloat())
        inverseMatrix.mapRect(scratchRect)

        gridPaint.strokeWidth = 1f / scale

        var x = floor(scratchRect.left / step) * step
        while (x <= scratchRect.right) {
            canvas.drawLine(x, scratchRect.top, x, scratchRect.bottom, gridPaint)
            x += step
        }
        var y = floor(scratchRect.top / step) * step
        while (y <= scratchRect.bottom) {
            canvas.drawLine(scratchRect.left, y, scratchRect.right, y, gridPaint)
            y += step
        }
    }

    private fun drawStroke(canvas: Canvas, path: Path, color: Int, width: Float, eraser: Boolean) {
        strokePaint.color = color
        strokePaint.strokeWidth = width
        strokePaint.xfermode = if (eraser) eraseMode else null
        canvas.drawPath(path, strokePaint)
        strokePaint.xfermode = null
    }

    private companion object {
        const val MIN_SCALE = 0.05f
        const val MAX_SCALE = 24f
        const val FIT_PADDING = 0.92f
        const val MIN_WORLD_WIDTH = 0.05f
        const val MAX_EXPORT_PX = 3072f
        const val EXPORT_MARGIN = 8f
        const val GRID_STEP = 128f
        const val MIN_GRID_PX = 28f
        const val MAX_GRID_PX = 224f
        const val GRID_GUARD = 16
        const val SKIP_NONE = -1
    }
}
