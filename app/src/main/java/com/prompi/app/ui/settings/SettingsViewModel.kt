package com.prompi.app.ui.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prompi.app.R
import com.prompi.app.domain.model.BackupException
import com.prompi.app.domain.model.ImportMode
import com.prompi.app.domain.model.ImportResult
import com.prompi.app.domain.model.ParsedBackup
import com.prompi.app.domain.model.ThemeMode
import com.prompi.app.domain.repository.BackupRepository
import com.prompi.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data class ExportDone(val cardCount: Int) : SettingsEvent
    data class ImportDone(val result: ImportResult) : SettingsEvent
    data class Error(@StringRes val messageRes: Int) : SettingsEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode?> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _busy = MutableStateFlow(false)

    /** `true` mientras se exporta, se lee o se importa un archivo. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _pendingImport = MutableStateFlow<ParsedBackup?>(null)

    /** Copia ya leída y validada, a la espera de que el usuario confirme cómo importarla. */
    val pendingImport: StateFlow<ParsedBackup?> = _pendingImport.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun export(uri: Uri) = runBusy {
        backupRepository.export(uri).fold(
            onSuccess = { _events.send(SettingsEvent.ExportDone(it)) },
            onFailure = { _events.send(SettingsEvent.Error(it.toMessageRes())) },
        )
    }

    fun readImportFile(uri: Uri) = runBusy {
        backupRepository.read(uri).fold(
            onSuccess = { backup ->
                if (backup.categories.isEmpty()) {
                    _events.send(SettingsEvent.Error(R.string.error_backup_empty))
                } else {
                    _pendingImport.value = backup
                }
            },
            onFailure = { _events.send(SettingsEvent.Error(it.toMessageRes())) },
        )
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun confirmImport(mode: ImportMode) {
        val backup = _pendingImport.value ?: return
        _pendingImport.value = null
        runBusy {
            try {
                _events.send(SettingsEvent.ImportDone(backupRepository.import(backup, mode)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // La transacción se revierte: los datos anteriores quedan intactos.
                _events.send(SettingsEvent.Error(R.string.error_import_failed))
            }
        }
    }

    fun reportError(@StringRes messageRes: Int) {
        viewModelScope.launch { _events.send(SettingsEvent.Error(messageRes)) }
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }
}

@StringRes
private fun Throwable.toMessageRes(): Int = when (this) {
    is BackupException.InvalidFile -> R.string.error_invalid_file
    is BackupException.NotPrompiFile -> R.string.error_not_prompi_file
    is BackupException.UnsupportedVersion -> R.string.error_unsupported_version
    is BackupException.TooLarge -> R.string.error_file_too_large
    is BackupException.ReadError -> R.string.error_read_file
    is BackupException.WriteError -> R.string.error_write_file
    else -> R.string.error_generic
}
