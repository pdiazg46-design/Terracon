package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.example.data.local.CaptureItemEntity
import com.example.data.local.CaptureType
import com.example.data.local.ProjectEntity
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.CaptureCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(
    projects: List<ProjectEntity>,
    meetings: List<CaptureItemEntity>,
    isRecording: Boolean,
    recordingSeconds: Int,
    isProcessingAi: Boolean,
    playingAudioPath: String?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> File?,
    onSaveMeeting: (title: String, projectId: Long, projectName: String, audioFile: File?, notes: String) -> Unit,
    onPlayAudio: (String) -> Unit,
    onDeleteCapture: (Long) -> Unit,
    onAgentClean: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
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

            // Recording Card Section
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
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grabar Audio de Reunión",
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
                            label = { Text("Proyecto Asignado") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("meeting_project_dropdown")
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

                    // Audio Recording Visualizer & Timer
                    if (isRecording) {
                        Text(
                            text = "GRABANDO REUNIÓN (${recordingSeconds}s)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AudioWaveformBar(isRecording = true)
                        Spacer(modifier = Modifier.height(12.dp))
                    } else if (recordedFile != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Audio capturado (${recordedFile?.name})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Recording Mic Button
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                recordedFile = onStopRecording()
                            } else {
                                recordedFile = null
                                onStartRecording()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            .testTag("record_meeting_mic_btn")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Detener Grabación" else "Iniciar Grabación",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = {
                            val proj = selectedProject ?: return@Button
                            val finalTitle = "Reunión ${proj.name}"
                            onSaveMeeting(finalTitle, proj.id, proj.name, recordedFile, "")
                            recordedFile = null
                        },
                        enabled = !isRecording && !isProcessingAi && selectedProject != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_meeting_btn")
                    ) {
                        if (isProcessingAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Procesando Acuerdos con IA...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Procesar y Guardar Reunión")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Historial de Reuniones Procesadas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (meetings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se han grabado reuniones aún.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(meetings, key = { it.id }) { meeting ->
                CaptureCard(
                    capture = meeting,
                    isPlaying = playingAudioPath == meeting.audioPath,
                    onPlayAudio = onPlayAudio,
                    onToggleCompleted = {},
                    onDelete = onDeleteCapture,
                    onAgentClean = onAgentClean
                )
            }
        }
    }
}
