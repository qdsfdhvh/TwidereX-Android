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
package com.twidere.twiderex.paging.crud

import androidx.paging.PagingSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class MemoryCachePagingSourceTest {
    private val pagingMemoryCache = PagingMemoryCache<String>()

    @Test
    fun load_loadFromMemoryCache() = runBlocking {
        pagingMemoryCache.insert(listOf("1", "2", "3", "4"))
        val source = MemoryCachePagingSource(pagingMemoryCache)
        var result = source.load(PagingSource.LoadParams.Refresh(null, 2, placeholdersEnabled = false)) as PagingSource.LoadResult.Page
        assert(result.data.isNotEmpty())
        Assert.assertEquals(1, result.nextKey)

        result = source.load(PagingSource.LoadParams.Append(result.nextKey!!, 2, placeholdersEnabled = false)) as PagingSource.LoadResult.Page
        assert(result.data.isNotEmpty())
        Assert.assertEquals(2, result.nextKey)

        result = source.load(PagingSource.LoadParams.Append(result.nextKey!!, 2, placeholdersEnabled = false)) as PagingSource.LoadResult.Page
        assert(result.data.isEmpty())
        Assert.assertEquals(null, result.nextKey)
    }
}
