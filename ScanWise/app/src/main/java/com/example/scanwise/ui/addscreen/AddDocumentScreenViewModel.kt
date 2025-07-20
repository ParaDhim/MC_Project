package com.example.scanwise.ui.addscreen

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import data.audioproc.TTSProcessor
import data.documentstore.Document
import data.documentstore.DocumentRepository
import data.textproc.MLKitProcessor
import data.translation.TranslationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDocumentScreenViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val mlKitProcessor: MLKitProcessor
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddDocumentScreenState())
    val uiState = _uiState.asStateFlow()

    private var ttsProcessor: TTSProcessor? = null
    private val translationService = TranslationService()

    fun initTextToSpeech(context: Context) {
        if (ttsProcessor == null) {
            ttsProcessor = TTSProcessor(context)
        }
    }

    fun processDocument(imageUri: Uri, context: Context) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Initialize TTS
        initTextToSpeech(context)

        viewModelScope.launch {
            try {
                mlKitProcessor.processDocument(imageUri, context) { recognizedText ->
                    val extracted = recognizedText.split("|")
                    _uiState.value = _uiState.value.copy(
                        title = extracted[0],
                        fullText = extracted[1],
                        textPath = extracted[2],
                        translatedTitle = extracted[0],
                        translatedText = extracted[1],
                        isLoading = false
                    )
                }
                generateAudio()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle, translatedTitle = newTitle)
    }

    fun onTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(fullText = newText, translatedText = newText)
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
            sourceLanguage = TranslationService.LANGUAGE_ENGLISH,
            targetLanguage = languageCode,
            titleToTranslate = currentState.title,
            textToTranslate = currentState.fullText
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

    private fun generateAudioForLanguage(text: String, languageCode: String) {
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(isGeneratingAudio = true)

        ttsProcessor?.let { processor ->
            // Set language for TTS
            processor.setLanguage(languageCode)

            // Generate audio file
            processor.generateAudioFile(text) { audioPath ->
                _uiState.value = _uiState.value.copy(
                    audioPath = audioPath,
                    isGeneratingAudio = false
                )
            }
        }
    }

    fun generateAudio() {
        val currentText = _uiState.value.translatedText
        val languageCode = _uiState.value.selectedLanguage

        generateAudioForLanguage(currentText, languageCode)
    }

    fun playAudio() {
        val audioPath = _uiState.value.audioPath
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

    fun saveDocument(imageUri: Uri) {
        viewModelScope.launch {
            val currentState = _uiState.value

            val newDocument = Document(
                id = 0L,
                title = if (currentState.selectedLanguage == TranslationService.LANGUAGE_ENGLISH)
                    currentState.title else currentState.translatedTitle,
                fullText = if (currentState.selectedLanguage == TranslationService.LANGUAGE_ENGLISH)
                    currentState.fullText else currentState.translatedText,
                textPath = currentState.textPath,
                imageUri = imageUri.toString(),
                audioPath = currentState.audioPath,
                language = currentState.selectedLanguage
            )
            documentRepository.addDocument(newDocument)
        }
    }

    override fun onCleared() {
        super.onCleared()
        translationService.cleanup()
        ttsProcessor?.shutdown()
    }
}