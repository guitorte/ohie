package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.AudioClip
import com.example.data.model.MergedAudio
import com.example.data.model.ProjectWithClips
import com.example.ui.AudioViewModel
import com.example.ui.components.*
import com.example.ui.theme.WhatsAppGreen
import com.example.util.AudioStorageManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val activeProjectWithClips by viewModel.activeProjectWithClips.collectAsState()
    val allProjectsWithClips by viewModel.allProjectsWithClips.collectAsState()
    val allMergedAudios by viewModel.allMergedAudios.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val importBanner by viewModel.importBannerState.collectAsState()
    val mergeState by viewModel.mergeState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Pasta Ativa, 1: Minhas Pastas, 2: Unificados

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<ProjectWithClips?>(null) }
    var folderToDelete by remember { mutableStateOf<ProjectWithClips?>(null) }
    var showMergeFormatDialog by remember { mutableStateOf(false) }
    var showClearQueueDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var pendingFileToSave by remember { mutableStateOf<File?>(null) }

    // Launcher to save exported audio file to local storage via system file picker
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val fileToSave = pendingFileToSave
        if (uri != null && fileToSave != null && fileToSave.exists()) {
            val success = saveMergedFileToLocalStorage(context, fileToSave, uri)
            if (success) {
                android.widget.Toast.makeText(context, "✓ Arquivo salvo no seu armazenamento local com sucesso!", android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(context, "Erro ao salvar arquivo na memória.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        pendingFileToSave = null
    }

    fun launchSaveToStorage(file: File) {
        if (!file.exists()) return
        pendingFileToSave = file
        createDocumentLauncher.launch(file.name)
    }

    // File picker launcher for manually picking local audio files
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onAudioReceived(it, "Arquivo Local") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AudioJoiner",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        activeProjectWithClips?.project?.let { activeProj ->
                            Text(
                                text = "Ativa: ${activeProj.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.testTag("help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Ajuda e Instruções"
                        )
                    }
                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.testTag("add_folder_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Nova Pasta"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                AudioPlayerBottomBar(
                    playbackState = playbackState,
                    onPause = { viewModel.pausePlayback() },
                    onResume = { viewModel.resumePlayback() },
                    onSeek = { viewModel.seekTo(it) },
                    onSetSpeed = { viewModel.setPlaybackSpeed(it) },
                    onClose = { viewModel.playerHelper.stop() }
                )

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                        label = { Text("Pasta Ativa") },
                        modifier = Modifier.testTag("nav_active_folder")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        label = { Text("Minhas Pastas") },
                        modifier = Modifier.testTag("nav_all_folders")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Merge, contentDescription = null) },
                        label = { Text("Unificados (${allMergedAudios.size})") },
                        modifier = Modifier.testTag("nav_merged_audios")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 && activeProjectWithClips?.clips?.isNotEmpty() == true) {
                ExtendedFloatingActionButton(
                    onClick = { showMergeFormatDialog = true },
                    icon = { Icon(Icons.Default.Merge, contentDescription = null) },
                    text = { Text("Unificar Áudios") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_merge_audios")
                )
            } else if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showCreateFolderDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_create_folder")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nova Pasta")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Import banner notification
            ImportNotificationBanner(
                banner = importBanner,
                onDismiss = { viewModel.dismissImportBanner() }
            )

            when (selectedTab) {
                0 -> ActiveFolderTabContent(
                    activeProjectWithClips = activeProjectWithClips,
                    playbackState = playbackState,
                    onPlayClip = { viewModel.playClip(it) },
                    onMoveUp = { clip, clips -> viewModel.moveClipUp(clip, clips) },
                    onMoveDown = { clip, clips -> viewModel.moveClipDown(clip, clips) },
                    onDeleteClip = { viewModel.deleteClip(it) },
                    onAddAudioFile = { audioPickerLauncher.launch("audio/*") },
                    onClearQueueClick = { showClearQueueDialog = true },
                    onMergeClick = { showMergeFormatDialog = true },
                    onChangeFolderClick = { selectedTab = 1 }
                )

                1 -> FoldersTabContent(
                    allProjectsWithClips = allProjectsWithClips,
                    onSelectFolder = { projectWithClips ->
                        viewModel.setActiveFolder(projectWithClips.project.id)
                        selectedTab = 0
                    },
                    onSetActiveFolder = { viewModel.setActiveFolder(it.project.id) },
                    onRenameFolder = { folderToRename = it },
                    onDeleteFolder = { folderToDelete = it },
                    onCreateNewFolder = { showCreateFolderDialog = true }
                )

                2 -> MergedAudiosTabContent(
                    mergedAudios = allMergedAudios,
                    playbackState = playbackState,
                    onPlayMerged = { viewModel.playMergedAudio(it) },
                    onShareMerged = { shareAudioFile(context, File(it.filePath)) },
                    onSaveToStorageMerged = { launchSaveToStorage(File(it.filePath)) },
                    onDeleteMerged = { viewModel.deleteMergedAudio(it) }
                )
            }
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreateFolder = { name ->
                viewModel.createFolder(name)
                showCreateFolderDialog = false
                selectedTab = 0
            }
        )
    }

    folderToRename?.let { projectWithClips ->
        RenameFolderDialog(
            initialName = projectWithClips.project.name,
            onDismiss = { folderToRename = null },
            onRename = { newName ->
                viewModel.renameFolder(projectWithClips.project.id, newName)
                folderToRename = null
            }
        )
    }

    folderToDelete?.let { projectWithClips ->
        DeleteFolderDialog(
            folderName = projectWithClips.project.name,
            onDismiss = { folderToDelete = null },
            onConfirmDelete = {
                viewModel.deleteFolder(projectWithClips)
                folderToDelete = null
            }
        )
    }

    if (showMergeFormatDialog) {
        val clipCount = activeProjectWithClips?.clips?.size ?: 0
        MergeFormatDialog(
            clipCount = clipCount,
            onDismiss = { showMergeFormatDialog = false },
            onConfirmMerge = { format ->
                showMergeFormatDialog = false
                viewModel.mergeActiveFolder(format)
            }
        )
    }

    // Progress Dialog during merging
    MergeProgressDialog(
        mergeState = mergeState,
        onDismiss = { viewModel.resetMergeState() },
        onShareMerged = {
            if (mergeState is AudioViewModel.MergeUiState.Success) {
                val merged = (mergeState as AudioViewModel.MergeUiState.Success).mergedAudio
                shareAudioFile(context, File(merged.filePath))
            }
        },
        onSaveToStorage = {
            if (mergeState is AudioViewModel.MergeUiState.Success) {
                val merged = (mergeState as AudioViewModel.MergeUiState.Success).mergedAudio
                launchSaveToStorage(File(merged.filePath))
            }
        }
    )

    // Clear Queue Dialog
    if (showClearQueueDialog) {
        val clipCount = activeProjectWithClips?.clips?.size ?: 0
        val folderName = activeProjectWithClips?.project?.name ?: "Pasta Ativa"
        AlertDialog(
            onDismissRequest = { showClearQueueDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Limpar Fila de Áudios?") },
            text = {
                Text("Tem certeza de que deseja remover todos os $clipCount áudios da fila da pasta '$folderName'?\n\nTodos os arquivos da fila serão apagados para você começar uma nova unificação do zero.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearActiveQueue()
                        showClearQueueDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_queue_btn")
                ) {
                    Text("Limpar Fila")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearQueueDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = { Icon(Icons.Default.Help, contentDescription = null, tint = WhatsAppGreen) },
            title = { Text("Como usar o AudioJoiner", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Abra o WhatsApp e selecione um áudio.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Toque em Encaminhar / Compartilhar e escolha o AudioJoiner. O áudio será salvo na pasta ativa automaticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "2. Envie 10+ áudios sequenciais.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Como o aplicativo é otimizado para velocidade, cada áudio encaminhado entra direto na fila da pasta sem interrupções.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "3. Reordene e Unifique.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Ajuste a ordem pelas setas (⬆ ⬇), ouça a prévia de qualquer clipe e toque em 'Unificar Áudios' para gerar o arquivo final unificado!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Entendi!")
                }
            }
        )
    }
}

@Composable
fun ActiveFolderTabContent(
    activeProjectWithClips: ProjectWithClips?,
    playbackState: com.example.util.AudioPlayerHelper.PlaybackState,
    onPlayClip: (AudioClip) -> Unit,
    onMoveUp: (AudioClip, List<AudioClip>) -> Unit,
    onMoveDown: (AudioClip, List<AudioClip>) -> Unit,
    onDeleteClip: (AudioClip) -> Unit,
    onAddAudioFile: () -> Unit,
    onClearQueueClick: () -> Unit,
    onMergeClick: () -> Unit,
    onChangeFolderClick: () -> Unit
) {
    val project = activeProjectWithClips?.project
    val clips = activeProjectWithClips?.sortedClips ?: emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        // Active Folder Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = project?.name ?: "Pasta Ativa",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    TextButton(onClick = onChangeFolderClick) {
                        Text("Trocar Pasta", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${clips.size} áudios • Duração total: ${AudioStorageManager.formatDuration(activeProjectWithClips?.totalDurationMs ?: 0L)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = AudioStorageManager.formatFileSize(activeProjectWithClips?.totalSizeBytes ?: 0L),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddAudioFile,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_local_audio_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar", style = MaterialTheme.typography.labelMedium)
                    }

                    if (clips.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearQueueClick,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("clear_queue_btn")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Limpar Fila", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpar Fila", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = onMergeClick,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("merge_active_folder_btn")
                        ) {
                            Icon(Icons.Default.Merge, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unificar (${clips.size})", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Clip List or Empty State
        if (clips.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Pasta vazia",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Encaminhe áudios do WhatsApp (.opus/.ogg) um por um para este app ou toque em 'Adicionar Áudio' para carregar do celular.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(clips, key = { _, clip -> clip.id }) { index, clip ->
                    val isPlayingThis = playbackState.clipId == clip.id && playbackState.isPlaying

                    ClipListItem(
                        clip = clip,
                        sequenceNumber = index + 1,
                        isPlaying = isPlayingThis,
                        isFirst = index == 0,
                        isLast = index == clips.size - 1,
                        onPlayToggle = { onPlayClip(clip) },
                        onMoveUp = { onMoveUp(clip, clips) },
                        onMoveDown = { onMoveDown(clip, clips) },
                        onDelete = { onDeleteClip(clip) }
                    )
                }
            }
        }
    }
}

@Composable
fun FoldersTabContent(
    allProjectsWithClips: List<ProjectWithClips>,
    onSelectFolder: (ProjectWithClips) -> Unit,
    onSetActiveFolder: (ProjectWithClips) -> Unit,
    onRenameFolder: (ProjectWithClips) -> Unit,
    onDeleteFolder: (ProjectWithClips) -> Unit,
    onCreateNewFolder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Minhas Pastas de Áudio",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            TextButton(onClick = onCreateNewFolder) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nova Pasta")
            }
        }

        if (allProjectsWithClips.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = "Nenhuma pasta criada. Toque em 'Nova Pasta' para começar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allProjectsWithClips, key = { it.project.id }) { projectWithClips ->
                    FolderListItem(
                        projectWithClips = projectWithClips,
                        onSelectFolder = { onSelectFolder(projectWithClips) },
                        onSetActiveFolder = { onSetActiveFolder(projectWithClips) },
                        onRenameFolder = { onRenameFolder(projectWithClips) },
                        onDeleteFolder = { onDeleteFolder(projectWithClips) }
                    )
                }
            }
        }
    }
}

@Composable
fun MergedAudiosTabContent(
    mergedAudios: List<MergedAudio>,
    playbackState: com.example.util.AudioPlayerHelper.PlaybackState,
    onPlayMerged: (MergedAudio) -> Unit,
    onShareMerged: (MergedAudio) -> Unit,
    onSaveToStorageMerged: (MergedAudio) -> Unit = {},
    onDeleteMerged: (MergedAudio) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Histórico de Áudios Unificados",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (mergedAudios.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Merge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhum áudio unificado ainda",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Selecione a pasta ativa e toque em 'Unificar Áudios' para concatenar seus áudios do WhatsApp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(mergedAudios, key = { it.id }) { item ->
                    val isPlayingThis = playbackState.clipId == item.id && playbackState.isPlaying

                    MergedAudioItem(
                        mergedAudio = item,
                        isPlaying = isPlayingThis,
                        onPlayToggle = { onPlayMerged(item) },
                        onShare = { onShareMerged(item) },
                        onSaveToStorage = { onSaveToStorageMerged(item) },
                        onDelete = { onDeleteMerged(item) }
                    )
                }
            }
        }
    }
}

private fun shareAudioFile(context: Context, file: File) {
    if (!file.exists()) return

    try {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val mimeType = when {
            file.name.endsWith(".m4a") -> "audio/mp4"
            file.name.endsWith(".opus") || file.name.endsWith(".ogg") -> "audio/ogg"
            file.name.endsWith(".aac") -> "audio/aac"
            file.name.endsWith(".wav") -> "audio/wav"
            else -> "audio/*"
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Enviar Áudio Unificado via..."))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun saveMergedFileToLocalStorage(context: Context, sourceFile: File, targetUri: Uri): Boolean {
    return try {
        context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
            sourceFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
