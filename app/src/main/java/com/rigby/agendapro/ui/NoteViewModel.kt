package com.rigby.agendapro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rigby.agendapro.data.local.NoteEntity
import com.rigby.agendapro.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// Definición de los bloques de contenido
sealed class NoteBlock {
    data class Text(val id: String = UUID.randomUUID().toString(), var text: String) : NoteBlock()
    data class Image(val id: String = UUID.randomUUID().toString(), val uri: String) : NoteBlock()
    data class Audio(val id: String = UUID.randomUUID().toString(), val path: String) : NoteBlock()
    data class Checkbox(val id: String = UUID.randomUUID().toString(), var text: String, var checked: Boolean) : NoteBlock()
}

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Serializador simple: Convierte lista de bloques a String
    fun serializeBlocks(blocks: List<NoteBlock>): String {
        return blocks.joinToString(separator = "«BLOCK»") { block ->
            when(block) {
                is NoteBlock.Text -> "TEXT|${block.text}"
                is NoteBlock.Image -> "IMAGE|${block.uri}"
                is NoteBlock.Audio -> "AUDIO|${block.path}"
                is NoteBlock.Checkbox -> "CHECK|${block.checked}|${block.text}"
            }
        }
    }

    // Deserializador: Convierte String a lista de bloques
    fun parseBlocks(data: String?): List<NoteBlock> {
        if (data.isNullOrEmpty()) return listOf(NoteBlock.Text(text = "")) // Bloque inicial vacío

        return data.split("«BLOCK»").mapNotNull { raw ->
            val parts = raw.split("|", limit = 2)
            if (parts.isEmpty()) return@mapNotNull null

            when(parts[0]) {
                "TEXT" -> NoteBlock.Text(text = parts.getOrElse(1) { "" })
                "IMAGE" -> NoteBlock.Image(uri = parts.getOrElse(1) { "" })
                "AUDIO" -> NoteBlock.Audio(path = parts.getOrElse(1) { "" })
                "CHECK" -> {
                    val checkParts = parts.getOrElse(1){""}.split("|", limit = 2)
                    val checked = checkParts.getOrElse(0){"false"}.toBoolean()
                    val text = checkParts.getOrElse(1){""}
                    NoteBlock.Checkbox(text = text, checked = checked)
                }
                else -> null
            }
        }
    }

    fun saveNote(
        currentNote: NoteEntity?,
        title: String,
        blocks: List<NoteBlock>,
        urgency: Int,
        isPinned: Boolean,
        reminderTime: Long?,
        isAlarm: Boolean,
        repeatMode: String?
    ) {
        if (title.isBlank() && blocks.isEmpty()) return

        viewModelScope.launch {
            val blocksString = serializeBlocks(blocks)
            // Generamos un preview de texto plano para la búsqueda
            val plainContent = blocks.filterIsInstance<NoteBlock.Text>().joinToString(" ") { it.text }

            val noteToSave = currentNote?.copy(
                title = title,
                content = plainContent,
                urgency = urgency,
                isPinned = isPinned,
                blocksData = blocksString,
                reminderTime = reminderTime,
                isAlarm = isAlarm,
                repeatMode = repeatMode,
                createdAt = System.currentTimeMillis()
            ) ?: NoteEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                content = plainContent,
                urgency = urgency,
                isPinned = isPinned,
                blocksData = blocksString,
                reminderTime = reminderTime,
                isAlarm = isAlarm,
                repeatMode = repeatMode,
                createdAt = System.currentTimeMillis()
            )
            repository.saveNote(noteToSave)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun deleteMultipleNotes(ids: Set<String>) {
        viewModelScope.launch {
            val notesToDelete = notes.value.filter { it.id in ids }
            notesToDelete.forEach { repository.deleteNote(it) }
        }
    }
}