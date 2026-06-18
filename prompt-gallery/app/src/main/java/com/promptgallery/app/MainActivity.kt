package com.promptgallery.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.promptgallery.ui.navigation.PromptGalleryApp
import com.promptgallery.ui.theme.PromptGalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleShareIntent(intent)

        lifecycleScope.launch {
            viewModel.messages.collect { message ->
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            PromptGalleryTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PromptGalleryApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    /** Imports images sent to the app via the system share sheet. */
    private fun handleShareIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                getParcelable(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
                    viewModel.importShared(listOf(it))
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                getParcelableList(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
                    viewModel.importShared(it)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun <T> getParcelable(intent: Intent, key: String, clazz: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, clazz)
        } else {
            intent.getParcelableExtra(key)
        }

    @Suppress("DEPRECATION")
    private fun <T> getParcelableList(intent: Intent, key: String, clazz: Class<T>): List<T>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(key, clazz)
        } else {
            intent.getParcelableArrayListExtra(key)
        }
}
