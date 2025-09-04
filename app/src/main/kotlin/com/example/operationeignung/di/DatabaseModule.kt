package com.example.operationeignung.di

import android.content.Context
import androidx.room.Room
import com.example.operationeignung.data.database.AppDatabase
import com.example.operationeignung.data.database.dao.CategoryDao
import com.example.operationeignung.data.database.dao.QuestionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt-Modul für die Datenbank **/
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database.db"
        )
            //.addMigrations(MIGRATION_7_8)
            .fallbackToDestructiveMigration(false) // ← alte DB wird bei Versionssprung gelöscht und neu erstellt
            .build()
    }

    @Provides
    fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
}

