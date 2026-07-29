package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CaptureItemEntity
import com.example.data.local.CaptureType
import com.example.data.local.ProjectEntity
import com.example.data.repository.CaptureRepository
import com.example.util.AudioPlayer
import com.example.util.AudioRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ChatMessage(
    val sender: String, // "Usuario" or "IA Administrador"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CaptureRepository(application)
    private val audioRecorder = AudioRecorder(application)
    private val audioPlayer = AudioPlayer(application)

    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCaptures: StateFlow<List<CaptureItemEntity>> = repository.allCaptures
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Audio recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var recordedAudioFile: File? = null

    // Currently playing audio state
    private val _playingAudioPath = MutableStateFlow<String?>(null)
    val playingAudioPath: StateFlow<String?> = _playingAudioPath.asStateFlow()

    // Loading & status
    private val _isProcessingAi = MutableStateFlow(false)
    val isProcessingAi: StateFlow<Boolean> = _isProcessingAi.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // AI Manager Chat state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultProjectsExist()
            _chatMessages.value = listOf(
                ChatMessage(
                    sender = "IA Administrador",
                    text = "¡Hola! Soy tu Administrador Autónomo de Proyectos. Captura audios de reuniones, comprobantes de gastos o instrucciones de voz, y los procesaré para mantener tus proyectos bajo control."
                )
            )
        }
    }

    fun startRecording(prefix: String) {
        if (_isRecording.value) return
        recordedAudioFile = audioRecorder.start(prefix)
        if (recordedAudioFile != null) {
            _isRecording.value = true
            _recordingSeconds.value = 0
            recordingTimerJob = viewModelScope.launch {
                while (_isRecording.value) {
                    delay(1000)
                    _recordingSeconds.value += 1
                }
            }
        } else {
            _userMessage.value = "Error al iniciar el micrófono."
        }
    }

    fun stopRecording(): File? {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        _isRecording.value = false
        return audioRecorder.stop()
    }

    fun playAudio(path: String) {
        if (_playingAudioPath.value == path && audioPlayer.isPlaying) {
            audioPlayer.stop()
            _playingAudioPath.value = null
        } else {
            _playingAudioPath.value = path
            audioPlayer.play(path) {
                _playingAudioPath.value = null
            }
        }
    }

    fun stopAudio() {
        audioPlayer.stop()
        _playingAudioPath.value = null
    }

    fun saveCapture(
        title: String,
        type: CaptureType,
        projectId: Long,
        projectName: String,
        audioFile: File?,
        photoFile: File?,
        rawNotes: String,
        amount: Double = 0.0
    ) {
        viewModelScope.launch {
            _isProcessingAi.value = true
            try {
                val duration = _recordingSeconds.value
                val audioPath = audioFile?.absolutePath
                val photoPath = photoFile?.absolutePath

                repository.saveAndProcessCapture(
                    title = if (title.isBlank()) "Captura de ${type.name}" else title,
                    type = type,
                    projectId = projectId,
                    projectName = projectName,
                    audioPath = audioPath,
                    audioDurationSeconds = duration,
                    photoPath = photoPath,
                    rawNotes = rawNotes,
                    userEnteredAmount = amount
                )
                _userMessage.value = "✅ Registro procesado exitosamente por la IA."
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving capture", e)
                _userMessage.value = "Error al procesar registro: ${e.localizedMessage}"
            } finally {
                _isProcessingAi.value = false
                _recordingSeconds.value = 0
            }
        }
    }

    fun toggleTaskCompleted(capture: CaptureItemEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(capture)
        }
    }

    fun deleteCapture(id: Long) {
        viewModelScope.launch {
            repository.deleteCapture(id)
            _userMessage.value = "Registro eliminado."
        }
    }

    fun syncAllWithDrive() {
        viewModelScope.launch {
            try {
                val count = repository.syncAllDriveCaptures()
                _userMessage.value = "☁️ Google Drive Sincronizado ($count registros en bandeja)"
            } catch (e: Exception) {
                _userMessage.value = "Modo Offline: Guardado localmente. Se actualizará en Drive al reconectarse."
            }
        }
    }

    fun markAgentCleared(captureId: Long) {
        viewModelScope.launch {
            try {
                repository.markAgentCleared(captureId)
                _userMessage.value = "🧹 Agente Antigravity procesó y limpió la bandeja de Google Drive."
            } catch (e: Exception) {
                _userMessage.value = "Error al depurar Drive: ${e.localizedMessage}"
            }
        }
    }

    fun createProject(name: String, description: String, clientName: String, colorHex: String) {
        viewModelScope.launch {
            repository.createProject(name, description, clientName, colorHex)
            _userMessage.value = "Proyecto '$name' creado."
        }
    }

    fun sendUserQueryToAI(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage(sender = "Usuario", text = query)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isProcessingAi.value = true
            val aiResponse = repository.askProjectAI(query)
            _isProcessingAi.value = false
            val aiMsg = ChatMessage(sender = "IA Administrador", text = aiResponse)
            _chatMessages.value = _chatMessages.value + aiMsg
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stop()
        audioPlayer.stop()
    }
}
