package com.rigby.agendapro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String, // Texto plano para búsqueda rápida
    val urgency: Int,
    val isPinned: Boolean,

    // --- CAMPO MÁGICO PARA EL ORDEN ---
    // Guardaremos un JSON o String especial aquí con el orden exacto
    // Formato: "TEXT|Hola...<SEP>IMAGE|uri<SEP>AUDIO|path<SEP>CHECK|comprar pan|false"
    val blocksData: String? = null,

    // Mantenemos estos por compatibilidad y filtros
    val reminderTime: Long? = null,
    val isAlarm: Boolean = false,
    val repeatMode: String? = null,

    val createdAt: Long
)