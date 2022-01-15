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
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.databinding.MainActivityBinding
import com.cliffracertech.bootycrate.fragment.AppSettingsFragment
import com.cliffracertech.bootycrate.recyclerview.ItemGroupSelectorOptionsMenu
import com.cliffracertech.bootycrate.utils.*
import com.cliffracertech.bootycrate.utils.setPadding
import com.cliffracertech.bootycrate.viewmodel.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * A BottomNavViewActivity with a fragment interface that enables implementing fragments to use its custom UI.
 *
 * MainActivity is a BottomNavViewActivity subclass with a custom UI including
 * a ListActionBar, a BottomAppBar, and a checkout button and an add button in
 * the cradle of the BottomAppBar. In order for fragments to inform MainActivity
 * which of these UI elements should be displayed when they are active, the
 * fragment should implement MainActivity.FragmentInterface. While it is not
 * necessary for fragments to implement FragmentInterface, fragments that do
 * not will not be able to affect the visibility of the MainActivity UI when
 * they are displayed.
 */
class MainActivity : BottomNavViewActivity() {
    private val viewModel: MainActivityViewModel by viewModels()
    private val shoppingListViewModel: ShoppingListViewModel by viewModels()
    private val inventoryViewModel: InventoryViewModel by viewModels()
    private val itemGroupSelectorViewModel: ItemGroupSelectorViewModel by viewModels()

    lateinit var ui: MainActivityBinding
    private var pendingCradleAnim: Animator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        initTheme()
        ui = MainActivityBinding.inflate(LayoutInflater.from(this))
        setContentView(ui.root)
        fragmentContainerId = ui.fragmentContainer.id
        navigationView = ui.bottomNavigationView
        super.onCreate(savedInstanceState)
        initOnClickListeners()
        initAnimatorConfigs()
        initGradientStyle()

        shoppingListViewModel.onDeletedItemsMessage = viewModel::postItemsDeletedMessage
        inventoryViewModel.onDeletedItemsMessage = viewModel::postItemsDeletedMessage
        repeatWhenStarted {
            launch { viewModel.messages.collect(::displayMessage) }
            launch { viewModel.searchFilter.collect { shoppingListViewModel.searchFilter = it
                                                      inventoryViewModel.searchFilter = it }}
            launch { viewModel.backButtonIsVisible.collect(ui.actionBar::setBackButtonIsVisible) }
            launch { viewModel.titleState.collect(ui.actionBar::setTitleState) }
            launch { viewModel.searchButtonState.collect(ui.actionBar::setSearchButtonState) }
            launch { viewModel.changeSortButtonState.collect(ui.actionBar::setChangeSortButtonState) }
            launch { viewModel.moreOptionsButtonVisible.collect(ui.actionBar::setMenuButtonVisible) }
            launch { viewModel.shoppingListSizeChange.collect(::updateShoppingListBadge) }
            launch { itemGroupSelectorViewModel.itemGroups.collect(ui.itemGroupSelector::submitList) }
        }
        ui.itemGroupSelector.onItemClick = itemGroupSelectorViewModel::onItemGroupClick
        ui.itemGroupSelector.onItemRenameRequest = itemGroupSelectorViewModel::onConfirmItemGroupRenameDialog
        ui.itemGroupSelector.onItemDeletionRequest = itemGroupSelectorViewModel::onConfirmDeleteItemGroupDialog
    }

    private fun displayMessage(message: MessageViewModel.Message) {
        val snackBar = Snackbar.make(ui.root, message.text, Snackbar.LENGTH_LONG)
            .setAnchorView(ui.bottomAppBar)
            .setAction(message.actionText) { message.onActionClick?.invoke() }
            .setActionTextColor(theme.resolveIntAttribute(R.attr.colorAccent))
            .addCallback(object: BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    message.onDismiss?.invoke(event)
                }
            })
        val textView = snackBar.view.findViewById<TextView>(R.id.snackbar_text)
        (textView.parent as? View)?.setPadding(start = dpToPixels(10f).toInt())
        snackBar.show()
        SoftKeyboard.hide(ui.root)
    }

    override fun onBackPressed() { ui.actionBar.onBackButtonClick?.invoke() }

    override fun onNewFragmentSelected(oldFragment: Fragment?, newFragment: Fragment) {
        val needToAnimate = oldFragment != null
        if (oldFragment != null)
            (oldFragment as? MainActivityFragment)?.onActiveStateChanged(isActive = false, ui)
        else if (newFragment is MainActivityFragment) {
            // currentFragment being null implies an activity restart. In
            // this case we need to set cradleLayout to visible or invisible
            // to ensure that the bottom app bar measures its top edge path
            // length properly, and initialize the nav indicator alpha so
            // that it isn't visible when the bottomAppBar is hidden.
            val showsBottomAppBar = newFragment.showsBottomAppBar()
            ui.cradleLayout.isInvisible = !showsBottomAppBar
            ui.bottomAppBar.navIndicator.alpha = if (showsBottomAppBar) 1f else 0f
        }
        viewModel.onNewFragmentSelected(newFragment)
        if (newFragment !is MainActivityFragment) return

        if (newFragment.showsBottomAppBar()) ui.bottomNavigationDrawer.show()
        else                                 ui.bottomNavigationDrawer.hide()

        val needToAnimateCheckoutButton = needToAnimate && !ui.bottomNavigationDrawer.isHidden
        val showsCheckoutButton = newFragment.showsCheckoutButton()
        if (showsCheckoutButton != null)
            // The cradle animation is stored here and started in the cradle
            // layout's layoutTransition's transition listener's transitionStart
            // override so that the animation is synced with the layout transition.
            pendingCradleAnim = ui.showCheckoutButton(
                showing = showsCheckoutButton,
                animatorConfig = primaryFragmentTransitionAnimatorConfig,
                animate = needToAnimateCheckoutButton)

        ui.bottomAppBar.navIndicator.moveToItem(menuItemId = navigationView.selectedItemId,
                                                animate = needToAnimateCheckoutButton)
        newFragment.onActiveStateChanged(isActive = true, ui)
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
            if (!viewModel.onBackPressed())
                supportFragmentManager.popBackStack()
        }
        ui.actionBar.onSearchButtonClick = viewModel::onSearchButtonClick
        ui.actionBar.onSearchQueryChange = viewModel::onSearchFilterChangeRequest
        ui.actionBar.onDeleteButtonClick = viewModel::onDeleteButtonClick
        ui.actionBar.setOnSortOptionClick { viewModel.onSortOptionSelected(it.itemId) }
        ui.actionBar.setOnOptionsItemClick(::fwdMenuItemClick)

        ui.settingsButton.setOnClickListener { addSecondaryFragment(AppSettingsFragment()) }
        ui.bottomNavigationDrawer.addBottomSheetCallback(ui.bottomSheetCallback())
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
    }

    private fun initAnimatorConfigs() {
        val transitionAnimConfig = AnimatorConfig(
            resources.getInteger(R.integer.primaryFragmentTransitionDuration).toLong(),
            AnimationUtils.loadInterpolator(this, R.anim.default_interpolator))
        primaryFragmentTransitionAnimatorConfig = transitionAnimConfig
        defaultSecondaryFragmentEnterAnimResId = R.animator.fragment_close_enter
        defaultSecondaryFragmentExitAnimResId = R.animator.fragment_close_exit
        ui.actionBar.animatorConfig = transitionAnimConfig
        ui.bottomAppBar.navIndicator.width = 2.5f * ui.bottomNavigationView.itemIconSize
        ui.bottomAppBar.navIndicator.animatorConfig = transitionAnimConfig
            // The nav indicator anim duration is increased to improve its visibility
            .copy(duration = (transitionAnimConfig.duration * 1.2f).toLong())
        ui.checkoutButton.animatorConfig = transitionAnimConfig
        ui.cradleLayout.layoutTransition = layoutTransition(transitionAnimConfig).apply {
            // views with bottomSheetBehaviors do not like to be animated by layout
            // transitions (it makes them jump up to the top of the screen), so we
            // have to set animatedParentHierarchy to false to prevent this
            setAnimateParentHierarchy(false)
            doOnStart { pendingCradleAnim?.start()
                        pendingCradleAnim = null }
        }
    }

    private var accumulatedNewItemCount = 0
    private fun updateShoppingListBadge(shoppingListSizeChange: Int) {
        accumulatedNewItemCount += shoppingListSizeChange
        if (accumulatedNewItemCount == 0) return

        val badgeParent = ui.shoppingListBadge.parent as? View
        if (badgeParent?.isVisible != true) return

        ui.shoppingListBadge.isVisible = true
        ui.shoppingListBadge.alpha = 1f
        ui.shoppingListBadge.text = getString(R.string.shopping_list_badge_text,
                                              accumulatedNewItemCount)
        ui.shoppingListBadge.animate().alpha(0f)
            .setDuration(1000).setStartDelay(1500)
            .withEndAction { accumulatedNewItemCount = 0 }
            .withLayer().start()
    }


    /**
     * An interface that informs MainActivity how its Fragment implementor affects the main activity ui.
     *
     * MainActivityFragment can be implemented by a Fragment subclass to
     * inform the MainActivity as to how its UI should be displayed when the
     * fragment is in the foreground, and to provide a callback for back button
     * (either the hardware/software bottom back button or the action bar's
     * back button) presses. Fragments should always indicate their desired
     * bottom app bar and checkout button states through an override of the
     * appropriate function instead of changing their visibility manually to
     * ensure that the appropriate hide/show animations are played. The
     * function addSecondaryFragment allows implementing fragments to add a
     * secondary fragment (see BottomNavViewActivity documentation for an
     * explanation of primary/secondary fragments) themselves.
     */
    interface MainActivityFragment {
        /** Return whether the bottom app bar should be visible when the implementing fragment is.*/
        fun showsBottomAppBar() = true
        /** Return whether the checkout button should be visible when the implementing fragment
         * is, or null if the implementing fragment's showsBottomAppBar returns false.*/
        fun showsCheckoutButton(): Boolean? = null
        /** Return whether the implementing fragment consumed the back button press. */
        fun onBackPressed() = false
        /** Perform any additional actions on the @param activityUi that the fragment desires,
         * given its @param isActive state. */
        fun onActiveStateChanged(isActive: Boolean, activityUi: MainActivityBinding) { }

        fun addSecondaryFragment(fragment: Fragment) {
            val mainActivityFragment = this as? Fragment ?: throw IllegalStateException(
                "Implementors of MainActivityFragment must inherit from androidx.fragment.app.Fragment")
            val mainActivity = mainActivityFragment.activity as? MainActivity ?: throw IllegalStateException(
                "Implementors of MainActivityFragment must be hosted inside a MainActivity instance.")
            mainActivity.addSecondaryFragment(fragment)
        }
    }
}