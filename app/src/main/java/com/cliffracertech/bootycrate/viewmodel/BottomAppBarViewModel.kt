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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BottomAppBarViewModel @Inject constructor(
    navigationState: MainActivityNavigationState,
    private val itemDao: ItemDao
) : ViewModel() {

    private val activeFragment = navigationState.activeFragment
    private val selectedNavItemId = navigationState.navViewSelectedItemId

    private val checkedItemsSize = itemDao.getCheckedShoppingListItemsSize()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), 0)

    data class UiState(
        val visible: Boolean = true,
        val checkoutButtonVisible: Boolean = true,
        val checkoutButtonIsEnabled: Boolean = false,
        val selectedNavItemId: Int = 0)

    val bottomAppBarState = activeFragment.combine(checkedItemsSize) { fragment, checkedItemsSize ->
        UiState(visible = !fragment.isOther,
                checkoutButtonVisible = fragment.isShoppingList,
                checkoutButtonIsEnabled = checkedItemsSize > 0,
                selectedNavItemId = selectedNavItemId.value)

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), UiState())

    private var oldShoppingListSize = 0
    val shoppingListSizeChange = itemDao.getShoppingListItemCount().map { shoppingListSize ->
        val change = shoppingListSize - oldShoppingListSize
        oldShoppingListSize = shoppingListSize
        if (activeFragment.value.isShoppingList) 0
        else change
    }.drop(1).shareIn(viewModelScope, SharingStarted.WhileSubscribed(3000), 0)

    fun onCheckoutButtonClick() {
        if (activeFragment.value.isShoppingList)
            viewModelScope.launch { itemDao.checkout() }
    }
}