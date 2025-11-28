package com.rigby.agendapro.di

import android.content.Context
import androidx.room.Room
import com.rigby.agendapro.data.local.AppDatabase
import com.rigby.agendapro.data.local.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "notiagenda_db"
        ).fallbackToDestructiveMigration() // Borra la DB si cambiamos la estructura (útil en desarrollo)
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(db: AppDatabase): NoteDao {
        return db.noteDao()
    }
}