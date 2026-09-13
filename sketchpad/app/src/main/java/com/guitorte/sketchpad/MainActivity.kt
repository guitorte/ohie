package com.guitorte.sketchpad

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guitorte.sketchpad.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val swatchViews = mutableListOf<View>()

    private val palette = intArrayOf(
        R.color.ink_black, R.color.ink_red, R.color.ink_orange, R.color.ink_yellow,
        R.color.ink_green, R.color.ink_teal, R.color.ink_blue, R.color.ink_purple,
        R.color.ink_brown, R.color.ink_white
    )

    private val requestLegacyStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) saveDrawing() else toast(R.string.save_needs_permission)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        buildPalette()

        binding.sizeSlider.addOnChangeListener { _, value, _ ->
            binding.drawingView.strokeWidth = value
        }
        binding.drawingView.strokeWidth = binding.sizeSlider.value

        binding.eraserButton.addOnCheckedChangeListener { _, checked ->
            binding.drawingView.eraserEnabled = checked
        }

        binding.undoButton.setOnClickListener { binding.drawingView.undo() }
        binding.redoButton.setOnClickListener { binding.drawingView.redo() }
        binding.clearButton.setOnClickListener { confirmClear() }
        binding.saveButton.setOnClickListener { onSaveRequested() }

        binding.drawingView.onHistoryChanged = ::refreshHistoryButtons
        refreshHistoryButtons()

        selectColor(0)
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

    private fun buildPalette() {
        val size = dp(40)
        val margin = dp(4)

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

    private fun selectColor(index: Int) {
        val color = ContextCompat.getColor(this, palette[index])
        binding.drawingView.strokeColor = color

        // Picking a colour is an implicit request to stop erasing.
        if (binding.eraserButton.isChecked) binding.eraserButton.isChecked = false

        swatchViews.forEachIndexed { i, view ->
            val swatchColor = ContextCompat.getColor(this, palette[i])
            view.background = swatchDrawable(swatchColor, selected = i == index)
        }
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

    private fun onSaveRequested() {
        if (!binding.drawingView.canUndo) {
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
        val bitmap = binding.drawingView.exportBitmap()
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

    private fun toast(messageRes: Int) =
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
