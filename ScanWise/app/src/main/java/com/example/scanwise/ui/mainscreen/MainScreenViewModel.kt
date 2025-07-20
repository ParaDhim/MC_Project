package com.example.scanwise.ui.mainscreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanwise.util.FileUriHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import data.audioproc.TTSProcessor
import data.documentstore.Document
import data.documentstore.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState = _uiState.asStateFlow()

    // I assume this is defined elsewhere in your actual code
    private var ttsProcessor: TTSProcessor? = null
    fun initTextToSpeech(context: Context) {
        if (ttsProcessor == null) {
            ttsProcessor = TTSProcessor(context)
        }
    }
    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch {
            try {
                val documents = documentRepository.getDocuments()
                _uiState.update { it.copy(documents = documents) }
            } catch (_: Exception) {
                // Handle error if needed
            }
        }
    }

    fun playAudio(docId: Long, audioPath: String) {
        if (audioPath.isNotBlank()) {
            // First stop any currently playing audio
            if (_uiState.value.isAudioPlaying) {
                stopAudio()
            }

            // Update state to indicate this document is playing
            _uiState.update { it.copy(
                isAudioPlaying = true,
                currentPlayingDocId = docId
            )}

            // Play the audio using ttsProcessor
            ttsProcessor?.playAudio(audioPath) {
                // When playback completes, reset the state
                _uiState.update { it.copy(
                    isAudioPlaying = false,
                    currentPlayingDocId = null
                )}
            }
        }
    }

    fun stopAudio() {
        ttsProcessor?.stopAudio()
        _uiState.update { it.copy(
            isAudioPlaying = false,
            currentPlayingDocId = null
        )}
    }

    fun shareDocumentImage(context: Context, document: Document) {
        if (document.imageUri.isNotEmpty()) {
            val shareableUri = FileUriHelper.getShareableUri(context, document.imageUri)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, shareableUri)
                type = "image/*"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val shareChooser = Intent.createChooser(shareIntent, "Share Document Image")
            context.startActivity(shareChooser)
        }
    }
}