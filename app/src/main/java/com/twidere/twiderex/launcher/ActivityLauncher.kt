/*
 *  TwidereX
 *
 *  Copyright (C) 2020 Tlaster <tlaster@outlook.com>
 * 
 *  This file is part of TwidereX.
 * 
 *  TwidereX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  TwidereX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with TwidereX. If not, see <http://www.gnu.org/licenses/>.
 */
package com.twidere.twiderex.launcher

import android.net.Uri
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.runtime.ambientOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ActivityLauncher(registry: ActivityResultRegistry) : DefaultLifecycleObserver {
    private val multipleFilePickerLauncher = MultipleFilePickerLauncher(registry)
    private val requestMultiplePermissionsLauncher = RequestMultiplePermissionsLauncher(registry)
    private lateinit var owner: LifecycleOwner

    override fun onCreate(owner: LifecycleOwner) {
        this.owner = owner
        multipleFilePickerLauncher.register(owner)
        requestMultiplePermissionsLauncher.register(owner)
    }

    suspend fun launchMultipleFilePicker(type: String): List<Uri> {
        return multipleFilePickerLauncher.launch(type)
    }

    suspend fun requestMultiplePermissions(permissions: Array<String>): Map<String, Boolean> {
        return requestMultiplePermissionsLauncher.launch(permissions)
    }
}

val AmbientLauncher = ambientOf<ActivityLauncher>()