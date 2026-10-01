package com.prompi.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.res.Resources
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prompi.app.BuildConfig
import com.prompi.app.R
import com.prompi.app.domain.model.ImportMode
import com.prompi.app.domain.model.ParsedBackup
import com.prompi.app.domain.model.ThemeMode
import com.prompi.app.ui.common.AppViewModelProvider
import com.prompi.app.ui.components.BackButton
import com.prompi.app.ui.components.MaxContentWidth
import com.prompi.app.ui.components.RadioRow
import com.prompi.app.ui.components.ScrollableDialogContent
import com.prompi.app.ui.components.VerticalSpace
import com.prompi.app.ui.components.quantityString
import com.prompi.app.util.defaultBackupFileName
import kotlinx.coroutines.launch

/** Tipos MIME aceptados al importar: algunos gestores de archivos no reconocen .json. */
private val ImportMimeTypes = arrayOf("application/json", "text/plain", "application/octet-stream")

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.export(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.readImportFile(uri) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val text = event.toText(context.resources)
            // Lanzado aparte para no bloquear la recogida de eventos mientras se ve el Snackbar.
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(text, withDismissAction = true, duration = SnackbarDuration.Long)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = { BackButton(onBack) },
                    scrollBehavior = scrollBehavior,
                )
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
            ) {
                SectionTitle(stringResource(R.string.settings_section_appearance))
                Column(Modifier.padding(horizontal = 16.dp)) {
                    ThemeOption(ThemeMode.SYSTEM, R.string.theme_system, R.string.theme_system_desc, themeMode, viewModel::setThemeMode)
                    ThemeOption(ThemeMode.LIGHT, R.string.theme_light, null, themeMode, viewModel::setThemeMode)
                    ThemeOption(ThemeMode.DARK, R.string.theme_dark, null, themeMode, viewModel::setThemeMode)
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SectionTitle(stringResource(R.string.settings_section_backup))
                SettingsAction(
                    icon = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
                    title = stringResource(R.string.settings_export),
                    description = stringResource(R.string.settings_export_desc),
                    enabled = !busy,
                    onClick = {
                        try {
                            exportLauncher.launch(defaultBackupFileName())
                        } catch (e: ActivityNotFoundException) {
                            viewModel.reportError(R.string.error_no_file_app)
                        }
                    },
                )
                SettingsAction(
                    icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                    title = stringResource(R.string.settings_import),
                    description = stringResource(R.string.settings_import_desc),
                    enabled = !busy,
                    onClick = {
                        try {
                            importLauncher.launch(ImportMimeTypes)
                        } catch (e: ActivityNotFoundException) {
                            viewModel.reportError(R.string.error_no_file_app)
                        }
                    },
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SectionTitle(stringResource(R.string.settings_section_about))
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = { Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)) },
                )
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_privacy_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_privacy_text)) },
                )
            }
        }
    }

    pendingImport?.let { backup ->
        ImportConfirmDialog(
            backup = backup,
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::cancelImport,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    labelRes: Int,
    descriptionRes: Int?,
    current: ThemeMode?,
    onSelect: (ThemeMode) -> Unit,
) {
    RadioRow(
        selected = current == mode,
        text = stringResource(labelRes),
        supportingText = descriptionRes?.let { stringResource(it) },
        onClick = { onSelect(mode) },
    )
}

@Composable
private fun SettingsAction(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        leadingContent = icon,
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
    )
}

@Composable
private fun ImportConfirmDialog(
    backup: ParsedBackup,
    onConfirm: (ImportMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(ImportMode.MERGE) }
    val categoryCount = backup.categories.size
    val cardCount = backup.cardCount

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
        title = { Text(stringResource(R.string.dialog_import_title)) },
        text = {
            ScrollableDialogContent {
                Text(
                    stringResource(
                        R.string.dialog_import_summary,
                        quantityString(R.plurals.category_count, categoryCount, categoryCount),
                        quantityString(R.plurals.card_count, cardCount, cardCount),
                    ),
                )
                if (backup.adjustedValues > 0) {
                    VerticalSpace(8.dp)
                    Text(
                        quantityString(R.plurals.dialog_import_adjusted, backup.adjustedValues, backup.adjustedValues),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                VerticalSpace(12.dp)
                RadioRow(
                    selected = mode == ImportMode.MERGE,
                    text = stringResource(R.string.import_mode_merge),
                    supportingText = stringResource(R.string.import_mode_merge_desc),
                    onClick = { mode = ImportMode.MERGE },
                )
                RadioRow(
                    selected = mode == ImportMode.REPLACE,
                    text = stringResource(R.string.import_mode_replace),
                    supportingText = stringResource(R.string.import_mode_replace_desc),
                    onClick = { mode = ImportMode.REPLACE },
                    destructive = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(mode) }) {
                Text(
                    stringResource(if (mode == ImportMode.REPLACE) R.string.action_replace else R.string.action_import),
                    color = if (mode == ImportMode.REPLACE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun SettingsEvent.toText(res: Resources): String = when (this) {
    is SettingsEvent.ExportDone -> res.getQuantityString(R.plurals.msg_export_done, cardCount, cardCount)
    is SettingsEvent.Error -> res.getString(messageRes)
    is SettingsEvent.ImportDone -> buildList {
        add(res.getQuantityString(R.plurals.msg_import_done, result.cardsImported, result.cardsImported))
        if (result.duplicatesSkipped > 0) {
            add(res.getQuantityString(R.plurals.msg_import_duplicates, result.duplicatesSkipped, result.duplicatesSkipped))
        }
    }.joinToString(" · ")
}
