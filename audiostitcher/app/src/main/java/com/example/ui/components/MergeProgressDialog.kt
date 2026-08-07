package com.example.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                Icon(
                    imageVector = when (mergeState) {
                        is AudioViewModel.MergeUiState.Processing -> Icons.Default.Merge
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
                        Text(
                            text = "Aguarde enquanto os áudios sequenciais são concatenados em um único arquivo...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        LinearProgressIndicator(
                            progress = mergeState.progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .testTag("merge_progress_bar")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(mergeState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    is AudioViewModel.MergeUiState.Success -> {
                        Text(
                            text = "Os ${mergeState.mergedAudio.clipCount} áudios da pasta foram unificados com sucesso em um único arquivo de áudio!",
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
