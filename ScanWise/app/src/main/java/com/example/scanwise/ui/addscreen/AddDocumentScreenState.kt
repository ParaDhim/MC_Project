package com.example.scanwise.ui.addscreen

import data.translation.TranslationService

data class AddDocumentScreenState(
    val title: String = "",
    val fullText: String = "",
    val textPath: String = "",
    val audioPath: String = "",
    val isLoading: Boolean = false,
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