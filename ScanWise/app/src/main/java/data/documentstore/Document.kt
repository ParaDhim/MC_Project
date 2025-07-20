package data.documentstore

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val fullText: String,
    val textPath: String,
    val imageUri: String,
    val audioPath: String,
    val language: String = "en" // Default to English
)