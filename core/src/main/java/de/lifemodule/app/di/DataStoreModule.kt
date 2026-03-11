/*
 * LifeModule — A modular, privacy-focused life tracking app for Android.
 * Copyright (C) 2026 Paul Bernhard Colin Witzke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.lifemodule.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

// Top-level DataStore delegate properties - safe for multi-process because
// we use preferencesDataStore (single-process, file-backed).
private val Context.themeDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "app_theme_v1")

private val Context.modulesDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "module_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * Provides the DataStore used by [de.lifemodule.app.ui.theme.AppThemePreferences].
     * File stored at: `files/datastore/app_theme_v1.preferences_pb`
     */
    @Provides
    @Singleton
    @Named("theme")
    fun provideThemeDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.themeDataStore

    /**
     * Provides the DataStore used by [de.lifemodule.app.ui.dashboard.ModulePreferences].
     * File stored at: `files/datastore/module_prefs.preferences_pb`
     */
    @Provides
    @Singleton
    @Named("modules")
    fun provideModulesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.modulesDataStore
}
