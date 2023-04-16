/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.with
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffracertech.bootycrate.BuildConfig
import com.cliffracertech.bootycrate.actionbar.BootyCrateActionBar
import com.cliffracertech.bootycrate.bottomdrawer.BootyCrateBottomAppDrawer
import com.cliffracertech.bootycrate.itemlist.InventoryScreen
import com.cliffracertech.bootycrate.itemlist.ShoppingListScreen
import com.cliffracertech.bootycrate.model.NavigationState
import com.cliffracertech.bootycrate.settings.AppTheme
import com.cliffracertech.bootycrate.settings.PrefKeys
import com.cliffracertech.bootycrate.settings.edit
import com.cliffracertech.bootycrate.ui.theme.BootyCrateTheme
import com.cliffracertech.bootycrate.utils.awaitEnumPreferenceState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel(
    private val dataStore: DataStore<Preferences>,
    private val navigationState: NavigationState,
    messageHandler: MessageHandler,
    coroutineScope: CoroutineScope?
): ViewModel() {
    @Inject constructor(
        dataStore: DataStore<Preferences>,
        navigationState: NavigationState,
        messageHandler: MessageHandler,
    ): this(dataStore, navigationState, messageHandler, null)

    private val scope = coroutineScope ?: viewModelScope
    private val lastLaunchVersionCodeKey = intPreferencesKey(PrefKeys.lastLaunchVersionCode)
    private val appThemeKey = intPreferencesKey(PrefKeys.appTheme)
    val messages = messageHandler.messages

    // The app theme preference value must be obtained before the UI
    // is rendered to prevent the screen from flickering due to the
    // theme changing really quickly.
    val appTheme by runBlocking {
        dataStore.awaitEnumPreferenceState<AppTheme>(appThemeKey, scope)
    }

    val visibleScreen by navigationState::visibleScreen

    suspend fun getLastLaunchVersionCode() =
        dataStore.data.first()[lastLaunchVersionCodeKey] ?: 9

    fun updateLastLaunchVersionCode() =
        dataStore.edit(lastLaunchVersionCodeKey, BuildConfig.VERSION_CODE, scope)


    /** Try to handle a back button press, returning whether or not the press was handled. */
    fun onBackPressed(): Boolean = navigationState.popStack()
}

/**
 * A NavViewActivity with a predefined UI.
 *
 * MainActivity is a NavViewActivity subclass with a custom UI consisting of a
 * ListActionBar and a BottomNavigationDrawer. The navigation drawer contains a
 * BottomNavigationView and a BootyCrateBottomAppBar when it is collapsed, or
 * an app settings button and an ItemGroupSelector when it is expanded.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setThemedContent {
            val scaffoldState = rememberScaffoldState()
            MessageDisplayer(this, scaffoldState.snackbarHostState, viewModel.messages)
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = {
                    BootyCrateActionBar(onUnhandledBackButtonClick = ::onBackPressed)
                },
            ) { padding ->
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    MainContent(
                        visibleScreen = viewModel.visibleScreen,
                        modifier = Modifier.padding(padding),
                        maxWidth = maxWidth,
                        contentPadding = PaddingValues(start = 8.dp, top = 8.dp,
                                                       end = 8.dp, bottom = 64.dp))
                    BootyCrateBottomAppDrawer(
                        modifier = Modifier.align(Alignment.BottomStart),
                        additionalPeekHeight = WindowInsets.navigationBars
                            .asPaddingValues(LocalDensity.current)
                            .calculateBottomPadding())
                }
            }
        }
    }

    override fun onBackPressed() {
        if (!viewModel.onBackPressed())
            super.onBackPressed()
    }

    private fun setThemedContent(
        parent: CompositionContext? = null,
        content: @Composable () -> Unit
    ) = setContent(parent) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val themePref = viewModel.appTheme
        val useDarkTheme = themePref == AppTheme.Dark ||
            (themePref == AppTheme.MatchSystem && isSystemInDarkTheme())

        val uiController = rememberSystemUiController()
        // For some reason the status bar icons get reset
        // to a light color when the theme is changed, so
        // this effect needs to run after every theme change.
        LaunchedEffect(useDarkTheme) {
            uiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = true)
            uiController.setNavigationBarColor(
                color = Color.Transparent,
                darkIcons = true,
                navigationBarContrastEnforced = false)
        }
        BootyCrateTheme(useDarkTheme) { content() }
    }

    @Composable fun MainContent(
        visibleScreen: NavigationState.Screen,
        modifier: Modifier = Modifier,
        maxWidth: Dp,
        contentPadding: PaddingValues,
    ) {
        var lastScreen by remember { mutableStateOf(visibleScreen) }
        val lastScreenValue = lastScreen
        lastScreen = visibleScreen

        val shoppingListScrollState = rememberLazyListState()
        val inventoryScrollState = rememberLazyListState()

        AnimatedContent(
            targetState = visibleScreen,
            modifier = modifier,
            transitionSpec = {
                if (visibleScreen is NavigationState.RootScreen &&
                    lastScreenValue is NavigationState.RootScreen
                ) {
                    val leftToRight = visibleScreen.leftToRightIndex >
                                      lastScreenValue.leftToRightIndex
                    slideIn { IntOffset(if (leftToRight) it.width else -it.width, 0) } + fadeIn() with
                    slideOut { IntOffset(if (leftToRight) -it.width else it.width, 0) } + fadeOut()
                } else {
                    val addingToStack = visibleScreen.stackIndex > lastScreen.stackIndex
                    val enteringInitialScale = if (addingToStack) 1.1f else 0.9f
                    val exitingTargetScale = if (addingToStack) 0.9f else 1.1f
                    (scaleIn(initialScale = enteringInitialScale) + fadeIn() with
                     scaleOut(targetScale = exitingTargetScale) + fadeOut()).apply {
                        targetContentZIndex = if (addingToStack) 1f else -1f
                    }
                }
            }
        ) { when {
            it.isShoppingList ->
                ShoppingListScreen(Modifier, maxWidth, contentPadding, shoppingListScrollState)
            it.isInventory ->
                InventoryScreen(Modifier, maxWidth, contentPadding, inventoryScrollState)
            it.isAppSettings -> {
                val settingsScrollState = rememberLazyListState()
            }
        }}
    }

//    private fun checkForNeededMigrations() {
//        val
//    }
}