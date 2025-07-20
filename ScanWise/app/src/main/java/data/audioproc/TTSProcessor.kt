package data.audioproc

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale
import java.util.UUID

class TTSProcessor(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isInitialized = false

    // Map of language codes to Locale objects
    private val languageLocales = mapOf(
        "en" to Locale.US,
        "fr" to Locale.FRANCE,
        "es" to Locale("es", "ES")
    )

    init {
        initTTS()
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS

            if (isInitialized) {
                // Default to English
                textToSpeech?.language = Locale.US
            }
        }
    }

    fun setLanguage(languageCode: String) {
        if (!isInitialized) return

        val locale = languageLocales[languageCode] ?: Locale.US
        textToSpeech?.language = locale
    }

    fun generateAudioFile(text: String, onComplete: (String) -> Unit) {
        if (!isInitialized) {
            onComplete("")
            return
        }

        val fileName = "tts_audio_${UUID.randomUUID()}.wav"
        val file = File(context.cacheDir, fileName)
        val filePath = file.absolutePath

        // Set up utterance progress listener
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}

            override fun onDone(utteranceId: String) {
                onComplete(filePath)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                onComplete("")
            }
        })

        // Generate audio file
        textToSpeech?.synthesizeToFile(text, null, file, "TTS_UTTERANCE")
    }

    fun playAudio(audioPath: String, onComplete: () -> Unit) {
        stopAudio()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    onComplete()
                }
                setOnErrorListener { _, _, _ ->
                    release()
                    mediaPlayer = null
                    onComplete()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            onComplete()
        }
    }

    fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }

    fun shutdown() {
        stopAudio()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}