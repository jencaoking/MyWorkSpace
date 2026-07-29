package com.jencao.mywork.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.jencao.mywork.data.local.dao.*
import com.jencao.mywork.data.local.entity.*

@Database(
    entities = [
        TaskEntity::class,
        TaskCheckinEntity::class,
        CategoryEntity::class,
        NoteEntity::class,
        SportRecordEntity::class,
        EnglishWordEntity::class,
        MovieBookEntity::class,
        HealthRecordEntity::class,
        AccountRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskCheckinDao(): TaskCheckinDao
    abstract fun categoryDao(): CategoryDao
    abstract fun noteDao(): NoteDao
    abstract fun sportRecordDao(): SportRecordDao
    abstract fun englishWordDao(): EnglishWordDao
    abstract fun movieBookDao(): MovieBookDao
    abstract fun healthRecordDao(): HealthRecordDao
    abstract fun accountRecordDao(): AccountRecordDao

    companion object {
        const val DATABASE_NAME = "mywork.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // 阶段1 开发期：结构变更直接重建，避免手动 Migration
                    .fallbackToDestructiveMigration(dropAllTablesOnMigration = true)
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
