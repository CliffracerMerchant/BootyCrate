/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.activity

import android.animation.Animator
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.preference.PreferenceManager
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.fragment.AppSettingsFragment
import com.cliffracertech.bootycrate.recyclerview.ItemGroupSelectorOptionsMenu
import com.cliffracertech.bootycrate.utils.*
import com.cliffracertech.bootycrate.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A NavViewActivity with a predefined UI.
 *
 * MainActivity is a NavViewActivity subclass with a custom UI consisting of a
 * ListActionBar and a BottomNavigationDrawer. The navigation drawer contains a
 * BottomNavigationView and a BootyCrateBottomAppBar when it is collapsed, or
 * an app settings button and an ItemGroupSelector when it is expanded.
 */
@AndroidEntryPoint
class MainActivity : NavViewActivity() {

    private val actionBarViewModel: ActionBarViewModel by viewModels()
    private val bottomAppBarViewModel: BottomAppBarViewModel by viewModels()
    private val itemGroupSelectorViewModel: ItemGroupSelectorViewModel by viewModels()
    private var pendingCradleAnim: Animator? = null

    @Inject lateinit var messageHandler: MessageHandler
    lateinit var ui: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        initTheme()
        ui = MainActivityBinding.inflate(LayoutInflater.from(this))
        setContentView(ui.root)
        fragmentContainerId = ui.fragmentContainer.id
        navigationView = ui.bottomAppBar.ui.navigationView
        super.onCreate(savedInstanceState)
        initOnClickListeners()
        initAnimatorConfigs()
        initGradientStyle()

        messageHandler.messages.displayWithSnackBarAnchoredTo(ui.bottomAppBar)
        repeatWhenStarted {
            launch { actionBarViewModel.backButtonIsVisible.collect(ui.actionBar::setBackButtonIsVisible) }
            launch { actionBarViewModel.titleState.collect(ui.actionBar::setTitleState) }
            launch { actionBarViewModel.searchButtonState.collect(ui.actionBar::setSearchButtonState) }
            launch { actionBarViewModel.changeSortButtonState.collect(ui.actionBar::setChangeSortButtonState) }
            launch { actionBarViewModel.moreOptionsButtonVisible.collect(ui.actionBar::setMenuButtonVisible) }
            launch { actionBarViewModel.optionsMenuContent.collect(ui.actionBar::setOptionsMenuContents)}

            launch { bottomAppBarViewModel.uiState.collect(::updateBottomAppBarState) }
            launch { bottomAppBarViewModel.shoppingListSizeChange.collect(ui.bottomAppBar::updateShoppingListBadge) }
            launch { itemGroupSelectorViewModel.itemGroups.collect(ui.itemGroupSelector::submitList) }
        }
    }

    override fun onBackPressed() { ui.actionBar.onBackButtonClick?.invoke() }

    private fun updateBottomAppBarState(uiState: BottomAppBarViewModel.UiState) {
        val animate = ui.bottomNavigationDrawer.isLaidOut &&
                      !ui.bottomNavigationDrawer.isHidden
        // The cradle layout animation is stored here and started in the cradle
        // layout's layoutTransition's transition listener's transitionStart
        // override so that the animation is synced with the layout transition.
        pendingCradleAnim = ui.bottomAppBar.showCheckoutButton(
            showing = uiState.checkoutButtonVisible, animate = animate
        )?.apply {
            ui.bottomNavigationDrawer.isDraggable = false
            doOnEnd { ui.bottomNavigationDrawer.isDraggable = true }
        }
        ui.bottomAppBar.ui.checkoutButton.isEnabled = uiState.checkoutButtonIsEnabled
        ui.bottomNavigationDrawer.isHidden = !uiState.visible
    }

    private fun initTheme() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val prefKey = getString(R.string.pref_light_dark_mode_key)
        val themeDefault = getString(R.string.pref_theme_sys_default_title)
        val sysDarkThemeIsActive = Configuration.UI_MODE_NIGHT_YES ==
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        setTheme(when (prefs.getString(prefKey, themeDefault) ?: "") {
            getString(R.string.pref_theme_light_theme_title) -> R.style.LightTheme
            getString(R.string.pref_theme_dark_theme_title) ->  R.style.DarkTheme
            else -> if (sysDarkThemeIsActive) R.style.DarkTheme
                    else                      R.style.LightTheme
        })
    }

    private fun fwdMenuItemClick(menuItem: MenuItem) =
        visibleFragment?.onOptionsItemSelected(menuItem) ?: false

    private fun initOnClickListeners() {
        ui.actionBar.onBackButtonClick = {
            if (!actionBarViewModel.onBackPressed())
                supportFragmentManager.popBackStack()
        }
        ui.actionBar.onSearchButtonClick = actionBarViewModel::onSearchButtonClick
        ui.actionBar.onSearchQueryChange = actionBarViewModel::onSearchQueryChangeRequest
        ui.actionBar.onDeleteButtonClick = actionBarViewModel::onDeleteButtonClick
        ui.actionBar.setOnSortOptionClick { actionBarViewModel.onSortOptionClick(it.itemId) }
        ui.actionBar.setOnOptionsItemClick(::fwdMenuItemClick)

        ui.settingsButton.setOnClickListener { addSecondaryFragment(AppSettingsFragment()) }
        ui.bottomNavigationDrawer.addBottomSheetCallback(ui.bottomSheetCallback())
        ui.bottomAppBar.ui.checkoutButton.onConfirm = bottomAppBarViewModel::onCheckoutButtonClick
        ui.addItemGroupButton.setOnClickListener {
            itemGroupNameDialog(this, null, itemGroupSelectorViewModel::onConfirmAddNewItemGroupDialog)
        }
        ui.itemGroupSelectorOptionsButton.setOnClickListener {
            ItemGroupSelectorOptionsMenu(
                anchor = ui.itemGroupSelectorOptionsButton,
                multiSelectItemGroups = itemGroupSelectorViewModel.multiSelectGroups,
                onMultiSelectCheckboxClick = itemGroupSelectorViewModel::onMultiSelectCheckboxClick,
                onSelectAllClick = itemGroupSelectorViewModel::onSelectAllGroupsClick
            ).show()
        }
        ui.itemGroupSelector.onItemGroupClick =
            itemGroupSelectorViewModel::onItemGroupClick
        ui.itemGroupSelector.onItemGroupRenameRequest =
            itemGroupSelectorViewModel::onConfirmItemGroupRenameDialog
        ui.itemGroupSelector.onItemGroupDeletionRequest =
            itemGroupSelectorViewModel::onConfirmDeleteItemGroupDialog
    }

    private fun initAnimatorConfigs() {
        val transitionAnimConfig = AnimatorConfig(
            resources.getInteger(R.integer.primaryFragmentTransitionDuration).toLong(),
            AnimationUtils.loadInterpolator(this, R.anim.default_interpolator))
        primaryFragmentTransitionAnimatorConfig = transitionAnimConfig
        defaultSecondaryFragmentEnterAnimResId = R.animator.fragment_close_enter
        defaultSecondaryFragmentExitAnimResId = R.animator.fragment_close_exit
        ui.actionBar.animatorConfig = transitionAnimConfig

        ui.bottomAppBar.navIndicator.width =
            2.5f * ui.bottomAppBar.ui.navigationView.itemIconSize
        ui.bottomAppBar.animatorConfig = transitionAnimConfig
        ui.bottomAppBar.ui.cradleLayout.layoutTransition.apply {
            // views with bottomSheetBehaviors do not like to be animated by layout
            // transitions (it makes them jump up to the top of the screen), so we
            // have to set animateParentHierarchy to false to prevent this
            setAnimateParentHierarchy(false)
            doOnStart { pendingCradleAnim?.start()
                        pendingCradleAnim = null }
        }
    }
}