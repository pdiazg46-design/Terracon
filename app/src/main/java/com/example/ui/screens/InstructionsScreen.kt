package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CaptureItemEntity
import com.example.data.local.ProjectEntity
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.CaptureCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsScreen(
    projects: List<ProjectEntity>,
    instructions: List<CaptureItemEntity>,
    isRecording: Boolean,
    recordingSeconds: Int,
    isProcessingAi: Boolean,
    playingAudioPath: String?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> File?,
    onSaveInstruction: (
        title: String,
        projectId: Long,
        projectName: String,
        audioFile: File?,
        notes: String
    ) -> Unit,
    onPlayAudio: (String) -> Unit,
    onToggleTaskCompleted: (CaptureItemEntity) -> Unit,
    onDeleteCapture: (Long) -> Unit,
    onAgentClean: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(projects) {
        if (selectedProject == null && projects.isNotEmpty()) {
            selectedProject = projects.first()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grabar Instrucción o Recordatorio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Select Project Dropdown
                    ExposedDropdownMenuBox(
                        expanded = projectDropdownExpanded,
                        onExpandedChange = { projectDropdownExpanded = !projectDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedProject?.name ?: "Seleccionar Proyecto",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Proyecto / Área") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("instruction_project_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = projectDropdownExpanded,
                            onDismissRequest = { projectDropdownExpanded = false }
                        ) {
                            projects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text(proj.name) },
                                    onClick = {
                                        selectedProject = proj
                                        projectDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isRecording) {
                        Text(
                            text = "GRABANDO INSTRUCCIÓN DE VOZ (${recordingSeconds}s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AudioWaveformBar(isRecording = true)
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (recordedAudioFile != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Instrucción grabada con éxito", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    IconButton(
                        onClick = {
                            if (isRecording) {
                                recordedAudioFile = onStopRecording()
                            } else {
                                recordedAudioFile = null
                                onStartRecording()
                            }
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) MaterialTheme.colorScheme.error else Color(0xFFFFB300))
                            .testTag("record_instruction_mic_btn")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Detener" else "Grabar Instrucción",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val proj = selectedProject ?: return@Button
                            val finalTitle = "Instrucción de Voz ${proj.name}"
                            onSaveInstruction(
                                finalTitle,
                                proj.id,
                                proj.name,
                                recordedAudioFile,
                                ""
                            )
                            recordedAudioFile = null
                        },
                        enabled = !isRecording && !isProcessingAi && selectedProject != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_instruction_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                    ) {
                        if (isProcessingAi) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generando Tareas con IA...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar e Entregar a IA")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Instrucciones y Recordatorios de Proyectos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (instructions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay instrucciones o recordatorios grabados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(instructions, key = { it.id }) { item ->
                CaptureCard(
                    capture = item,
                    isPlaying = playingAudioPath == item.audioPath,
                    onPlayAudio = onPlayAudio,
                    onToggleCompleted = onToggleTaskCompleted,
                    onDelete = onDeleteCapture,
                    onAgentClean = onAgentClean
                )
            }
        }
    }
}
