package com.jencao.mywork.reminder

import com.jencao.mywork.data.repository.HealthRecordRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 供非 Hilt 托管的组件（如 BroadcastReceiver）获取仓储的入口点。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun healthRepo(): HealthRecordRepository
}
