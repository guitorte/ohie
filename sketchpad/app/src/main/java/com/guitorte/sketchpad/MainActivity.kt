package com.guitorte.sketchpad

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guitorte.sketchpad.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val swatchViews = mutableListOf<View>()
    private lateinit var eyedropperView: ImageView
    private lateinit var prefs: SharedPreferences

    /** Where a colour sampled from the canvas should land. */
    private enum class PickTarget { INK, BACKGROUND }

    private var pickTarget = PickTarget.INK
    private var picking = false

    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val backgrounds = intArrayOf(
        R.color.bg_mist, R.color.bg_white, R.color.bg_paper,
        R.color.bg_slate, R.color.bg_charcoal, R.color.bg_black
    )

    private val palette = intArrayOf(
        R.color.ink_black, R.color.ink_red, R.color.ink_orange, R.color.ink_yellow,
        R.color.ink_green, R.color.ink_teal, R.color.ink_blue, R.color.ink_purple,
        R.color.ink_brown, R.color.ink_white
    )

    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) loadPhoto(uri) }

    private val requestLegacyStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) saveDrawing() else toast(R.string.save_needs_permission)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        applyWindowInsets()
        buildPalette()

        binding.drawingView.canvasColor = prefs.getInt(
            KEY_BACKGROUND,
            ContextCompat.getColor(this, R.color.bg_mist)
        )

        binding.sizeSlider.addOnChangeListener { _, value, _ ->
            binding.drawingView.brushWidth = value
        }
        binding.drawingView.brushWidth = binding.sizeSlider.value

        // Eraser and laser are both "what the pen does", so they are exclusive:
        // turning one on releases the other rather than disabling its button.
        binding.eraserButton.addOnCheckedChangeListener { _, checked ->
            binding.drawingView.eraserEnabled = checked
            if (checked) binding.laserButton.isChecked = false
            applyMode()
        }

        binding.laserButton.addOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.eraserButton.isChecked = false
                toast(R.string.laser_hint)
            }
            applyMode()
        }

        // The pan toggle only swaps what one finger does; the drawing tools keep
        // their state, so switching back resumes the brush or eraser as it was.
        binding.panButton.addOnCheckedChangeListener { _, _ -> applyMode() }

        binding.paletteButton.addOnCheckedChangeListener { _, checked -> setToolsVisible(checked) }

        binding.drawingView.onColorPicked = ::onColorPicked

        binding.photoButton.setOnClickListener {
            pickPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.undoButton.setOnClickListener { binding.drawingView.undo() }
        binding.redoButton.setOnClickListener { binding.drawingView.redo() }
        binding.clearButton.setOnClickListener { confirmClear() }
        binding.moreButton.setOnClickListener { showMoreMenu() }

        binding.drawingView.onHistoryChanged = ::refreshHistoryButtons
        refreshHistoryButtons()

        selectColor(0)
        setToolsVisible(binding.paletteButton.isChecked)
        applyMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdownNow()
    }

    /**
     * The app draws edge to edge, so the toolbar has to keep clear of the system
     * bars itself rather than relying on the framework to inset it.
     */
    private fun applyWindowInsets() {
        // Opt in explicitly so the behaviour is the same on every API level, not
        // only on Android 15, where targetSdk 35 turns edge-to-edge on for us.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right)
            binding.toolbar.updatePadding(bottom = bars.bottom + dp(8))
            insets
        }
    }

    // region drawing tools

    private fun buildPalette() {
        val size = dp(40)
        val margin = dp(4)

        // The dropper sits with the colours and doubles as the swatch showing the
        // current ink, which is the only way to display a colour picked off canvas.
        eyedropperView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
            setPadding(dp(9), dp(9), dp(9), dp(9))
            setImageResource(R.drawable.ic_eyedropper)
            contentDescription = getString(R.string.eyedropper)
            setOnClickListener {
                if (picking) cancelPicking() else startPicking(PickTarget.INK)
            }
        }
        binding.colorRow.addView(eyedropperView)

        palette.forEachIndexed { index, colorRes ->
            val color = ContextCompat.getColor(this, colorRes)
            val swatch = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                background = swatchDrawable(color, selected = false)
                contentDescription = getString(R.string.colour_swatch)
                setOnClickListener { selectColor(index) }
            }

            binding.colorRow.addView(swatch)
            swatchViews.add(swatch)
        }
    }

    private fun selectColor(index: Int) =
        applyInkColor(ContextCompat.getColor(this, palette[index]), fromPalette = index)

    /**
     * @param fromPalette index of the swatch that supplied the colour, or null when
     *   it came off the canvas — in which case no swatch is ringed and the dropper
     *   carries the colour instead.
     */
    private fun applyInkColor(color: Int, fromPalette: Int?) {
        binding.drawingView.strokeColor = color

        // Choosing a colour is an implicit request to stop erasing.
        if (binding.eraserButton.isChecked) binding.eraserButton.isChecked = false

        swatchViews.forEachIndexed { i, view ->
            val swatchColor = ContextCompat.getColor(this, palette[i])
            view.background = swatchDrawable(swatchColor, selected = i == fromPalette)
        }
        eyedropperView.background = swatchDrawable(color, selected = fromPalette == null)
        eyedropperView.imageTintList = ColorStateList.valueOf(
            if (isLight(color)) Color.BLACK else Color.WHITE
        )
    }

    private fun startPicking(target: PickTarget) {
        pickTarget = target
        picking = true
        applyMode()
        toast(R.string.eyedropper_hint)
    }

    private fun cancelPicking() {
        picking = false
        applyMode()
    }

    private fun onColorPicked(color: Int) {
        picking = false
        applyMode()
        when (pickTarget) {
            PickTarget.INK -> applyInkColor(color, fromPalette = null)
            PickTarget.BACKGROUND -> applyBackground(color)
        }
    }

    private fun applyBackground(color: Int) {
        binding.drawingView.canvasColor = color
        prefs.edit().putInt(KEY_BACKGROUND, color).apply()
    }

    private fun swatchDrawable(color: Int, selected: Boolean): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            val outline = if (selected) {
                if (isLight(color)) Color.BLACK else Color.WHITE
            } else {
                Color.argb(60, 0, 0, 0)
            }
            setStroke(dp(if (selected) 3 else 1), outline)
        }

    private fun isLight(color: Int): Boolean {
        val luminance = 0.299 * Color.red(color) +
            0.587 * Color.green(color) +
            0.114 * Color.blue(color)
        return luminance > 160
    }

    /**
     * Derives what one finger does from the two things that can claim it. Nothing
     * here touches the drawing tools' own state, which is what lets a trip through
     * pan or pick mode return to exactly the brush or eraser that was in use.
     */
    private fun applyMode() {
        val panning = binding.panButton.isChecked
        binding.drawingView.mode = when {
            picking -> DrawingView.Mode.PICK
            panning -> DrawingView.Mode.NAVIGATE
            binding.laserButton.isChecked -> DrawingView.Mode.LASER
            else -> DrawingView.Mode.DRAW
        }

        val drawingDisabled = panning || picking
        val alpha = if (drawingDisabled) 0.38f else 1f

        // Dimmed per swatch rather than on the whole row: the dropper stays live
        // even while panning, and must not look disabled when it is not.
        swatchViews.forEach {
            it.isEnabled = !drawingDisabled
            it.alpha = alpha
        }
        binding.sizeSlider.alpha = alpha
        binding.sizeSlider.isEnabled = !drawingDisabled
        binding.eraserButton.alpha = alpha
        binding.eraserButton.isEnabled = !drawingDisabled
        binding.laserButton.alpha = alpha
        binding.laserButton.isEnabled = !drawingDisabled
    }

    /** Retracts the colours and brush size, leaving the action bar in place. */
    private fun setToolsVisible(visible: Boolean) {
        TransitionManager.beginDelayedTransition(binding.toolbar)
        binding.toolPanel.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun showMoreMenu() {
        val menu = PopupMenu(this, binding.moreButton, Gravity.END)
        menu.menu.add(0, MENU_BACKGROUND, 0, R.string.background_colour)
        menu.menu.add(0, MENU_SAVE, 1, R.string.save)

        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_BACKGROUND -> showBackgroundDialog()
                MENU_SAVE -> onSaveRequested()
            }
            true
        }
        menu.show()
    }

    /** Preset backdrops plus a dropper, for sampling one straight off the photo. */
    private fun showBackgroundDialog() {
        val current = binding.drawingView.canvasColor
        val size = dp(48)
        val margin = dp(6)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(8))
        }

        val scroller = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.background_colour)
            .setView(scroller)
            .setNegativeButton(R.string.cancel, null)
            .create()

        backgrounds.forEach { colorRes ->
            val color = ContextCompat.getColor(this, colorRes)
            row.addView(
                View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(margin, margin, margin, margin)
                    }
                    background = swatchDrawable(color, selected = color == current)
                    setOnClickListener {
                        applyBackground(color)
                        dialog.dismiss()
                    }
                }
            )
        }

        row.addView(
            ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                setPadding(dp(11), dp(11), dp(11), dp(11))
                setImageResource(R.drawable.ic_eyedropper)
                contentDescription = getString(R.string.pick_from_canvas)
                background = swatchDrawable(current, selected = false)
                imageTintList = ColorStateList.valueOf(
                    if (isLight(current)) Color.BLACK else Color.WHITE
                )
                setOnClickListener {
                    dialog.dismiss()
                    startPicking(PickTarget.BACKGROUND)
                }
            }
        )

        dialog.show()
    }

    private fun refreshHistoryButtons() {
        binding.undoButton.isEnabled = binding.drawingView.canUndo
        binding.redoButton.isEnabled = binding.drawingView.canRedo
        binding.clearButton.isEnabled = binding.drawingView.canUndo || binding.drawingView.canRedo
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_title)
            .setMessage(R.string.clear_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear) { _, _ -> binding.drawingView.clear() }
            .show()
    }

    // endregion

    // region photo

    private fun loadPhoto(uri: Uri) {
        toast(R.string.loading_photo)
        ioExecutor.execute {
            val bitmap = runCatching { decodeScaled(uri) }.getOrNull()
            mainHandler.post {
                if (isFinishing || isDestroyed) {
                    bitmap?.recycle()
                    return@post
                }
                if (bitmap == null) {
                    toast(R.string.photo_failed)
                } else {
                    binding.drawingView.setPhoto(bitmap)
                }
            }
        }
    }

    /**
     * Decodes at most [MAX_PHOTO_PX] on the long edge. Full-resolution phone photos
     * would otherwise be tens of megabytes on the heap for no visible gain, and one
     * world unit is one photo pixel, so this also caps the exported size.
     */
    private fun decodeScaled(uri: Uri): Bitmap? {
        val probe = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, probe) }
        if (probe.outWidth <= 0 || probe.outHeight <= 0) return null

        var sample = 1
        while (probe.outWidth / sample > MAX_PHOTO_PX || probe.outHeight / sample > MAX_PHOTO_PX) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null

        return applyExifOrientation(uri, decoded)
    }

    /** Cameras record orientation in EXIF rather than rotating the pixels. */
    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { if (it != bitmap) bitmap.recycle() }
        }.getOrDefault(bitmap)
    }

    // endregion

    // region saving

    private fun onSaveRequested() {
        if (!binding.drawingView.canUndo && !binding.drawingView.hasPhoto) {
            toast(R.string.nothing_to_save)
            return
        }
        // MediaStore needs no permission from Android 10 on.
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED

        if (needsPermission) {
            requestLegacyStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveDrawing()
        }
    }

    private fun saveDrawing() {
        val bitmap = runCatching { binding.drawingView.exportBitmap() }.getOrNull()
        if (bitmap == null) {
            toast(R.string.save_failed)
            return
        }

        val name = "sketch_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".png"

        val saved = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeViaMediaStore(bitmap, name)
            } else {
                writeToLegacyPictures(bitmap, name)
            }
        }.getOrDefault(false)

        bitmap.recycle()
        toast(if (saved) R.string.saved_to_gallery else R.string.save_failed)
    }

    private fun writeViaMediaStore(bitmap: Bitmap, name: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/Sketchpad"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false

        val written = runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: false
        }.getOrDefault(false)

        if (written) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            resolver.delete(uri, null, null)
        }
        return written
    }

    @Suppress("DEPRECATION")
    private fun writeToLegacyPictures(bitmap: Bitmap, name: String): Boolean {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Sketchpad"
        )
        if (!dir.exists() && !dir.mkdirs()) return false

        val file = File(dir, name)
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) return false
        }
        // Make the new file visible to the gallery straight away.
        MediaStore.Images.Media.insertImage(contentResolver, file.absolutePath, name, null)
        return true
    }

    // endregion

    private fun toast(messageRes: Int) =
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val MAX_PHOTO_PX = 3072
        const val PREFS = "sketchpad"
        const val KEY_BACKGROUND = "background_color"
        const val MENU_BACKGROUND = 1
        const val MENU_SAVE = 2
    }
}
