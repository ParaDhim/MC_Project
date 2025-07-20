package data.textproc

import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MLKitProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun processDocument(imageUri: Uri, context: Context, onResult: (String) -> Unit) {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val title = extractTitle(visionText)
                    val fullText = visionText.text

                    // Save OCR text to file
                    val textPath = saveOCRTextToFile(context, fullText)

                    val resultString = "$title|$fullText|$textPath"
                    onResult(resultString)

                }
                .addOnFailureListener { e ->
                    onResult("Error processing document: ${e.message}")
                }
        } catch (e: IOException) {
            e.printStackTrace()
            onResult("Error: ${e.message}")
        }
    }

    private fun saveOCRTextToFile(context: Context, text: String): String {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "OCR_Text_$timestamp.txt"

            // Save to app-specific external storage if available, otherwise use internal storage
            val file = if (context.getExternalFilesDir(null) != null) {
                File(context.getExternalFilesDir(null), filename)
            } else {
                File(context.filesDir, filename)
            }

            FileOutputStream(file).use { outputStream ->
                outputStream.write(text.toByteArray())
            }

            return file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }

    companion object {

        @VisibleForTesting
        fun extractTitle(text: Text): String {
            if(text.textBlocks.size > 0) {
                return text.textBlocks[0].lines[0].text
            }
            return "No title found"
        }

    }
}