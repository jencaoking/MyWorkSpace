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
        DailyPendingLogEntity::class,
        CalcHistoryEntity::class,
        QrScanEntity::class,
        CountdownEntity::class,
        HabitPlanEntity::class,
        HabitEntity::class,
        HabitCheckinEntity::class,
        FlashcardDeckEntity::class,
        FlashcardEntity::class,
        InspirationEntity::class,
        ExpressPackageEntity::class
    ],
    version = 11,
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
    abstract fun calcHistoryDao(): CalcHistoryDao
    abstract fun qrScanDao(): QrScanDao
    abstract fun countdownDao(): CountdownDao
    abstract fun habitPlanDao(): HabitPlanDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCheckinDao(): HabitCheckinDao
    abstract fun flashcardDeckDao(): FlashcardDeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun inspirationDao(): InspirationDao
    abstract fun expressPackageDao(): ExpressPackageDao

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

        /** 版本 10 -> 11：新增工具箱 8 模块共 10 张表（计算器/扫码/倒计时/习惯/闪卡/灵感/快递）。 */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `calc_history` (
                        `id` TEXT NOT NULL, `expr` TEXT NOT NULL, `result` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL, `last_modified` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL, `device_id` TEXT NOT NULL, `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calc_history_created_at` ON `calc_history` (`created_at`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `qr_scan_history` (
                        `id` TEXT NOT NULL, `content` TEXT NOT NULL, `format` TEXT NOT NULL, `note` TEXT,
                        `created_at` INTEGER NOT NULL, `last_modified` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL, `device_id` TEXT NOT NULL, `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_qr_scan_history_created_at` ON `qr_scan_history` (`created_at`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `countdown_events` (
                        `id` TEXT NOT NULL, `title` TEXT NOT NULL, `target_time` INTEGER NOT NULL, `remark` TEXT,
                        `created_at` INTEGER NOT NULL, `last_modified` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL, `device_id` TEXT NOT NULL, `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_countdown_events_target_time` ON `countdown_events` (`target_time`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `habit_plans` (
                        `id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `period` INTEGER NOT NULL,
                        `start_date` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `last_modified` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL, `device_id` TEXT NOT NULL, `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_plans_start_date` ON `habit_plans` (`start_date`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `habits` (
                        `id` TEXT NOT NULL, `plan_id` TEXT NOT NULL, `title` TEXT NOT NULL, `frequency` INTEGER NOT NULL,
                        `days` TEXT, `time_min` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `last_modified` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL, `device_id` TEXT NOT NULL, `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_plan_id` ON `habits` (`plan_id`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `habit_checkins` (
                        `id` TEXT NOT NULL, `habit_id` TEXT NOT NULL, `date` TEXT NOT NULL, `created_at` INTEGER NOT NULL,
                        `last_modified` INTEGER NOT NULL, `is_deleted` INTEGER NOT NULL, `device_id` TEXT NOT NULL, `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_checkins_habit_id` ON `habit_checkins` (`habit_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_checkins_date` ON `habit_checkins` (`date`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `flashcard_decks` (
                        `id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `created_at` INTEGER NOT NULL,
                        `last_modified` INTEGER NOT NULL, `is_deleted` INTEGER NOT NULL, `device_id` TEXT NOT NULL, `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_decks_created_at` ON `flashcard_decks` (`created_at`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `flashcards` (
                        `id` TEXT NOT NULL, `deck_id` TEXT NOT NULL, `front` TEXT NOT NULL, `back` TEXT NOT NULL,
                        `next_review` INTEGER NOT NULL, `interval_days` INTEGER NOT NULL, `ease` REAL NOT NULL,
                        `created_at` INTEGER NOT NULL, `last_modified` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL, `device_id` TEXT NOT NULL, `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_deck_id` ON `flashcards` (`deck_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcards_next_review` ON `flashcards` (`next_review`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `inspiration_items` (
                        `id` TEXT NOT NULL, `content` TEXT NOT NULL, `author` TEXT, `source` TEXT, `tags` TEXT,
                        `favorite` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `last_modified` INTEGER NOT NULL,
                        `is_deleted` INTEGER NOT NULL, `device_id` TEXT NOT NULL, `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inspiration_items_created_at` ON `inspiration_items` (`created_at`)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `express_packages` (
                        `id` TEXT NOT NULL, `company` TEXT NOT NULL, `company_name` TEXT NOT NULL, `tracking_no` TEXT NOT NULL,
                        `goods` TEXT, `current_status` TEXT, `last_update` INTEGER, `created_at` INTEGER NOT NULL,
                        `last_modified` INTEGER NOT NULL, `is_deleted` INTEGER NOT NULL, `device_id` TEXT NOT NULL, `needs_sync` INTEGER NOT NULL,
                        PRIMARY KEY(`id`))"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_express_packages_tracking_no` ON `express_packages` (`tracking_no`)")
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
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_9_10, MIGRATION_10_11)
                    .build()
                INSTANCE = instance
                instance
            }
    }
}
