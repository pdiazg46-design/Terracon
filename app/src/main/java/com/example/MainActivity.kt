package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CaptureType
import com.example.ui.MainViewModel
import com.example.ui.components.AtSitLogo
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

sealed class NavTab(val title: String, val icon: ImageVector, val route: String) {
    object Home : NavTab("Resumen", Icons.Default.Dashboard, "home")
    object Meetings : NavTab("Reuniones", Icons.Default.Groups, "meetings")
    object Expenses : NavTab("Gastos", Icons.Default.ReceiptLong, "expenses")
    object Instructions : NavTab("Instrucciones", Icons.Default.RecordVoiceOver, "instructions")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val allCaptures by viewModel.allCaptures.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingSeconds by viewModel.recordingSeconds.collectAsStateWithLifecycle()
    val isProcessingAi by viewModel.isProcessingAi.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val playingAudioPath by viewModel.playingAudioPath.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()

    var selectedTabItem by remember { mutableStateOf<NavTab>(NavTab.Home) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission state & launcher
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasMicPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                )
            )
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val meetingCaptures = remember(allCaptures) {
        allCaptures.filter { it.type == CaptureType.MEETING }
    }
    val expenseCaptures = remember(allCaptures) {
        allCaptures.filter { it.type == CaptureType.EXPENSE }
    }
    val instructionCaptures = remember(allCaptures) {
        allCaptures.filter { it.type == CaptureType.INSTRUCTION }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        AtSitLogo(height = 32.dp, showBackgroundBadge = true)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showNewProjectDialog = true },
                        modifier = Modifier.testTag("add_project_topbar_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nuevo Proyecto")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val tabs = listOf(
                    NavTab.Home,
                    NavTab.Meetings,
                    NavTab.Expenses,
                    NavTab.Instructions
                )
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTabItem.route == tab.route,
                        onClick = { selectedTabItem = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabItem) {
                NavTab.Home -> HomeScreen(
                    projects = projects,
                    captures = allCaptures,
                    playingAudioPath = playingAudioPath,
                    onPlayAudio = { viewModel.playAudio(it) },
                    onToggleTaskCompleted = { viewModel.toggleTaskCompleted(it) },
                    onDeleteCapture = { viewModel.deleteCapture(it) },
                    onNavigateToTab = { index ->
                        when (index) {
                            1 -> selectedTabItem = NavTab.Meetings
                            2 -> selectedTabItem = NavTab.Expenses
                            3 -> selectedTabItem = NavTab.Instructions
                        }
                    },
                    onOpenNewProjectDialog = { showNewProjectDialog = true },
                    onAgentClean = { viewModel.markAgentCleared(it) },
                    onSyncDrive = { viewModel.syncAllWithDrive() }
                )

                NavTab.Meetings -> MeetingsScreen(
                    projects = projects,
                    meetings = meetingCaptures,
                    isRecording = isRecording,
                    recordingSeconds = recordingSeconds,
                    isProcessingAi = isProcessingAi,
                    playingAudioPath = playingAudioPath,
                    onStartRecording = {
                        if (hasMicPermission) {
                            viewModel.startRecording("meeting")
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        }
                    },
                    onStopRecording = { viewModel.stopRecording() },
                    onSaveMeeting = { title, projectId, projectName, audioFile, notes ->
                        viewModel.saveCapture(
                            title = title,
                            type = CaptureType.MEETING,
                            projectId = projectId,
                            projectName = projectName,
                            audioFile = audioFile,
                            photoFile = null,
                            rawNotes = notes
                        )
                    },
                    onPlayAudio = { viewModel.playAudio(it) },
                    onDeleteCapture = { viewModel.deleteCapture(it) },
                    onAgentClean = { viewModel.markAgentCleared(it) }
                )

                NavTab.Expenses -> ExpensesScreen(
                    projects = projects,
                    expenses = expenseCaptures,
                    isRecording = isRecording,
                    recordingSeconds = recordingSeconds,
                    isProcessingAi = isProcessingAi,
                    playingAudioPath = playingAudioPath,
                    onStartRecording = {
                        if (hasMicPermission) {
                            viewModel.startRecording("expense")
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        }
                    },
                    onStopRecording = { viewModel.stopRecording() },
                    onSaveExpense = { title, projectId, projectName, audioFile, photoFile, notes, amount ->
                        viewModel.saveCapture(
                            title = title,
                            type = CaptureType.EXPENSE,
                            projectId = projectId,
                            projectName = projectName,
                            audioFile = audioFile,
                            photoFile = photoFile,
                            rawNotes = notes,
                            amount = amount
                        )
                    },
                    onPlayAudio = { viewModel.playAudio(it) },
                    onDeleteCapture = { viewModel.deleteCapture(it) },
                    onAgentClean = { viewModel.markAgentCleared(it) }
                )

                NavTab.Instructions -> InstructionsScreen(
                    projects = projects,
                    instructions = instructionCaptures,
                    isRecording = isRecording,
                    recordingSeconds = recordingSeconds,
                    isProcessingAi = isProcessingAi,
                    playingAudioPath = playingAudioPath,
                    onStartRecording = {
                        if (hasMicPermission) {
                            viewModel.startRecording("instruction")
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        }
                    },
                    onStopRecording = { viewModel.stopRecording() },
                    onSaveInstruction = { title, projectId, projectName, audioFile, notes ->
                        viewModel.saveCapture(
                            title = title,
                            type = CaptureType.INSTRUCTION,
                            projectId = projectId,
                            projectName = projectName,
                            audioFile = audioFile,
                            photoFile = null,
                            rawNotes = notes
                        )
                    },
                    onPlayAudio = { viewModel.playAudio(it) },
                    onToggleTaskCompleted = { viewModel.toggleTaskCompleted(it) },
                    onDeleteCapture = { viewModel.deleteCapture(it) },
                    onAgentClean = { viewModel.markAgentCleared(it) }
                )
            }

            if (showNewProjectDialog) {
                NewProjectDialog(
                    onDismiss = { showNewProjectDialog = false },
                    onCreateProject = { name, desc, client, color ->
                        viewModel.createProject(name, desc, client, color)
                    }
                )
            }
        }
    }
}
