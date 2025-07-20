package com.example.scanwise.ui.mainscreen

import data.documentstore.Document

data class MainScreenState(
    val documents: List<Document> = emptyList(),
    val isAudioPlaying: Boolean = false,
    val currentPlayingDocId: Long? = null
)