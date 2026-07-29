package com.jencao.mywork.dailypending

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jencao.mywork.data.repository.DailyPendingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 每日归档 Worker：把「截止日期已过且未完成」的任务快照进每日作业清单。
 * 由 WorkManager 在每天 00:05 左右调度；应用启动时也会兜底执行一次。
 * 归档基于确定性主键幂等，重复执行无副作用；服务端 Cron 亦做同样归档，双端收敛。
 */
@HiltWorker
class DailyArchiveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repo: DailyPendingRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (runAttemptCount > 3) return Result.failure()
        return try {
            repo.archiveOverdueTasks()
            Result.success()
        } catch (e: Throwable) {
            Result.retry()
        }
    }
}
