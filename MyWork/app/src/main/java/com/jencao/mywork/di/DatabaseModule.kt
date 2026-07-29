package com.jencao.mywork.di

import android.content.Context
import com.jencao.mywork.data.local.AppDatabase
import com.jencao.mywork.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides fun provideTaskDao(db: AppDatabase) = db.taskDao()
    @Provides fun provideTaskCheckinDao(db: AppDatabase) = db.taskCheckinDao()
    @Provides fun provideCategoryDao(db: AppDatabase) = db.categoryDao()
    @Provides fun provideNoteDao(db: AppDatabase) = db.noteDao()
    @Provides fun provideSportRecordDao(db: AppDatabase) = db.sportRecordDao()
    @Provides fun provideEnglishWordDao(db: AppDatabase) = db.englishWordDao()
    @Provides fun provideMovieBookDao(db: AppDatabase) = db.movieBookDao()
    @Provides fun provideHealthRecordDao(db: AppDatabase) = db.healthRecordDao()
    @Provides fun provideAccountRecordDao(db: AppDatabase) = db.accountRecordDao()
    @Provides fun providePomodoroSessionDao(db: AppDatabase) = db.pomodoroSessionDao()
}
