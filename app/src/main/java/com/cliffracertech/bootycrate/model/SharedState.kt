/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.model

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.cliffracertech.bootycrate.model.NavigationState.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Qualifier

/**
 * A state holder that contains the application's navigation state (described
 * as a value of [Screen]) in its member [visibleScreen]. A new [RootScreen]
 * (i.e. one of the possible [Screen]s when the back stack is empty) can be
 * chosen via the method [navigateToRootScreen]. An [AdditionalScreen] can be
 * added to the back stack via the method [addToStack], while the top-most
 * [AdditionalScreen] can be removed from the stack via the method [popStack].
 */
@ActivityRetainedScoped
class NavigationState @Inject constructor() {

    /** A navigation destination for the app. */
    sealed class Screen(val stackIndex: Int) {
        val isRootScreen get() = this is RootScreen
        val isAdditionalScreen get() = this is AdditionalScreen
        val isShoppingList get() = this is RootScreen.ShoppingList
        val isInventory get() = this is RootScreen.Inventory
        val isAppSettings get() = this is AdditionalScreen && this.type.isAppSettings
    }

    /** RootScreen's values describe the possible root [Screen]s for the
     * app, i.e. the possible [Screen]s when the back stack is empty. The
     * [leftToRightIndex] is the left to right ordering of the screen among
     * all [RootScreen]s, and can be used to determine the appropriate
     * transition animation to play. */
    sealed class RootScreen(
        val leftToRightIndex: Int
    ): Screen(stackIndex = 0) {
        object ShoppingList : RootScreen(0)
        object Inventory : RootScreen(1)
    }

    /** AdditionalScreenType's values describe the types of additional
     * [Screen]s that can appear on top of one of the [RootScreen]s.*/
    enum class AdditionalScreenType { AppSettings;
        val isAppSettings get() = this == AppSettings
    }

    /** AdditionalScreen describes a given Screen that can appear over
     * one of the [RootScreen]s. The type of screen is described by the
     * value of [type]. [Screen]'s property [stackIndex] can be used in
     * determining the appropriate transition animation to play. */
    class AdditionalScreen(
        val type: AdditionalScreenType,
        stackIndex: Int,
    ): Screen(stackIndex)

    var rootScreen by mutableStateOf<RootScreen>(RootScreen.ShoppingList)
        private set
    private val additionalScreenStack = mutableStateListOf<AdditionalScreen>()

    /** The current visible [Screen] in the MainActivity instance. */
    val visibleScreen by derivedStateOf {
        if (additionalScreenStack.isNotEmpty())
            additionalScreenStack.last()
        else rootScreen
    }

    /** Choose a new [RootScreen] for the app. This is a no-op if the back
     * stack is not empty (i.e. if [visibleScreen] is a value of [AdditionalScreen]
     * rather than [RootScreen]. */
    fun navigateToRootScreen(screen: RootScreen) {
        if (additionalScreenStack.isEmpty())
            rootScreen = screen
    }

    /** Add a new [AdditionalScreen] to the back stack. This is a no-op
     * if the top-most [AdditionalScreen] on the back stack's is the same
     * [AdditionalScreenType] as [screenType]. */
    fun addToStack(screenType: AdditionalScreenType) {
        if (additionalScreenStack.lastOrNull()?.type != screenType)
            additionalScreenStack.add(
                AdditionalScreen(screenType, additionalScreenStack.size + 1))
    }

    /** Pop the top element off of the back stack. The return
     * value indicates whether or not the top element was removed. */
    fun popStack(): Boolean {
        if (additionalScreenStack.isNotEmpty()) {
            additionalScreenStack.removeLast()
            return true
        }
        return false
    }
}

/** A state holder that holds a set of [Long] ids that represent selected
 * items. The [ImmutableSet] of selected item ids can be accessed through
 * the [ids] member. The selection's contents can be altered via the
 * methods [toggle], [addAll], and [clear]. */
class SelectionState {
    private val _ids: SnapshotStateMap<Long, Unit> = mutableStateMapOf()
    val ids by derivedStateOf {
        _ids.keys.toImmutableSet()
    }

    val size get() = _ids.size
    val isEmpty get() = _ids.isEmpty()
    val isNotEmpty get() = _ids.isNotEmpty()

    operator fun contains(id: Long) = id in _ids

    fun toggle(id: Long) {
        if (_ids[id] == null)
            _ids[id] = Unit
        else _ids.remove(id)
    }

    fun addAll(ids: List<Long>) =
        _ids.putAll(ids.map { it to Unit })

    fun clear() = _ids.clear()
}

/** A holder of volatile state (i.e. state that would not typically be
 * remembered across restarts) for a list of expandable, selectable, and
 * color sortable items. The [SelectionState] can be accessed through
 * [selection]. The id of the expanded item can be accessed through
 * [expandedItemId], and changed via the method [toggleExpansionFor].
 * Likewise, the id of the item for whom a color picker is being shown
 * can be accessed through [colorPickerItemId] and changed via the method
 * [toggleShowColorPickerFor]. */
class ItemListVolatileState @Inject constructor() {
    val selection = SelectionState()

    var expandedItemId by mutableStateOf<Long?>(null)
        private set
    fun toggleExpansionFor(itemId: Long) {
        colorPickerItemId = null
        expandedItemId = if (expandedItemId != itemId) itemId
                         else                          null
    }

    var colorPickerItemId by mutableStateOf<Long?>(null)
        private set
    fun toggleShowColorPickerFor(itemId: Long) {
        if (expandedItemId != itemId)
            expandedItemId = null
        colorPickerItemId = if (colorPickerItemId != itemId) itemId
                            else                             null
    }
}

@Module @InstallIn(ActivityRetainedComponent::class)
class SharedState {
    @Qualifier @Retention(AnnotationRetention.BINARY)
    annotation class ShoppingListVolatileState

    @Qualifier @Retention(AnnotationRetention.BINARY)
    annotation class InventoryVolatileState

    @ShoppingListVolatileState @ActivityRetainedScoped @Provides
    fun provideShoppingListVolatileState() = ItemListVolatileState()

    @InventoryVolatileState @ActivityRetainedScoped @Provides
    fun provideInventoryVolatileState() = ItemListVolatileState()

    @ShoppingListVolatileState @ActivityRetainedScoped @Provides
    fun provideShoppingListSelection(
        @ShoppingListVolatileState volatileState: ItemListVolatileState
    ) = volatileState.selection

    @InventoryVolatileState @ActivityRetainedScoped @Provides
    fun provideInventorySelection(
        @InventoryVolatileState volatileState: ItemListVolatileState
    ) = volatileState.selection

    @Qualifier @Retention(AnnotationRetention.BINARY)
    annotation class SearchQuery

    @SearchQuery @ActivityRetainedScoped @Provides
    fun provideSearchQueryState() = MutableStateFlow<String?>(null)
}

@ActivityRetainedScoped
class NewItemDialogVisibilityState @Inject constructor() {
    var showingNewShoppingListItemDialog by mutableStateOf(false)
    var showingNewInventoryItemDialog by mutableStateOf(false)
}