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
package com.twidere.twiderex.scenes.user

import androidx.compose.foundation.layout.Box
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.twidere.twiderex.R
import com.twidere.twiderex.component.UserComponent
import com.twidere.twiderex.component.foundation.AppBar
import com.twidere.twiderex.component.foundation.AppBarNavigationButton
import com.twidere.twiderex.component.foundation.InAppNotificationScaffold
import com.twidere.twiderex.component.status.UserName
import com.twidere.twiderex.di.assisted.assistedViewModel
import com.twidere.twiderex.extensions.observeAsState
import com.twidere.twiderex.extensions.withElevation
import com.twidere.twiderex.model.MicroBlogKey
import com.twidere.twiderex.model.enums.PlatformType
import com.twidere.twiderex.navigation.RootRoute
import com.twidere.twiderex.ui.LocalActiveAccount
import com.twidere.twiderex.ui.LocalNavController
import com.twidere.twiderex.ui.TwidereScene
import com.twidere.twiderex.viewmodel.dm.DMNewConversationViewModel
import com.twidere.twiderex.viewmodel.user.UserViewModel

@Composable
fun UserScene(
    userKey: MicroBlogKey,
) {
    val account = LocalActiveAccount.current ?: return
    val viewModel = assistedViewModel<UserViewModel.AssistedFactory, UserViewModel>(
        account,
        userKey,
    ) {
        it.create(account, userKey)
    }

    val conversationViewModel = assistedViewModel<DMNewConversationViewModel.AssistedFactory, DMNewConversationViewModel>(
        account,
    ) {
        it.create(account)
    }
    val user by viewModel.user.observeAsState(initial = null)
    val navController = LocalNavController.current
    var expanded by remember { mutableStateOf(false) }
    var showBlockAlert by remember { mutableStateOf(false) }
    val relationship by viewModel.relationship.observeAsState(initial = null)
    val loadingRelationship by viewModel.loadingRelationship.observeAsState(initial = false)
    TwidereScene {
        InAppNotificationScaffold(
            // TODO: Show top bar with actions
            topBar = {
                AppBar(
                    backgroundColor = MaterialTheme.colors.surface.withElevation(),
                    navigationIcon = {
                        AppBarNavigationButton()
                    },
                    actions = {
                        if (account.type == PlatformType.Twitter && user?.platformType == PlatformType.Twitter) {
                            user?.let {
                                if (userKey != account.accountKey) {
                                    IconButton(
                                        onClick = {
                                            conversationViewModel.createNewConversation(
                                                it,
                                                onResult = { conversationKey ->
                                                    conversationKey?.let {
                                                        navController.navigate(RootRoute.Messages.Conversation(it))
                                                    }
                                                }
                                            )
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_mail),
                                            contentDescription = stringResource(
                                                id = R.string.scene_messages_title
                                            ),
                                            tint = MaterialTheme.colors.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        Box {
                            if (userKey != account.accountKey) {
                                IconButton(
                                    onClick = {
                                        expanded = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = stringResource(
                                            id = R.string.accessibility_common_more
                                        ),
                                        tint = MaterialTheme.colors.onSurface
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                relationship.takeIf { !loadingRelationship }
                                    ?.blocking?.let { blocking ->
                                        DropdownMenuItem(
                                            onClick = {
                                                if (blocking)
                                                    viewModel.unblock()
                                                else
                                                    showBlockAlert = true
                                                expanded = false
                                            }
                                        ) {
                                            Text(
                                                text = stringResource(
                                                    id = if (blocking) R.string.common_controls_friendship_actions_unblock
                                                    else R.string.common_controls_friendship_actions_block
                                                )
                                            )
                                        }
                                    }
                            }
                        }
                    },
                    elevation = 0.dp,
                    title = {
                        user?.let {
                            UserName(user = it)
                        }
                    }
                )
            }
        ) {
            Box {
                UserComponent(userKey)
                if (showBlockAlert) {
                    user?.let {
                        BlockAlert(
                            screenName = it.getDisplayScreenName(it.userKey.host),
                            onDismissRequest = { showBlockAlert = false },
                            onConfirm = {
                                viewModel.block()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockAlert(
    screenName: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            onDismissRequest.invoke()
        },
        title = {
            Text(
                text = stringResource(id = R.string.common_alerts_block_user_confirm_title, screenName),
                style = MaterialTheme.typography.subtitle1
            )
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest.invoke()
                }
            ) {
                Text(text = stringResource(id = R.string.common_controls_actions_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismissRequest.invoke()
                }
            ) {
                Text(text = stringResource(id = R.string.common_controls_actions_yes))
            }
        },
    )
}
