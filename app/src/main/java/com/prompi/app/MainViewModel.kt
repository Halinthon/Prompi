package com.prompi.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prompi.app.domain.model.ThemeMode
import com.prompi.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Expone el tema elegido. Es `null` hasta leer DataStore, para no mostrar un tema incorrecto. */
class MainViewModel(settingsRepository: SettingsRepository) : ViewModel() {
    val themeMode: StateFlow<ThemeMode?> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
