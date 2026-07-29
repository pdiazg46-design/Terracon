package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CaptureItemEntity
import com.example.data.local.CaptureType
import com.example.data.local.ProjectEntity
import com.example.ui.components.AtSitLogoBanner
import com.example.ui.components.CaptureCard

@Composable
fun HomeScreen(
    projects: List<ProjectEntity>,
    captures: List<CaptureItemEntity>,
    playingAudioPath: String?,
    onPlayAudio: (String) -> Unit,
    onToggleTaskCompleted: (CaptureItemEntity) -> Unit,
    onDeleteCapture: (Long) -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onOpenNewProjectDialog: () -> Unit,
    onAgentClean: ((Long) -> Unit)? = null,
    onSyncDrive: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCaptures = remember(captures, selectedProjectId, searchQuery) {
        captures.filter { item ->
            val matchesProject = selectedProjectId == null || item.projectId == selectedProjectId
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.projectName.contains(searchQuery, ignoreCase = true) ||
                    item.aiSummary.contains(searchQuery, ignoreCase = true)
            matchesProject && matchesSearch
        }
    }

    val totalExpenses = remember(captures) {
        captures.filter { it.type == CaptureType.EXPENSE }.sumOf { it.amount }
    }
    val meetingCount = remember(captures) {
        captures.count { it.type == CaptureType.MEETING }
    }
    val pendingInstructions = remember(captures) {
        captures.count { it.type == CaptureType.INSTRUCTION && !it.isCompleted }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            AtSitLogoBanner()
            Spacer(modifier = Modifier.height(12.dp))
            // Executive Welcome Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "IA",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Agente IA Conectado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = onOpenNewProjectDialog) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Nuevo Proyecto",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Centro de Captura AT-SIT",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Registra reuniones, rendiciones de gastos e instrucciones por voz para tus proyectos activos Carrera Pinto y Diego de Almagro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Google Drive Sync & Lightweight Inbox Card
            Surface(
                color = Color(0xFFF0F4F9),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Google Drive",
                                tint = Color(0xFF0F52BA),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bandeja Liviana Google Drive",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F52BA)
                            )
                        }

                        if (onSyncDrive != null) {
                            OutlinedButton(
                                onClick = onSyncDrive,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = "Sincronizar", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sincronizar Drive", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Operación 100% Offline: Tus audios, gastos y reuniones se guardan siempre en local aunque no tengas señal.\n• Sincronización a Drive: Al conectar, la app sube las capturas a tu carpeta 'AT-SIT_Inbox'.\n• Depuración Automática: Cuando tu agente Antigravity consulta la información, los datos de Drive se depuran para mantener la bandeja limpia y liviana.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334455),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Reuniones",
                    value = "$meetingCount",
                    icon = Icons.Default.Groups,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(1) }
                )
                StatCard(
                    title = "Gastos",
                    value = "$${String.format("%.0f", totalExpenses)}",
                    icon = Icons.Default.ReceiptLong,
                    color = Color(0xFF00897B),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(2) }
                )
                StatCard(
                    title = "Pendientes",
                    value = "$pendingInstructions",
                    icon = Icons.Default.RecordVoiceOver,
                    color = Color(0xFFFFB300),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(3) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Action Buttons
            Text(
                text = "Acciones Rápidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onNavigateToTab(1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_record_meeting_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reunión", fontSize = 12.sp)
                }

                Button(
                    onClick = { onNavigateToTab(2) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_scan_receipt_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gasto", fontSize = 12.sp)
                }

                Button(
                    onClick = { onNavigateToTab(3) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_voice_instruction_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Instrucción", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Filter Chips by Project
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Proyectos y Capturas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onOpenNewProjectDialog) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Nuevo", fontSize = 12.sp)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedProjectId == null,
                        onClick = { selectedProjectId = null },
                        label = { Text("Todos (${captures.size})") }
                    )
                }
                items(projects) { project ->
                    FilterChip(
                        selected = selectedProjectId == project.id,
                        onClick = { selectedProjectId = project.id },
                        label = { Text(project.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar en acuerdos, boletas o instrucciones...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_captures_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (filteredCaptures.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron resultados" else "Aún no hay capturas en este proyecto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(filteredCaptures, key = { it.id }) { capture ->
                CaptureCard(
                    capture = capture,
                    isPlaying = playingAudioPath == capture.audioPath,
                    onPlayAudio = onPlayAudio,
                    onToggleCompleted = onToggleTaskCompleted,
                    onDelete = onDeleteCapture,
                    onAgentClean = onAgentClean
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
