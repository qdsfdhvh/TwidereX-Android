/*
 *  Twidere X
 *
 *  Copyright (C) 2020-2021 Tlaster <tlaster@outlook.com>
 * 
 *  This file is part of Twidere X.
 * 
 *  Twidere X is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  Twidere X is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Twidere X. If not, see <http://www.gnu.org/licenses/>.
 */
package com.twidere.twiderex.repository

import coil.ImageLoader
import com.twidere.twiderex.db.AppDatabase
import com.twidere.twiderex.db.CacheDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.Cache
import java.io.File

class CacheRepository(
    private val database: CacheDatabase,
    private val cache: Cache,
    private val imageLoader: ImageLoader,
    private val appDatabase: AppDatabase,
    private val cacheDirs: List<File>,
) {
    suspend fun clearDatabaseCache() = coroutineScope {
        launch(Dispatchers.IO) {
            database.clearAllTables()
        }
    }

    suspend fun clearImageCache() = coroutineScope {
        imageLoader.memoryCache.clear()
        cache.directory.deleteRecursively()
    }

    suspend fun clearCacheDir() = coroutineScope {
        launch(Dispatchers.IO) {
            cacheDirs.forEach {
                it.listFiles()?.forEach { file ->
                    file.deleteRecursively()
                }
            }
        }
    }

    suspend fun clearSearchHistory() = coroutineScope {
        launch(Dispatchers.IO) {
            appDatabase.searchDao().clear()
        }
    }
}
