package com.example.scanwise.ui.detailscreen

import data.documentstore.Document
import data.translation.TranslationService

data class DetailScreenState(
    val document: Document = Document(
        id = 0,
        title = "",
        fullText = "",
        textPath = "",
        imageUri = "",
        audioPath = "",
        language = TranslationService.LANGUAGE_ENGLISH
    ),
    val isTitleEditing: Boolean = false,
    val isTextEditing: Boolean = false,
    val showDialog: Boolean = false,
    val isAudioPlaying: Boolean = false,
    val isGeneratingAudio: Boolean = false,
    val isTranslating: Boolean = false,
    val availableLanguages: List<String> = listOf(
        TranslationService.LANGUAGE_ENGLISH,
        TranslationService.LANGUAGE_FRENCH,
        TranslationService.LANGUAGE_SPANISH
    ),
    val selectedLanguage: String = TranslationService.LANGUAGE_ENGLISH,
    val translatedTitle: String = "",
    val translatedText: String = ""
)
