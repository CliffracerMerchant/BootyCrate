/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.bottombar

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.model.NavigationState
import com.cliffracertech.bootycrate.model.database.ItemDao
import com.cliffracertech.bootycrate.utils.collectAsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BottomAppBarViewModel(
    private val navigationState: NavigationState,
    private val itemDao: ItemDao,
    coroutineScope: CoroutineScope?
) : ViewModel() {

    @Inject constructor(
        navigationState: NavigationState,
        itemDao: ItemDao,
    ) : this(navigationState, itemDao, null)

    private val scope = coroutineScope ?: viewModelScope

    private val visibleScreen get() = navigationState.visibleScreen

    private val checkedItemsSize by itemDao
        .getCheckedShoppingListItemsSize()
        .collectAsState(0, scope)

    val checkoutButtonIsVisible by derivedStateOf {
        visibleScreen.isShoppingList
    }

    val checkoutButtonIsEnabled by derivedStateOf {
        checkedItemsSize > 0
    }

    private var oldShoppingListSize = 0
    val shoppingListSizeChange =
        itemDao.getShoppingListItemCount().map { shoppingListSize ->
            val change = shoppingListSize - oldShoppingListSize
            oldShoppingListSize = shoppingListSize
            if (visibleScreen.isShoppingList) 0
            else change
        }.drop(1).shareIn(scope, SharingStarted.WhileSubscribed(3000), 0)

    fun onCheckoutButtonClick() {
        if (visibleScreen.isShoppingList)
            scope.launch { itemDao.checkout() }
    }
}

