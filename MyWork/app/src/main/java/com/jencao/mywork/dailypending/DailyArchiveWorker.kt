package com.jencao.mywork.dailypending

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jencao.mywork.data.repository.DailyPendingRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * 每日归档 Worker：把「截止日期已过且未完成」的任务快照进每日作业清单。
 * 由 WorkManager 在每天 00:05 左右调度；应用启动时也会兜底执行一次。
 * 归档基于确定性主键幂等，重复执行无副作用；服务端 Cron 亦做同样归档，双端收敛。
 *
 * 不使用 @HiltWorker 注入，改为在 doWork 内通过 Hilt EntryPoint 取仓库，
 * 以避免依赖 Hilt 生成的 Worker 绑定（否则缺绑定时 WorkManager 默认工厂
 * 无法反射构造 (Context, WorkerParameters) 而抛 NoSuchMethodException）。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyArchiveWorkerEntryPoint {
    fun dailyPendingRepository(): DailyPendingRepository
}

class DailyArchiveWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (runAttemptCount > 3) return Result.failure()
        return try {
            val repo = EntryPointAccessors.fromApplication(
                applicationContext, DailyArchiveWorkerEntryPoint::class.java
            ).dailyPendingRepository()
            repo.archiveOverdueTasks()
            Result.success()
        } catch (e: Throwable) {
            Result.retry()
        }
    }
}
