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

package de.lifemodule.app.data.health

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/**
 * Fallback [ActivityResultContract] used when the Health Connect SDK is not
 * available on the device.  It accepts any input and always returns an empty
 * set, so Compose's `rememberLauncherForActivityResult` can be registered
 * unconditionally without crashing.
 */
class NoOpPermissionContract : ActivityResultContract<Set<String>, Set<String>>() {
    override fun createIntent(context: Context, input: Set<String>) =
        Intent(Intent.ACTION_VIEW) // never resolves; launch() is a silent no-op
    override fun parseResult(resultCode: Int, intent: Intent?) = emptySet<String>()
}
