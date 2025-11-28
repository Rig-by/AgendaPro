package com.rigby.agendapro.data.repository

import com.rigby.agendapro.data.local.NoteDao
import com.rigby.agendapro.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val dao: NoteDao
) {
    val allNotes: Flow<List<NoteEntity>> = dao.getAllNotes()
    val stickyNotes: Flow<List<NoteEntity>> = dao.getStickyNotes()
    val recentNotes: Flow<List<NoteEntity>> = dao.getRecentNotes()

    // Función unificada para guardar (Insertar o Actualizar)
    suspend fun saveNote(note: NoteEntity) {
        dao.insertNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) {
        dao.deleteNote(note)
    }

    suspend fun getNoteById(id: String): NoteEntity? {
        return dao.getNoteById(id)
    }
}