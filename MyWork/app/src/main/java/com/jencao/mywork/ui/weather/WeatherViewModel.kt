package com.jencao.mywork.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.location.LocationProvider
import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.QweatherCity
import com.jencao.mywork.data.remote.model.QweatherCityData
import com.jencao.mywork.data.remote.model.QweatherDaily
import com.jencao.mywork.data.remote.model.QweatherDailyData
import com.jencao.mywork.data.remote.model.QweatherNow
import com.jencao.mywork.data.remote.model.QweatherNowData
import com.jencao.mywork.data.repository.ProxyRepository
import com.jencao.mywork.data.settings.UserPreferencesRepository
import com.jencao.mywork.data.settings.WeatherLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeatherUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val now: QweatherNow? = null,
    val daily: List<QweatherDaily> = emptyList(),
    val cityName: String = "",
    val auto: Boolean = true
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val api: ApiService,
    private val prefs: UserPreferencesRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val repo = ProxyRepository(api)

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private val _searchResults = MutableStateFlow<List<QweatherCity>>(emptyList())
    val searchResults: StateFlow<List<QweatherCity>> = _searchResults.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _locationGranted = MutableStateFlow(false)
    val locationGranted: StateFlow<Boolean> = _locationGranted.asStateFlow()

    init {
        viewModelScope.launch {
            combine(prefs.weatherAuto, prefs.weatherLocation) { auto, loc -> auto to loc }
                .collect { (auto, loc) ->
                    _state.value = _state.value.copy(
                        auto = auto,
                        cityName = loc?.name ?: if (auto) "当前位置" else ""
                    )
                    refreshInternal(auto, loc)
                }
        }
    }

    fun setLocationGranted(granted: Boolean) {
        _locationGranted.value = granted
        if (granted) refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val auto = prefs.weatherAuto.first()
            val loc = prefs.weatherLocation.first()
            refreshInternal(auto, loc)
        }
    }

    private suspend fun refreshInternal(auto: Boolean, loc: WeatherLocation?) {
        _state.value = _state.value.copy(loading = true, error = null)
        val locationStr = if (auto) {
            if (_locationGranted.value) {
                val l = locationProvider.getLastLocation() ?: locationProvider.requestCurrentLocation()
                if (l != null) {
                    String.format("%.4f", l.longitude) + "," + String.format("%.4f", l.latitude)
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            loc?.id
        }

        if (locationStr == null) {
            _state.value = _state.value.copy(
                loading = false,
                error = if (auto) "未开启定位权限，点击选择城市" else "请先在天气卡片选择城市"
            )
            return
        }
        val nowRes = repo.getWeatherNow(locationStr)
        val dailyRes = repo.getWeather7d(locationStr)
        if (nowRes.isFailure) {
            _state.value = _state.value.copy(
                loading = false,
                error = nowRes.exceptionOrNull()?.message ?: "天气获取失败"
            )
            return
        }
        _state.value = _state.value.copy(
            loading = false,
            now = nowRes.getOrNull()?.now,
            daily = dailyRes.getOrNull()?.daily ?: emptyList(),
            cityName = if (auto) "当前位置" else (loc?.name ?: ""),
            error = null
        )
    }

    fun searchCity(keyword: String) {
        viewModelScope.launch {
            if (keyword.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            _searching.value = true
            val res = repo.lookupCity(keyword)
            _searchResults.value = if (res.isSuccess) res.getOrNull()?.location ?: emptyList() else emptyList()
            _searching.value = false
        }
    }

    fun selectCity(city: QweatherCity) {
        viewModelScope.launch {
            prefs.setWeatherAuto(false)
            prefs.setWeatherLocation(city.id, city.name)
        }
        _searchResults.value = emptyList()
        refresh()
    }

    fun enableAutoLocation() {
        viewModelScope.launch { prefs.setWeatherAuto(true) }
        refresh()
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }
}
