package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.AudioViewModel
import com.example.ui.theme.WhatsAppGreen

@Composable
fun MergeProgressDialog(
    mergeState: AudioViewModel.MergeUiState,
    onDismiss: () -> Unit,
    onShareMerged: () -> Unit,
    onSaveToStorage: () -> Unit = {}
) {
    if (mergeState is AudioViewModel.MergeUiState.Idle) return

    AlertDialog(
        onDismissRequest = {
            if (mergeState !is AudioViewModel.MergeUiState.Processing) {
                onDismiss()
            }
        },
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mergeState is AudioViewModel.MergeUiState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = when (mergeState) {
                            is AudioViewModel.MergeUiState.Success -> Icons.Default.CheckCircle
                            is AudioViewModel.MergeUiState.Error -> Icons.Default.Error
                            else -> Icons.Default.Merge
                        },
                        contentDescription = null,
                        tint = when (mergeState) {
                            is AudioViewModel.MergeUiState.Success -> WhatsAppGreen
                            is AudioViewModel.MergeUiState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (mergeState) {
                        is AudioViewModel.MergeUiState.Processing -> "Unificando Áudios..."
                        is AudioViewModel.MergeUiState.Success -> "Unificação Concluída!"
                        is AudioViewModel.MergeUiState.Error -> "Erro na Unificação"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (mergeState) {
                    is AudioViewModel.MergeUiState.Processing -> {
                        val animatedProgress by animateFloatAsState(
                            targetValue = mergeState.progress,
                            label = "mergeProgressAnimation"
                        )

                        Text(
                            text = "Aguarde enquanto os áudios são concatenados e processados em um único arquivo...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (mergeState.totalClips > 0) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "Áudio ${mergeState.currentClipIndex} de ${mergeState.totalClips}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    Text(
                                        text = "${(animatedProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LinearProgressIndicator(
                                    progress = animatedProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .testTag("merge_progress_bar")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = mergeState.statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is AudioViewModel.MergeUiState.Success -> {
                        Text(
                            text = "Os ${mergeState.mergedAudio.clipCount} áudios da pasta foram unificados com sucesso em um único arquivo!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = mergeState.mergedAudio.fileName,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Duração Total: ${com.example.util.AudioStorageManager.formatDuration(mergeState.mergedAudio.totalDurationMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📍 Salvo em: /storage/emulated/0/Music/AudioStitch",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    is AudioViewModel.MergeUiState.Error -> {
                        Text(
                            text = mergeState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {}
                }
            }
        },
        confirmButton = {
            when (mergeState) {
                is AudioViewModel.MergeUiState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onShareMerged,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_merged_result_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar / Compartilhar")
                        }

                        OutlinedButton(
                            onClick = onSaveToStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_merged_result_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salvar na Memória do Celular")
                        }
                    }
                }
                is AudioViewModel.MergeUiState.Error -> {
                    Button(onClick = onDismiss) {
                        Text("OK")
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            if (mergeState is AudioViewModel.MergeUiState.Success) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fechar")
                }
            }
        }
    )
}
