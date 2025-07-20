package com.example.scanwise.ui.addscreen

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.scanwise.R
import com.example.scanwise.ui.utils.LanguageSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentScreen(
    imageUri: Uri,
    onSave: () -> Unit
) {
    val viewModel: AddDocumentScreenViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val con: Context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.processDocument(imageUri, con)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = stringResource(R.string.document_details),
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.saveDocument(imageUri)
                    onSave()
                }
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.save_document)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            // Language selector
            LanguageSelector(
                selectedLanguage = uiState.selectedLanguage,
                onLanguageSelected = { languageCode ->
                    viewModel.setLanguage(languageCode)
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Document content fields
            TextField(
                value = uiState.translatedTitle,
                onValueChange = viewModel::onTitleChanged,
                label = { Text(stringResource(R.string.title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            TextField(
                value = uiState.translatedText,
                onValueChange = viewModel::onTextChanged,
                label = { Text(stringResource(R.string.extracted_text)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Audio controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Audio:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )

                when {
                    uiState.isGeneratingAudio -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = " Generating audio...",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    uiState.audioPath.isBlank() -> {
                        IconButton(onClick = { viewModel.generateAudio() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Generate Audio"
                            )
                        }
                        Text(
                            text = "Generate audio",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    uiState.isAudioPlaying -> {
                        IconButton(onClick = { viewModel.stopAudio() }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Stop Audio"
                            )
                        }
                        Text(
                            text = "Playing...",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    else -> {
                        IconButton(onClick = { viewModel.playAudio() }) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play Audio"
                            )
                        }
                        Text(
                            text = "Play audio",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Translation loading indicator
            if (uiState.isTranslating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Translating...",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = rememberAsyncImagePainter(model = imageUri),
                contentDescription = "Scanned Document",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extracting Information... Please wait",
                        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                    )
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 1.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}