package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.local.CaptureItemEntity
import com.example.data.local.ProjectEntity
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.CaptureCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    projects: List<ProjectEntity>,
    expenses: List<CaptureItemEntity>,
    isRecording: Boolean,
    recordingSeconds: Int,
    isProcessingAi: Boolean,
    playingAudioPath: String?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> File?,
    onSaveExpense: (
        title: String,
        projectId: Long,
        projectName: String,
        audioFile: File?,
        photoFile: File?,
        notes: String,
        amount: Double
    ) -> Unit,
    onPlayAudio: (String) -> Unit,
    onDeleteCapture: (Long) -> Unit,
    onAgentClean: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(projects) {
        if (selectedProject == null && projects.isNotEmpty()) {
            selectedProject = projects.first()
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            // If failed or cancelled, reset photoFile
            photoFile = null
        }
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val targetFile = File(context.filesDir, "receipt_${System.currentTimeMillis()}.jpg")
                targetFile.outputStream().use { output ->
                    inputStream?.copyTo(output)
                }
                photoFile = targetFile
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF00897B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rendir Gasto con Foto y Audio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Project Selector
                    ExposedDropdownMenuBox(
                        expanded = projectDropdownExpanded,
                        onExpandedChange = { projectDropdownExpanded = !projectDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedProject?.name ?: "Seleccionar Proyecto",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Proyecto a Cargar Gasto") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("expense_project_dropdown")
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Photo preview or camera button
                    if (photoFile != null && photoFile!!.exists()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = photoFile,
                                contentDescription = "Boleta capturada",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { photoFile = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remover Foto", tint = Color.White)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val file = File(context.filesDir, "receipt_${System.currentTimeMillis()}.jpg")
                                    photoFile = file
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    tempPhotoUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("take_photo_camera_btn")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tomar Foto")
                            }

                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galería")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio justification section
                    Text(
                        text = "Justificación de Voz para el Gasto",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isRecording) {
                        Text(
                            text = "GRABANDO JUSTIFICACIÓN (${recordingSeconds}s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
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
                                Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Audio de justificación grabado", style = MaterialTheme.typography.bodySmall)
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
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) MaterialTheme.colorScheme.error else Color(0xFF00897B))
                            .testTag("record_expense_audio_btn")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Detener" else "Grabar Justificación",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val proj = selectedProject ?: return@Button
                            val finalTitle = "Gasto ${proj.name}"
                            onSaveExpense(
                                finalTitle,
                                proj.id,
                                proj.name,
                                recordedAudioFile,
                                photoFile,
                                "",
                                0.0
                            )
                            recordedAudioFile = null
                            photoFile = null
                        },
                        enabled = !isRecording && !isProcessingAi && selectedProject != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_expense_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                    ) {
                        if (isProcessingAi) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extrayendo Boleta e IA Justificando...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar Rendición con IA")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Historial de Gastos y Rendiciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (expenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se han registrado rendiciones de gastos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                CaptureCard(
                    capture = expense,
                    isPlaying = playingAudioPath == expense.audioPath,
                    onPlayAudio = onPlayAudio,
                    onToggleCompleted = {},
                    onDelete = onDeleteCapture,
                    onAgentClean = onAgentClean
                )
            }
        }
    }
}
