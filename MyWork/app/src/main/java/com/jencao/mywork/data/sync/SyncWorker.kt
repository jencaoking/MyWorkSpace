package com.jencao.mywork.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jencao.mywork.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 后台同步 Worker：由 WorkManager 调度，周期性或按需执行一次 [SyncManager.syncOnce]。
 * 通过 @HiltWorker 注入单例的 SyncManager，复用既有的双向增量同步闭环。
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 连续失败超过 3 次不再无限重试，交由下一个周期重新触发
        if (runAttemptCount > 3) return Result.failure()
        return try {
            syncManager.syncOnce().getOrThrow()
            Result.success()
        } catch (e: Throwable) {
            Result.retry()
        }
    }
}
