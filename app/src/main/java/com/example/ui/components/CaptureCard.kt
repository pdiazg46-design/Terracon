package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.CaptureItemEntity
import com.example.data.local.CaptureType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CaptureCard(
    capture: CaptureItemEntity,
    isPlaying: Boolean,
    onPlayAudio: (String) -> Unit,
    onToggleCompleted: (CaptureItemEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onAgentClean: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val dateString = remember(capture.timestamp) { dateFormat.format(Date(capture.timestamp)) }

    val (typeColor, typeIcon, typeLabel) = when (capture.type) {
        CaptureType.MEETING -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Groups,
            "Reunión"
        )
        CaptureType.EXPENSE -> Triple(
            Color(0xFF00897B),
            Icons.Default.ReceiptLong,
            "Gasto Rendido"
        )
        CaptureType.INSTRUCTION -> Triple(
            Color(0xFFFFB300),
            Icons.Default.RecordVoiceOver,
            "Instrucción / Tarea"
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("capture_card_${capture.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Type badge, Project tag, Date & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = typeColor.copy(alpha = 0.15f),
                        contentColor = typeColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = typeLabel,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = capture.projectName,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { onDelete(capture.id) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_button_${capture.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Google Drive Sync & Agent Cleaning Status Indicator
            Surface(
                color = when (capture.syncStatus) {
                    "CLEARED_BY_AGENT" -> Color(0xFFE8F5E9)
                    "SYNCED_DRIVE" -> Color(0xFFE3F2FD)
                    else -> Color(0xFFFFF3E0)
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val statusText = when (capture.syncStatus) {
                            "CLEARED_BY_AGENT" -> "🟢 Procesado y Depurado de Drive por Agente"
                            "SYNCED_DRIVE" -> "🔵 Sincronizado en Drive (Bandeja AT-SIT)"
                            else -> "🟠 Guardado Local (Sin señal/Pendiente Sync)"
                        }
                        val statusColor = when (capture.syncStatus) {
                            "CLEARED_BY_AGENT" -> Color(0xFF1B5E20)
                            "SYNCED_DRIVE" -> Color(0xFF0D47A1)
                            else -> Color(0xFFE65100)
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    if (capture.syncStatus == "SYNCED_DRIVE" && onAgentClean != null) {
                        TextButton(
                            onClick = { onAgentClean(capture.id) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Limpiar Drive",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D47A1)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title and Checkbox (if instruction)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (capture.type == CaptureType.INSTRUCTION) {
                    Checkbox(
                        checked = capture.isCompleted,
                        onCheckedChange = { onToggleCompleted(capture) },
                        modifier = Modifier.testTag("checkbox_task_${capture.id}")
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = capture.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (capture.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (capture.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )

                    if (capture.type == CaptureType.EXPENSE && capture.amount > 0) {
                        Text(
                            text = "Monto: $${String.format("%.2f", capture.amount)} ${if (capture.expenseMerchant.isNotBlank()) "• " + capture.expenseMerchant else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Thumbnail Image if Expense Receipt photo exists
            if (!capture.photoPath.isNullOrBlank()) {
                val imgFile = File(capture.photoPath)
                if (imgFile.exists()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AsyncImage(
                        model = imgFile,
                        contentDescription = "Foto de Boleta / Factura",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Audio Player Bar if Audio recording exists
            if (!capture.audioPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { onPlayAudio(capture.audioPath) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("play_audio_button_${capture.id}")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar Audio" else "Reproducir Audio",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPlaying) "Reproduciendo audio de justificación..." else "Nota de voz de justificación/acta",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (capture.audioDurationSeconds > 0) {
                                Text(
                                    text = "Duración: ${capture.audioDurationSeconds} seg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // AI Summary preview / expandable details
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "IA",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Análisis Autónomo de la IA",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expandir",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (capture.aiSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = capture.aiSummary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column {
                            if (capture.aiActionItems.isNotBlank()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Text(
                                    text = "Acciones Tácticas Identificadas:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = capture.aiActionItems,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (capture.rawNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Notas adicionales: ${capture.rawNotes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
