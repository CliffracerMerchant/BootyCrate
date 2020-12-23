/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.AnimatorInflater
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat
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
    override val actionMode = ShoppingListActionMode()

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
        recyclerView.finishInit(viewLifecycleOwner,
                                mainActivity.shoppingListViewModel,
                                mainActivity.inventoryViewModel)
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
            activity.fab.setOnClickListener{ recyclerView.addNewItem() }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.add_to_inventory_button) {
            activity.inventoryViewModel.addFromSelectedShoppingListItems()
            actionMode.finishAndClearSelection()
            true
        } else super.onOptionsItemSelected(item)
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
        val resId = if (enabling) R.animator.checkout_button_disabled_to_enabled_animation
                    else          R.animator.checkout_button_enabled_to_disabled_animation
        val anim = AnimatorInflater.loadAnimator(context, resId)
        anim.setTarget(activity.checkoutBtn)
        anim.start()
    }

    /** An override of RecyclerViewActionMode that alters the visibility of menu items specific to shopping list items. */
    inner class ShoppingListActionMode() : RecyclerViewFragment<ShoppingListItem>.RecyclerViewActionMode() {
        override fun onStart(actionBar: ActionBar, menu: Menu, titleView: TextView?) {
            super.onStart(actionBar, menu, titleView)
            menu.setGroupVisible(R.id.shopping_list_view_action_mode_menu_group, true)
        }

        override fun onFinish(actionBar: ActionBar, menu: Menu, titleView: TextView?) {
            super.onFinish(actionBar, menu, titleView)
            menu.setGroupVisible(R.id.shopping_list_view_action_mode_menu_group, false)
        }
    }
}