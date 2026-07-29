package com.jencao.mywork.data.sync

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * 同步调度器：基于 WorkManager 管理自动同步。
 * - 周期任务：每 15 分钟（WorkManager 最小间隔）在联网时执行一次增量同步；
 * - 即时任务：供「立即同步」与 App 启动时触发；
 * - observeState：聚合周期与即时任务状态，供首页展示“同步中 / 失败”。
 *
 * 状态观察使用 work-runtime 自带的 getWorkInfosForUniqueWorkLiveData，
 * 经 callbackFlow 桥接为 Flow，避免依赖 work-runtime-ktx 的协程扩展。
 */
object SyncScheduler {
    private const val PERIODIC_ID = "auto_sync_periodic"
    const val ONETIME_ID = "auto_sync_now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** 注册周期性后台同步（幂等，重复调用仅更新已有任务） */
    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_ID, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    /** 关闭周期性后台同步 */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_ID)
    }

    /** 入队一次即时同步（替换上一次未执行的即时任务） */
    fun syncNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONETIME_ID, ExistingWorkPolicy.REPLACE, req)
    }

    /** 观察同步运行状态：是否同步中、上一次是否失败 */
    fun observeState(context: Context): Flow<SyncObserved> = callbackFlow {
        val wm = WorkManager.getInstance(context)
        val latest = mutableMapOf<String, List<WorkInfo>>()
        fun publish() {
            val all =
                (latest[PERIODIC_ID] ?: emptyList()) + (latest[ONETIME_ID] ?: emptyList())
            trySend(
                SyncObserved(
                    running = all.any { it.state == WorkInfo.State.RUNNING },
                    failed = all.any { it.state == WorkInfo.State.FAILED }
                )
            )
        }
        val obsPeriodic = Observer<List<WorkInfo>> { latest[PERIODIC_ID] = it; publish() }
        val obsOneTime = Observer<List<WorkInfo>> { latest[ONETIME_ID] = it; publish() }
        val livePeriodic = wm.getWorkInfosForUniqueWorkLiveData(PERIODIC_ID)
        val liveOneTime = wm.getWorkInfosForUniqueWorkLiveData(ONETIME_ID)
        livePeriodic.observeForever(obsPeriodic)
        liveOneTime.observeForever(obsOneTime)
        awaitClose {
            livePeriodic.removeObserver(obsPeriodic)
            liveOneTime.removeObserver(obsOneTime)
        }
    }

    /** 仅观察是否正在同步（供按钮禁用等场景） */
    fun observeRunning(context: Context): Flow<Boolean> = observeState(context).map { it.running }

    data class SyncObserved(val running: Boolean, val failed: Boolean)
}
