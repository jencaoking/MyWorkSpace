package com.jencao.mywork.data.sync

import com.jencao.mywork.data.local.BaseEntity

/**
 * 可同步仓库的统一契约：上传（待同步项）、删除（待删墓碑）、LWW 合并、标记已同步、回收墓碑。
 * 由 SyncManager 在自动同步流程中统一驱动，覆盖所有业务模块，实现“无令牌、全自动”的设备级同步。
 */
interface Syncer<T : BaseEntity> {
    suspend fun getPendingUploads(): List<T>
    suspend fun getPendingDeletions(): List<String>
    suspend fun mergeRemote(remote: List<T>)
    suspend fun markSynced(ids: List<String>)
    suspend fun purgeDeleted(ids: List<String>)
}
