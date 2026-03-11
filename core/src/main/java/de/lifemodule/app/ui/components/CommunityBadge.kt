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

package de.lifemodule.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lifemodule.app.core.R
import de.lifemodule.app.data.ImportSource
import de.lifemodule.app.ui.theme.LocalAccentColor

/**
 * Small accent-coloured pill badge displaying "Community".
 *
 * Show this next to the entity name whenever `importSource == COMMUNITY_HUB`.
 * Example:
 * ```
 *   Row {
 *       Text(entity.name)
 *       if (entity.importSource == ImportSource.COMMUNITY_HUB) {
 *           CommunityBadge()
 *       }
 *   }
 * ```
 */
@Composable
fun CommunityBadge(modifier: Modifier = Modifier) {
    val accent = LocalAccentColor.current
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = stringResource(R.string.core_community_badge),
        modifier = modifier
            .clip(shape)
            .border(1.dp, accent, shape)
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = accent
    )
}
