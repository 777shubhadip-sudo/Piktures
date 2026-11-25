package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.RelockTimeout
import com.example.domain.model.ThemeMode
import com.example.domain.model.UiColorPalette
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lumina_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val UI_COLOR_PALETTE = stringPreferencesKey("ui_color_palette")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val TRASH_RETENTION_DAYS = intPreferencesKey("trash_retention_days")
        val RELOCK_TIMEOUT = stringPreferencesKey("relock_timeout")
        val SEEN_EDITOR_TIPS = stringSetPreferencesKey("seen_editor_tips")
        val SKIP_ALL_EDITOR_TIPS = booleanPreferencesKey("skip_all_editor_tips")
        val REDUCE_TRANSPARENCY = booleanPreferencesKey("reduce_transparency")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let { name ->
            ThemeMode.entries.firstOrNull { it.name == name }
        } ?: ThemeMode.LIGHT

        val colorPalette = prefs[Keys.UI_COLOR_PALETTE]?.let { name ->
            UiColorPalette.entries.firstOrNull { it.name == name }
        } ?: UiColorPalette.ORCHID_MAUVE

        val columns = (prefs[Keys.GRID_COLUMNS] ?: 3).coerceIn(2, 5)
        val trashDays = prefs[Keys.TRASH_RETENTION_DAYS] ?: 30
        val timeout = prefs[Keys.RELOCK_TIMEOUT]?.let { name ->
            RelockTimeout.entries.firstOrNull { it.name == name }
        } ?: RelockTimeout.IMMEDIATE
        val reduceTransparency = prefs[Keys.REDUCE_TRANSPARENCY] ?: false

        AppSettings(
            themeMode = theme,
            uiColorPalette = colorPalette,
            gridColumns = columns,
            trashRetentionDays = trashDays,
            relockTimeout = timeout,
            reduceTransparency = reduceTransparency
        )
    }

    val seenEditorTipsFlow: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.SEEN_EDITOR_TIPS] ?: emptySet()
    }

    val skipAllEditorTipsFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.SKIP_ALL_EDITOR_TIPS] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUiColorPalette(palette: UiColorPalette) {
        context.settingsDataStore.edit { it[Keys.UI_COLOR_PALETTE] = palette.name }
    }

    suspend fun setGridColumns(columns: Int) {
        context.settingsDataStore.edit { it[Keys.GRID_COLUMNS] = columns.coerceIn(2, 5) }
    }

    suspend fun setTrashRetentionDays(days: Int) {
        context.settingsDataStore.edit { it[Keys.TRASH_RETENTION_DAYS] = days }
    }

    suspend fun setRelockTimeout(timeout: RelockTimeout) {
        context.settingsDataStore.edit { it[Keys.RELOCK_TIMEOUT] = timeout.name }
    }

    suspend fun setReduceTransparency(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REDUCE_TRANSPARENCY] = enabled }
    }

    suspend fun markEditorTipSeen(tipKey: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.SEEN_EDITOR_TIPS] ?: emptySet()
            prefs[Keys.SEEN_EDITOR_TIPS] = current + tipKey
        }
    }

    suspend fun skipAllEditorTips() {
        context.settingsDataStore.edit { it[Keys.SKIP_ALL_EDITOR_TIPS] = true }
    }
}
