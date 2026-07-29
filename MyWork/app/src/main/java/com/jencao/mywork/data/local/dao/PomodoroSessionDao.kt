package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jencao.mywork.data.local.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroSessionDao {
    @Query("SELECT * FROM pomodoro_sessions WHERE is_deleted = 0 ORDER BY completed_at DESC LIMIT 50")
    fun observeAll(): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE is_deleted = 0 AND mode = 'work'")
    fun observeWorkCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PomodoroSessionEntity)

    @Query("SELECT * FROM pomodoro_sessions WHERE is_deleted = 0 AND (mode LIKE '%' || :kw || '%' OR note LIKE '%' || :kw || '%') ORDER BY completed_at DESC LIMIT 50")
    suspend fun search(kw: String): List<PomodoroSessionEntity>
}
