package com.rigby.agendapro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rigby.agendapro.data.local.NoteEntity
import com.rigby.agendapro.data.local.NoteDao

@Database(entities = [NoteEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}