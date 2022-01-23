/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.database.ItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class BottomAppBarViewModel @Inject constructor(
    navigationState: MainActivityNavigationState,
    itemDao: ItemDao
) : ViewModel() {

    private val activeFragment = navigationState.activeFragment
    private val navViewSelectedItemId = navigationState.navViewSelectedItemId
    private var shoppingListSize = 0

    private val shoppingListSizeChange = itemDao.getShoppingListItemCount()
        .map { newShoppingListSize ->
            val change = newShoppingListSize - shoppingListSize
            shoppingListSize = newShoppingListSize
            if (activeFragment.value.isShoppingList) 0
            else change
        }.drop(1)

    data class UiState(
        val visible: Boolean = true,
        val checkoutButtonVisible: Boolean = true,
        val selectedNavItemId: Int = 0,
        val shoppingListSizeChange: Int = 0)

    val bottomAppBarState = combine(
        activeFragment,
        shoppingListSizeChange
    ) { fragment, shoppingListSizeChange ->
        UiState(visible = !fragment.isOther,
                checkoutButtonVisible = fragment.isShoppingList,
                selectedNavItemId = navViewSelectedItemId.value,
                shoppingListSizeChange = shoppingListSizeChange)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), UiState())
}