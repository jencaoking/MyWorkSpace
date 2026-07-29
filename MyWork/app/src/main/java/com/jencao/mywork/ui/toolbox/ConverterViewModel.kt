package com.jencao.mywork.ui.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.CurrencyRateResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnitDef(val name: String, val factor: Double = 1.0) // factor 相对基类（温度用特殊公式）
data class ConvCategory(val name: String, val units: List<UnitDef>, val isTemp: Boolean = false, val isCurrency: Boolean = false)

@HiltViewModel
class ConverterViewModel @Inject constructor(private val api: ApiService) : ViewModel() {
    val categories = listOf(
        ConvCategory("长度", listOf(
            UnitDef("米(m)", 1.0), UnitDef("千米(km)", 1000.0), UnitDef("厘米(cm)", 0.01),
            UnitDef("毫米(mm)", 0.001), UnitDef("英里(mi)", 1609.34), UnitDef("码(yd)", 0.9144),
            UnitDef("英尺(ft)", 0.3048), UnitDef("英寸(in)", 0.0254)
        )),
        ConvCategory("重量", listOf(
            UnitDef("千克(kg)", 1.0), UnitDef("克(g)", 0.001), UnitDef("吨(t)", 1000.0),
            UnitDef("磅(lb)", 0.453592), UnitDef("盎司(oz)", 0.0283495)
        )),
        ConvCategory("面积", listOf(
            UnitDef("平方米(m²)", 1.0), UnitDef("平方千米(km²)", 1e6), UnitDef("平方厘米(cm²)", 1e-4),
            UnitDef("公顷(ha)", 10000.0), UnitDef("平方英尺(ft²)", 0.092903), UnitDef("英亩(acre)", 4046.86)
        )),
        ConvCategory("体积", listOf(
            UnitDef("升(L)", 1.0), UnitDef("毫升(mL)", 0.001), UnitDef("立方米(m³)", 1000.0),
            UnitDef("加仑(US gal)", 3.78541), UnitDef("夸脱(qt)", 0.946353)
        )),
        ConvCategory("温度", listOf(
            UnitDef("摄氏度(℃)"), UnitDef("华氏度(℉)"), UnitDef("开尔文(K)")
        ), isTemp = true),
        ConvCategory("时间", listOf(
            UnitDef("秒(s)", 1.0), UnitDef("分(min)", 60.0), UnitDef("时(h)", 3600.0),
            UnitDef("天", 86400.0), UnitDef("周", 604800.0)
        )),
        ConvCategory("速度", listOf(
            UnitDef("米/秒(m/s)", 1.0), UnitDef("千米/时(km/h)", 0.277778),
            UnitDef("英里/时(mph)", 0.44704), UnitDef("节(kn)", 0.514444)
        )),
        ConvCategory("数据", listOf(
            UnitDef("字节(B)", 1.0), UnitDef("KB", 1024.0), UnitDef("MB", 1048576.0),
            UnitDef("GB", 1073741824.0), UnitDef("TB", 1099511627776.0)
        )),
        ConvCategory("货币", listOf(
            UnitDef("人民币(CNY)"), UnitDef("美元(USD)"), UnitDef("欧元(EUR)"),
            UnitDef("日元(JPY)"), UnitDef("英镑(GBP)"), UnitDef("港币(HKD)")
        ), isCurrency = true)
    )

    private val _catIdx = MutableStateFlow(0)
    val catIdx = _catIdx.asStateFlow()
    private val _fromIdx = MutableStateFlow(0)
    val fromIdx = _fromIdx.asStateFlow()
    private val _toIdx = MutableStateFlow(1)
    val toIdx = _toIdx.asStateFlow()
    private val _input = MutableStateFlow("1")
    val input = _input.asStateFlow()
    private val _result = MutableStateFlow("")
    val result = _result.asStateFlow()
    private val liveRates = mutableMapOf<String, Double>()

    val current get() = categories[_catIdx.value]

    fun setCategory(i: Int) { _catIdx.value = i; _fromIdx.value = 0; _toIdx.value = if (current.units.size > 1) 1 else 0; recompute() }
    fun setFrom(i: Int) { _fromIdx.value = i; recompute() }
    fun setTo(i: Int) { _toIdx.value = i; recompute() }
    fun setInput(s: String) { _input.value = s; recompute() }

    init { recompute() }

    fun recompute() {
        val cat = current
        val v = _input.value.toDoubleOrNull() ?: return
        _result.value = if (cat.isTemp) temp(cat, _fromIdx.value, _toIdx.value, v)
        else if (cat.isCurrency) {
            val key = "${cat.units[_fromIdx.value].name}>${cat.units[_toIdx.value].name}"
            val rate = liveRates[key]
            if (rate != null) String.format("%.4f", v * rate) else staticCurrency(_fromIdx.value, _toIdx.value, v)
        } else {
            val from = cat.units[_fromIdx.value].factor
            val to = cat.units[_toIdx.value].factor
            String.format("%.6g", v * from / to)
        }
    }

    /** 刷新货币实时汇率（服务端 /api/currency/rate，密钥在后台） */
    fun refreshLive(onDone: (String) -> Unit) {
        if (!current.isCurrency) { onDone("该类别不支持实时汇率"); return }
        val f = current.units[_fromIdx.value].name
        val t = current.units[_toIdx.value].name
        viewModelScope.launch {
            try {
                val resp: CurrencyRateResponse = api.currencyRate(from = f, to = t, amount = 1.0)
                if (resp.code == 0 && resp.data != null) {
                    liveRates["$f>$t"] = resp.data.rate
                    recompute()
                    onDone(if (resp.data.cached) "已用缓存汇率" else "实时汇率已更新")
                } else onDone(resp.message.ifBlank { "获取汇率失败" })
            } catch (e: Exception) { onDone("网络错误：${e.message}") }
        }
    }

    private fun temp(cat: ConvCategory, from: Int, to: Int, v: Double): String {
        val c = when (cat.units[from].name) {
            "华氏度(℉)" -> (v - 32) * 5 / 9
            "开尔文(K)" -> v - 273.15
            else -> v
        }
        val out = when (cat.units[to].name) {
            "华氏度(℉)" -> c * 9 / 5 + 32
            "开尔文(K)" -> c + 273.15
            else -> c
        }
        return String.format("%.4g", out)
    }

    // 货币静态近似汇率（以 1 USD 计），仅在无网络/未刷新时使用
    private val fx = mapOf("美元(USD)" to 1.0, "人民币(CNY)" to 7.1, "欧元(EUR)" to 0.92, "日元(JPY)" to 150.0, "英镑(GBP)" to 0.79, "港币(HKD)" to 7.8)
    private fun staticCurrency(from: Int, to: Int, v: Double): String {
        val f = fx[current.units[from].name] ?: 1.0
        val t = fx[current.units[to].name] ?: 1.0
        return String.format("%.4f", v * f / t)
    }
}
