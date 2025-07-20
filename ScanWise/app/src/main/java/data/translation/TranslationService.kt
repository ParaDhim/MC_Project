package data.translation

import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TranslationService {
    // Available languages with their codes
    companion object {
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_FRENCH = "fr"
        const val LANGUAGE_SPANISH = "es"

        // Get display name for language code
        fun getLanguageDisplayName(languageCode: String): String {
            return when (languageCode) {
                LANGUAGE_ENGLISH -> "English"
                LANGUAGE_FRENCH -> "French"
                LANGUAGE_SPANISH -> "Spanish"
                else -> "Unknown"
            }
        }
    }

    // Cache translators to avoid recreating them
    private val translators = mutableMapOf<String, com.google.mlkit.nl.translate.Translator>()

    fun translateText(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Don't translate if source and target are the same
        if (sourceLanguage == targetLanguage) {
            onComplete(text)
            return
        }

        // Create unique key for this language pair
        val translatorKey = "$sourceLanguage-$targetLanguage"

        try {
            // Get or create translator
            val translator = translators.getOrPut(translatorKey) {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .build()
                Translation.getClient(options)
            }

            // Check if model is downloaded and download if needed
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    // Perform translation
                    translator.translate(text)
                        .addOnSuccessListener { translatedText ->
                            onComplete(translatedText)
                        }
                        .addOnFailureListener { exception ->
                            onError(exception)
                        }
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun translateTextSuspend(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String = suspendCancellableCoroutine { continuation ->
        translateText(
            text = text,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            onComplete = { translatedText ->
                if (continuation.isActive) continuation.resume(translatedText)
            },
            onError = { exception ->
                if (continuation.isActive) continuation.resumeWithException(exception)
            }
        )
    }

    fun cleanup() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
}