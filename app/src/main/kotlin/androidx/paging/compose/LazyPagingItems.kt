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
package androidx.paging.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.CombinedLoadStates
import androidx.paging.DifferCallback
import androidx.paging.ItemSnapshotList
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.NullPaddedList
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataDiffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

/**
 * The class responsible for accessing the data from a [Flow] of [PagingData].
 * In order to obtain an instance of [LazyPagingItems] use the [collectAsLazyPagingItems] extension
 * method of [Flow] with [PagingData].
 * This instance can be used by the [items] and [itemsIndexed] methods inside [LazyListScope] to
 * display data received from the [Flow] of [PagingData].
 *
 * @param T the type of value used by [PagingData].
 */
public class LazyPagingItems<T : Any> internal constructor(
    /**
     * the [Flow] object which contains a stream of [PagingData] elements.
     */
    private val flow: Flow<PagingData<T>>
) {
    private val mainDispatcher = Dispatchers.Main

    /**
     * The number of items which can be accessed.
     */
    var itemCount: Int by mutableStateOf(0)
        private set

    /**
     * Set of value holders associated with the currently (composed) indexes. Once we got a new
     * value from the repository we can just update the value in the state.
     */
    private val activeHolders = HashSet<ActiveItemValueHolder<T>>()

    @SuppressLint("RestrictedApi")
    private val differCallback: DifferCallback = object : DifferCallback {
        override fun onChanged(position: Int, count: Int) {
            if (count > 0) {
                updateValueHolders(position, count)
            }
        }

        override fun onInserted(position: Int, count: Int) {
            if (count > 0) {
                // we have to update all the items starting from this position as the insertion
                // changes the positions for all the next items
                updateValueHolders(position, pagingDataDiffer.size - position)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            if (count > 0) {
                // we have to update all the items starting from this position as the removal
                // changes the positions for all the next items. plus we also want to set null
                // for the items which are now out of the valid bounds.
                updateValueHolders(position, pagingDataDiffer.size + count - position)
            }
        }
    }

    private val pagingDataDiffer = object : PagingDataDiffer<T>(
        differCallback = differCallback,
        mainDispatcher = mainDispatcher
    ) {
        override suspend fun presentNewList(
            previousList: NullPaddedList<T>,
            newList: NullPaddedList<T>,
            newCombinedLoadStates: CombinedLoadStates,
            lastAccessedIndex: Int,
            onListPresentable: () -> Unit
        ): Int? {
            onListPresentable()
            val oldSize = itemCount
            updateValueHolders(0, maxOf(newList.size, oldSize))
            return null
        }
    }

    /**
     * Returns the state containing the item specified at [index] and notifies Paging of the item
     * accessed in order to trigger any loads necessary to fulfill [PagingConfig.prefetchDistance].
     *
     * @param index the index of the item which should be returned.
     * @return the state containing the item specified at [index] or null if the item is a
     * placeholder or [index] is not within the correct bounds.
     */
    @Composable
    fun getAsState(index: Int): State<T?> {
        require(index >= 0) { "Index can't be negative. $index was passed." }
        val holder = remember(index) {
            val initial = if (index < pagingDataDiffer.size) {
                pagingDataDiffer[index]
            } else {
                null
            }
            ActiveItemValueHolder(index, initial)
        }
        DisposableEffect(index) {
            activeHolders.add(holder)
            onDispose {
                activeHolders.remove(holder)
            }
        }
        return holder.state
    }

    private fun updateValueHolders(position: Int, count: Int) {
        itemCount = pagingDataDiffer.size
        if (count > 0) {
            activeHolders.forEach {
                if (it.index in position until position + count) {
                    val newValue = if (it.index < pagingDataDiffer.size) {
                        pagingDataDiffer[it.index]
                    } else {
                        null
                    }
                    it.state.value = newValue
                }
            }
        }
    }

    /**
     * Returns the presented item at the specified position, without notifying Paging of the item
     * access that would normally trigger page loads.
     *
     * @param index Index of the presented item to return, including placeholders.
     * @return The presented item at position [index], `null` if it is a placeholder
     */
    fun peek(index: Int): T? {
        return pagingDataDiffer.peek(index)
    }

    /**
     * Returns a new [ItemSnapshotList] representing the currently presented items, including any
     * placeholders if they are enabled.
     */
    fun snapshot(): ItemSnapshotList<T> {
        return pagingDataDiffer.snapshot()
    }

    /**
     * Retry any failed load requests that would result in a [LoadState.Error] update to this
     * [LazyPagingItems].
     *
     * Unlike [refresh], this does not invalidate [PagingSource], it only retries failed loads
     * within the same generation of [PagingData].
     *
     * [LoadState.Error] can be generated from two types of load requests:
     *  * [PagingSource.load] returning [PagingSource.LoadResult.Error]
     *  * [RemoteMediator.load] returning [RemoteMediator.MediatorResult.Error]
     */
    fun retry() {
        pagingDataDiffer.retry()
    }

    /**
     * Refresh the data presented by this [LazyPagingItems].
     *
     * [refresh] triggers the creation of a new [PagingData] with a new instance of [PagingSource]
     * to represent an updated snapshot of the backing dataset. If a [RemoteMediator] is set,
     * calling [refresh] will also trigger a call to [RemoteMediator.load] with [LoadType] [REFRESH]
     * to allow [RemoteMediator] to check for updates to the dataset backing [PagingSource].
     *
     * Note: This API is intended for UI-driven refresh signals, such as swipe-to-refresh.
     * Invalidation due repository-layer signals, such as DB-updates, should instead use
     * [PagingSource.invalidate].
     *
     * @see PagingSource.invalidate
     */
    fun refresh() {
        pagingDataDiffer.refresh()
    }

    /**
     * A [CombinedLoadStates] object which represents the current loading state.
     */
    public var loadState: CombinedLoadStates by mutableStateOf(
        CombinedLoadStates(
            refresh = InitialLoadStates.refresh,
            prepend = InitialLoadStates.prepend,
            append = InitialLoadStates.append,
            source = InitialLoadStates
        )
    )
        private set

    internal suspend fun collectLoadState() {
        pagingDataDiffer.loadStateFlow.collect {
            loadState = it
        }
    }

    internal suspend fun collectPagingData() {
        flow.collectLatest {
            pagingDataDiffer.collectFrom(it)
        }
    }

    private class ActiveItemValueHolder<T : Any>(val index: Int, initialValue: T?) {
        val state = mutableStateOf(initialValue)
    }
}

private val IncompleteLoadState = LoadState.NotLoading(false)
private val InitialLoadStates = LoadStates(
    IncompleteLoadState,
    IncompleteLoadState,
    IncompleteLoadState
)

/**
 * Collects values from this [Flow] of [PagingData] and represents them inside a [LazyPagingItems]
 * instance. The [LazyPagingItems] instance can be used by the [items] and [itemsIndexed] methods
 * from [LazyListScope] in order to display the data obtained from a [Flow] of [PagingData].
 *
 * @sample androidx.paging.compose.samples.PagingBackendSample
 */
@Composable
public fun <T : Any> Flow<PagingData<T>>.collectAsLazyPagingItems(): LazyPagingItems<T> {
    val lazyPagingItems = remember(this) { LazyPagingItems(this) }

    LaunchedEffect(lazyPagingItems) {
        lazyPagingItems.collectPagingData()
    }
    LaunchedEffect(lazyPagingItems) {
        lazyPagingItems.collectLoadState()
    }

    return lazyPagingItems
}

/**
 * Adds the [LazyPagingItems] and their content to the scope. The range from 0 (inclusive) to
 * [LazyPagingItems.itemCount] (exclusive) always represents the full range of presentable items,
 * because every event from [PagingDataDiffer] will trigger a recomposition.
 *
 * @sample androidx.paging.compose.samples.ItemsDemo
 *
 * @param lazyPagingItems the items received from a [Flow] of [PagingData].
 * @param itemContent the content displayed by a single item. In case the item is `null`, the
 * [itemContent] method should handle the logic of displaying a placeholder instead of the main
 * content displayed by an item which is not `null`.
 */
public fun <T : Any> LazyListScope.items(
    lazyPagingItems: LazyPagingItems<T>,
    key: ((index: Int) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(value: T?) -> Unit
) {
    items(lazyPagingItems.itemCount, key = key) { index ->
        itemContent(lazyPagingItems.getAsState(index).value)
    }
}

/**
 * Adds the [LazyPagingItems] and their content to the scope where the content of an item is
 * aware of its local index. The range from 0 (inclusive) to [LazyPagingItems.itemCount] (exclusive)
 * always represents the full range of presentable items, because every event from
 * [PagingDataDiffer] will trigger a recomposition.
 *
 * @sample androidx.paging.compose.samples.ItemsIndexedDemo
 *
 * @param lazyPagingItems the items received from a [Flow] of [PagingData].
 * @param itemContent the content displayed by a single item. In case the item is `null`, the
 * [itemContent] method should handle the logic of displaying a placeholder instead of the main
 * content displayed by an item which is not `null`.
 */
public fun <T : Any> LazyListScope.itemsIndexed(
    lazyPagingItems: LazyPagingItems<T>,
    key: ((index: Int) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(index: Int, value: T?) -> Unit
) {
    items(lazyPagingItems.itemCount, key = key) { index ->
        itemContent(index, lazyPagingItems.getAsState(index).value)
    }
}