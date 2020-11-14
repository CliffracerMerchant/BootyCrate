/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.shopping_list_fragment_layout.*

/** A fragment to display and modify the user's shopping list.
 *
 *  ShoppingListFragment is a RecyclerViewFragment subclass to view and modify
 *  the user's shopping list using an ShoppingListRecyclerView. ShoppingList-
 *  Fragment overrides RecyclerViewFragment's abstract recyclerView property
 *  with an instance of ShoppingListRecyclerView, and overrides its ActionMode-
 *  Callback with its own version.
 *
 *  ShoppingListFragment also manages the state and function of the checkout
 *  button. The checkout button is enabled when the user has checked at least
 *  one shopping list item, and disabled when no items are checked through its
 *  observation of ShoppingListRecyclerView's checkedItems member. If the check-
 *  out button is clicked while it is enabled, it switches to its confirmatory
 *  state to safeguard the user from checking out accidentally. If the user
 *  does not press the button again within two seconds, it will revert to its
 *  normal state. */
class ShoppingListFragment(isActive: Boolean = false) :
        RecyclerViewFragment<ShoppingListItem>(isActive) {

    override lateinit var recyclerView: ShoppingListRecyclerView
    override val actionModeCallback = ActionModeCallback()
    override val fabRegularOnClickListener = View.OnClickListener { recyclerView.addNewItem() }
    override val fabActionModeOnClickListener = View.OnClickListener { recyclerView.deleteSelectedItems() }

    private val handler = Handler()
    private var checkoutButtonLastPressTimeStamp = 0L
    private var darkGrayColor: Int = 0
    private var lightGrayColor: Int = 0
    private var blackColor: Int = 0
    private var yellowColor: Int = 0
    private lateinit var checkoutButtonNormalText: String
    private lateinit var checkoutButtonConfirmText: String

    private var checkoutButtonIsEnabled = false
        set(value) { enableCheckoutButton(value); field = value }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.shopping_list_fragment_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = shoppingListRecyclerView
        val mainActivity = requireActivity() as MainActivity

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sortStr = prefs.getString(mainActivity.getString(R.string.pref_shopping_list_sort),
                                      ViewModelItem.Sort.Color.toString())
        val initialSort = ViewModelItem.sortFrom(sortStr)
        recyclerView.finishInit(viewLifecycleOwner, mainActivity.shoppingListViewModel,
                                mainActivity.inventoryViewModel,
                                mainActivity.supportFragmentManager, initialSort)

        darkGrayColor = ContextCompat.getColor(mainActivity, R.color.colorTextLightSecondary)
        lightGrayColor = ContextCompat.getColor(mainActivity, android.R.color.darker_gray)
        blackColor = ContextCompat.getColor(mainActivity, android.R.color.black)
        yellowColor = ContextCompat.getColor(mainActivity, R.color.checkoutButtonEnabledColor)
        checkoutButtonNormalText = getString(R.string.checkout_description)
        checkoutButtonConfirmText = getString(R.string.checkout_confirm_description)

        recyclerView.checkedItems.sizeLiveData.observe(viewLifecycleOwner) { newSize ->
            if (newSize > 0)
                checkoutButtonIsEnabled = true
            if (newSize == 0) {
                revertCheckoutButtonToNormalState()
                checkoutButtonIsEnabled = false
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActiveStateChanged(active: Boolean) {
        super.onActiveStateChanged(active)
        if (active) {
            activity.checkoutBtn.setOnClickListener {
                if (!checkoutButtonIsEnabled) return@setOnClickListener
                val currentTime = System.currentTimeMillis()
                if (currentTime < checkoutButtonLastPressTimeStamp + 2000) {
                    revertCheckoutButtonToNormalState()
                    recyclerView.checkout()
                } else {
                    checkoutButtonLastPressTimeStamp = currentTime
                    activity.checkoutBtn.text = checkoutButtonConfirmText
                    handler.removeCallbacks(::revertCheckoutButtonToNormalState)
                    handler.postDelayed(::revertCheckoutButtonToNormalState, 2000)
                }
            }
        }
    }

    override fun setOptionsMenuItemsVisible(showing: Boolean) {
        super.setOptionsMenuItemsVisible(showing)
        menu?.setGroupVisible(R.id.shopping_list_view_menu_group, showing)
    }

    private fun revertCheckoutButtonToNormalState() {
        activity.checkoutBtn.text = checkoutButtonNormalText
        checkoutButtonLastPressTimeStamp = 0
    }

    private fun enableCheckoutButton(enabling: Boolean) {
        if (checkoutButtonIsEnabled == enabling) return
        val bgColorAnim = ValueAnimator.ofArgb(if (enabling) lightGrayColor else yellowColor,
                                               if (enabling) yellowColor else lightGrayColor)
        bgColorAnim.duration = 200
        bgColorAnim.addUpdateListener {
            activity.checkoutBtn.backgroundTintList = ColorStateList.valueOf(it.animatedValue as Int)
        }
        bgColorAnim.start()
        val textColorAnim = ObjectAnimator.ofArgb(activity.checkoutBtn, "textColor",
                                                  if (enabling) darkGrayColor else blackColor,
                                                  if (enabling) blackColor else darkGrayColor)
        textColorAnim.duration = 200
        textColorAnim.start()
    }

    /** An ActionMode.Callback for use when the user selects one or more shopping list items.
     *
     *  ActionModeCallback overrides RecyclerViewFragment.ActionModeCallback with
     *  new implementations of onActionItemClicked and onPrepareActionMode. */
    inner class ActionModeCallback : RecyclerViewFragment<ShoppingListItem>.ActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            return if (super.onCreateActionMode(mode, menu)) {
                menu.setGroupVisible(R.id.shopping_list_view_action_mode_menu_group, true)
                true
            } else false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.add_to_inventory_button -> {
                    activity.inventoryViewModel.addFromSelectedShoppingListItems()
                    true
                } else -> activity.onOptionsItemSelected(item)
            }
        }
    }
}