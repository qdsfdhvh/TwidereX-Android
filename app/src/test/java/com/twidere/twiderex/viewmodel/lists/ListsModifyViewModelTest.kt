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
package com.twidere.twiderex.viewmodel.lists

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.twidere.twiderex.mock.MockCenter
import com.twidere.twiderex.model.AccountDetails
import com.twidere.twiderex.model.MicroBlogKey
import com.twidere.twiderex.notification.InAppNotification
import com.twidere.twiderex.notification.NotificationEvent
import com.twidere.twiderex.repository.ListsRepository
import com.twidere.twiderex.viewmodel.ViewModelTestBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ListsModifyViewModelTest : ViewModelTestBase() {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private var mockRepository: ListsRepository = ListsRepository(MockCenter.mockCacheDatabase())

    @Mock
    private lateinit var mockAppNotification: InAppNotification

    private val mockAccount: AccountDetails = mock {
        on { service }.doReturn(MockCenter.mockListsService())
        on { accountKey }.doReturn(MicroBlogKey.twitter("123"))
    }

    @Mock
    private lateinit var mockSuccessObserver: Observer<Boolean>

    @Mock
    private lateinit var mockLoadingObserver: Observer<Boolean>

    private var errorNotification: NotificationEvent? = null

    private lateinit var modifyViewModel: ListsModifyViewModel

    private val scope = CoroutineScope(Dispatchers.Main)

    @Test
    fun updateList_successExpectTrue(): Unit = runBlocking(Dispatchers.Main) {
        verifySuccessAndLoadingBefore(mockLoadingObserver, mockSuccessObserver)
        suspendCoroutine<Boolean> {
            modifyViewModel.editList(
                listId = "123",
                title = "title",
                private = false
            ) { success, _ ->
                mockSuccessObserver.onChanged(success)
                it.resume(success)
            }
        }
        verifySuccessAndLoadingAfter(mockLoadingObserver, mockSuccessObserver, true)
    }

    @Test
    fun updateList_failedExpectFalseAndShowNotification(): Unit = runBlocking(Dispatchers.Main) {
        verifySuccessAndLoadingBefore(mockLoadingObserver, mockSuccessObserver)
        Assert.assertNull(errorNotification)
        suspendCoroutine<Boolean> {
            modifyViewModel.editList(
                listId = "error",
                title = "name",
                private = false
            ) { success, _ ->
                mockSuccessObserver.onChanged(success)
                it.resume(success)
            }
        }
        verifySuccessAndLoadingAfter(mockLoadingObserver, mockSuccessObserver, false)
        Assert.assertNotNull(errorNotification)
    }

    @Test
    fun deleteList_successExpectTrue(): Unit = runBlocking(Dispatchers.Main) {
        verifySuccessAndLoadingBefore(mockLoadingObserver, mockSuccessObserver)
        suspendCoroutine<Boolean> {
            modifyViewModel.deleteList(listId = "123", MicroBlogKey.Empty) { success, _ ->
                mockSuccessObserver.onChanged(success)
                it.resume(success)
            }
        }
        verifySuccessAndLoadingAfter(mockLoadingObserver, mockSuccessObserver, true)
    }

    @Test
    fun deleteList_failedExpectFalseAndShowNotification(): Unit = runBlocking(Dispatchers.Main) {
        verifySuccessAndLoadingBefore(mockLoadingObserver, mockSuccessObserver)
        Assert.assertNull(errorNotification)
        suspendCoroutine<Boolean> {
            modifyViewModel.deleteList(listId = "error", MicroBlogKey.Empty) { success, _ ->
                mockSuccessObserver.onChanged(success)
                it.resume(success)
            }
        }
        verifySuccessAndLoadingAfter(mockLoadingObserver, mockSuccessObserver, false)
        Assert.assertNotNull(errorNotification)
    }

    @Test
    fun subscribeList_successExpectTrue(): Unit = runBlocking(Dispatchers.Main) {
        verifySuccessAndLoadingBefore(mockLoadingObserver, mockSuccessObserver)
        suspendCoroutine<Boolean> {
            modifyViewModel.subscribeList(MicroBlogKey.twitter("123")) { success, _ ->
                mockSuccessObserver.onChanged(success)
                it.resume(success)
            }
        }
        verifySuccessAndLoadingAfter(mockLoadingObserver, mockSuccessObserver, true)
    }

    @Test
    fun subscribeList_failedExpectFalseAndShowNotification(): Unit = runBlocking(Dispatchers.Main) {
        verifySuccessAndLoadingBefore(mockLoadingObserver, mockSuccessObserver)
        Assert.assertNull(errorNotification)
        suspendCoroutine<Boolean> {
            modifyViewModel.subscribeList(MicroBlogKey.twitter("error")) { success, _ ->
                mockSuccessObserver.onChanged(success)
                it.resume(success)
            }
        }
        verifySuccessAndLoadingAfter(mockLoadingObserver, mockSuccessObserver, false)
        Assert.assertNotNull(errorNotification)
    }

    @Test
    fun unsubscribeList_successExpectTrue(): Unit = runBlocking(Dispatchers.Main) {
        verifySuccessAndLoadingBefore(mockLoadingObserver, mockSuccessObserver)
        suspendCoroutine<Boolean> {
            modifyViewModel.unsubscribeList(MicroBlogKey.twitter("123")) { success, _ ->
                mockSuccessObserver.onChanged(success)
                it.resume(success)
            }
        }
        verifySuccessAndLoadingAfter(mockLoadingObserver, mockSuccessObserver, true)
    }

    @Test
    fun unsubscribeList_failedExpectFalseAndShowNotification(): Unit =
        runBlocking(Dispatchers.Main) {
            verifySuccessAndLoadingBefore(mockLoadingObserver, mockSuccessObserver)
            Assert.assertNull(errorNotification)
            suspendCoroutine<Boolean> {
                modifyViewModel.unsubscribeList(MicroBlogKey.twitter("error")) { success, _ ->
                    mockSuccessObserver.onChanged(success)
                    it.resume(success)
                }
            }
            verifySuccessAndLoadingAfter(mockLoadingObserver, mockSuccessObserver, false)
            Assert.assertNotNull(errorNotification)
        }

    override fun setUp() {
        super.setUp()
        mockRepository = ListsRepository(MockCenter.mockCacheDatabase())
        modifyViewModel = ListsModifyViewModel(
            mockRepository,
            mockAppNotification,
            mockAccount,
            listKey = MicroBlogKey.Empty,
        )
        whenever(mockAppNotification.show(any<NotificationEvent>())).then {
            errorNotification = it.getArgument(0) as NotificationEvent
            Unit
        }
        errorNotification = null
        mockSuccessObserver.onChanged(false)
        scope.launch {
            modifyViewModel.loading.collect {
                mockLoadingObserver.onChanged(it)
            }
        }
    }

    private fun verifySuccessAndLoadingBefore(
        loadingObserver: Observer<Boolean>,
        successObserver: Observer<Boolean>
    ) {
        verify(loadingObserver, times(1)).onChanged(false)
        verify(successObserver).onChanged(false)
    }

    private fun verifySuccessAndLoadingAfter(
        loadingObserver: Observer<Boolean>,
        successObserver: Observer<Boolean>,
        success: Boolean
    ) {
        verify(loadingObserver, times(1)).onChanged(true)
        verify(loadingObserver, times(1)).onChanged(false)
        verify(successObserver, if (success) times(1) else times(2)).onChanged(success)
    }
}
