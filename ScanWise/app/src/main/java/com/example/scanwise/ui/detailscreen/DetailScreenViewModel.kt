package com.example.scanwise.ui.detailscreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import data.audioproc.TTSProcessor
import data.documentstore.DocumentRepository
import data.translation.TranslationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailScreenViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailScreenState())
    val uiState = _uiState.asStateFlow()

    private var ttsProcessor: TTSProcessor? = null
    private val translationService = TranslationService()

    fun initTextToSpeech(context: Context) {
        if (ttsProcessor == null) {
            ttsProcessor = TTSProcessor(context)
        }
    }

    fun loadDocument(documentId: Long) {
        viewModelScope.launch {
            val document = documentRepository.getDocumentById(documentId)
            document?.let {
                _uiState.value = DetailScreenState(
                    document = it,
                    selectedLanguage = it.language,
                    translatedTitle = it.title,
                    translatedText = it.fullText
                )
            }
        }
    }

    fun toggleTitleEdit() {
        _uiState.value = _uiState.value.copy(
            isTitleEditing = !_uiState.value.isTitleEditing
        )
    }

    fun toggleTextEdit() {
        _uiState.value = _uiState.value.copy(
            isTextEditing = !_uiState.value.isTextEditing
        )
    }

    fun toggleShowDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = !_uiState.value.showDialog
        )
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(translatedTitle = newTitle)
    }

    fun updateText(newText: String) {
        _uiState.value = _uiState.value.copy(translatedText = newText)

        // Generate new audio based on updated text
        generateAudio(newText)
    }

    fun setLanguage(languageCode: String) {
        val currentState = _uiState.value

        // If we're already using this language, do nothing
        if (currentState.selectedLanguage == languageCode) return

        _uiState.value = currentState.copy(
            selectedLanguage = languageCode,
            isTranslating = true
        )

        // Translate content to the selected language
        translateContent(
            sourceLanguage = currentState.document.language, // Original document language
            targetLanguage = languageCode,
            titleToTranslate = currentState.document.title,
            textToTranslate = currentState.document.fullText
        )
    }

    private fun translateContent(
        sourceLanguage: String,
        targetLanguage: String,
        titleToTranslate: String,
        textToTranslate: String
    ) {
        viewModelScope.launch {
            try {
                // Don't translate if languages are the same
                if (sourceLanguage == targetLanguage) {
                    _uiState.value = _uiState.value.copy(
                        translatedTitle = titleToTranslate,
                        translatedText = textToTranslate,
                        isTranslating = false
                    )
                    return@launch
                }

                // First translate the title
                val translatedTitle = translationService.translateTextSuspend(
                    text = titleToTranslate,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage
                )

                // Then translate the full text
                val translatedText = translationService.translateTextSuspend(
                    text = textToTranslate,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage
                )

                // Update UI with translations
                _uiState.value = _uiState.value.copy(
                    translatedTitle = translatedTitle,
                    translatedText = translatedText,
                    isTranslating = false
                )

                // Generate audio for the translated text
                generateAudioForLanguage(translatedText, targetLanguage)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isTranslating = false)
            }
        }
    }

    fun saveDocument() {
        _uiState.value.let { state ->
            viewModelScope.launch {
                val updatedDocument = state.document.copy(
                    title = state.translatedTitle,
                    fullText = state.translatedText,
                    language = state.selectedLanguage
                )
                documentRepository.updateDocument(updatedDocument)

                // Update state with the new document
                _uiState.value = _uiState.value.copy(document = updatedDocument)
            }
        }
    }

    fun deleteDocument() {
        viewModelScope.launch {
            _uiState.value.let { state ->
                state.document.let { documentRepository.deleteDocument(it) }
            }
        }
    }

    private fun generateAudioForLanguage(text: String, languageCode: String) {
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(isGeneratingAudio = true)

        ttsProcessor?.let { processor ->
            // Set language for TTS
            processor.setLanguage(languageCode)

            // Generate audio file
            processor.generateAudioFile(text) { audioPath ->
                val updatedDocument = _uiState.value.document.copy(audioPath = audioPath)
                _uiState.value = _uiState.value.copy(
                    document = updatedDocument,
                    isGeneratingAudio = false
                )

                // Save the updated audio path
                viewModelScope.launch {
                    documentRepository.updateDocument(updatedDocument)
                }
            }
        }
    }

    fun generateAudio(newText: String = _uiState.value.translatedText) {
        if (newText.isNotBlank()) {
            val languageCode = _uiState.value.selectedLanguage
            generateAudioForLanguage(newText, languageCode)
        }
    }

    fun playAudio() {
        val audioPath = _uiState.value.document.audioPath
        if (audioPath.isNotBlank()) {
            _uiState.value = _uiState.value.copy(isAudioPlaying = true)

            ttsProcessor?.playAudio(audioPath) {
                _uiState.value = _uiState.value.copy(isAudioPlaying = false)
            }
        }
    }

    fun stopAudio() {
        ttsProcessor?.stopAudio()
        _uiState.value = _uiState.value.copy(isAudioPlaying = false)
    }

    override fun onCleared() {
        super.onCleared()
        translationService.cleanup()
        ttsProcessor?.shutdown()
    }
}