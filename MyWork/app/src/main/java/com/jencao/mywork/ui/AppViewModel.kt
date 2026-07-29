package com.jencao.mywork.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.settings.ModuleKey
import com.jencao.mywork.data.settings.ThemeMode
import com.jencao.mywork.data.settings.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用级 ViewModel（Activity 作用域），托管主题模式、板块开关、设备标识。
 * 这些偏好被根可组合用于切换主题与决定底部导航项，并被首页/设置页复用。
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM
    )

    val moduleToggles: StateFlow<Map<ModuleKey, Boolean>> = prefs.moduleToggles.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
    )

    val deviceId: StateFlow<String> = prefs.deviceId.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }

    fun toggleModule(key: ModuleKey, enabled: Boolean) =
        viewModelScope.launch { prefs.setModuleEnabled(key, enabled) }
}
