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
        PomodoroSessionEntity::class,
        DailyPendingLogEntity::class
    ],
    version = 10,
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
    abstract fun dailyPendingLogDao(): DailyPendingLogDao

    companion object {
        const val DATABASE_NAME = "mywork.db"

        /** 版本 5 -> 6：英语单词新增跟读录音本地路径字段。 */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE english_words ADD COLUMN audio_path TEXT")
            }
        }

        /** 版本 6 -> 7：影音书籍新增 TMDB 详情字段（原始名 / 简介 / 上映日期 / 评分）。 */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movie_books ADD COLUMN original_title TEXT")
                db.execSQL("ALTER TABLE movie_books ADD COLUMN overview TEXT")
                db.execSQL("ALTER TABLE movie_books ADD COLUMN release_date TEXT")
                db.execSQL("ALTER TABLE movie_books ADD COLUMN vote_average REAL")
            }
        }

        /** 版本 7 -> 8：健康记录新增提醒时间字段（复诊 / 用药闹钟）。 */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE health_records ADD COLUMN reminder_time INTEGER")
            }
        }

        /** 版本 9 -> 10：新增每日未完成作业归档表。 */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_pending_log` (
                        `task_id` TEXT NOT NULL,
                        `task_title` TEXT NOT NULL,
                        `category_name` TEXT NOT NULL,
                        `priority` INTEGER NOT NULL,
                        `original_due_date` INTEGER NOT NULL,
                        `log_date` TEXT NOT NULL,
                        `disposition` TEXT NOT NULL,
                        `disposed_at` INTEGER,
                        `new_due_date` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        `id` TEXT NOT NULL,
                        `last_modified` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL,
                        `device_id` TEXT NOT NULL,
                        `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_pending_log_task_id_log_date` ON `daily_pending_log` (`task_id`, `log_date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_pending_log_log_date` ON `daily_pending_log` (`log_date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_pending_log_disposition` ON `daily_pending_log` (`disposition`)")
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
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
