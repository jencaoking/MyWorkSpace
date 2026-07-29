package com.jencao.mywork.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.jencao.mywork.data.local.dao.*
import com.jencao.mywork.data.local.entity.*

@Database(
    entities = [
        TaskEntity::class,
        TaskCheckinEntity::class,
        CategoryEntity::class,
        NoteEntity::class,
        NoteFtsEntity::class,
        SportRecordEntity::class,
        EnglishWordEntity::class,
        MovieBookEntity::class,
        HealthRecordEntity::class,
        AccountRecordEntity::class,
        PomodoroSessionEntity::class
    ],
    version = 6,
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
    abstract fun pomodoroSessionDao(): PomodoroSessionDao

    companion object {
        const val DATABASE_NAME = "mywork.db"

        /** 版本 5 -> 6：英语单词新增跟读录音本地路径字段。 */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE english_words ADD COLUMN audio_path TEXT")
            }
        }

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
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
