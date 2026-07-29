package com.jencao.mywork.ui.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.ExpressPackageEntity
import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.ExpressTrackData
import com.jencao.mywork.data.remote.model.ExpressTrackRequest
import com.jencao.mywork.data.repository.ExpressPackageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpressViewModel @Inject constructor(
    private val repo: ExpressPackageRepository,
    private val api: ApiService
) : ViewModel() {
    val packages: StateFlow<List<ExpressPackageEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(company: String, companyName: String, trackingNo: String, goods: String) = viewModelScope.launch {
        if (trackingNo.isBlank()) return@launch
        repo.insert(ExpressPackageEntity(company = company, companyName = companyName, trackingNo = trackingNo, goods = goods))
    }
    fun delete(id: String) = viewModelScope.launch { repo.softDelete(id) }

    /** 实时查询物流（密钥在服务端后台管理），成功后回写本地状态 */
    fun track(id: String, onResult: (ExpressTrackData?, String) -> Unit) = viewModelScope.launch {
        val p = repo.getById(id) ?: return@launch onResult(null, "未找到包裹")
        try {
            val r = api.expressTrack(ExpressTrackRequest(company = p.company, tracking_no = p.trackingNo))
            if (r.code == 0 && r.data != null) {
                p.currentStatus = r.data.status
                p.lastUpdate = System.currentTimeMillis()
                repo.update(p)
                onResult(r.data, "")
            } else onResult(null, r.message.ifBlank { "查询失败" })
        } catch (e: Exception) {
            onResult(null, "网络错误：${e.message}")
        }
    }
}
