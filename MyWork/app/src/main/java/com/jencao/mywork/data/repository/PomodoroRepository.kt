package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.PomodoroSessionDao
import com.jencao.mywork.data.local.entity.PomodoroSessionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroRepository @Inject constructor(
    private val dao: PomodoroSessionDao
) {
    fun observeSessions() = dao.observeAll()
    fun observeWorkCount() = dao.observeWorkCount()

    suspend fun saveSession(mode: String, durationMin: Int) {
        val item = PomodoroSessionEntity(mode = mode, durationMin = durationMin)
        item.touch()
        dao.insert(item)
    }
}
