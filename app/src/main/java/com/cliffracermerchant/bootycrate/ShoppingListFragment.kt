/* Copyright 2020 Nicholas Hochstetler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isEmpty
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.shopping_list_fragment_layout.recyclerView

/** A fragment to display and modify the user's shopping list.
 *
 *  ShoppingListFragment's primary functions are to host an instance of Shop-
 *  pingListRecyclerView, and to manage the state and function of the floating
 *  action button and the checkout button.
 *
 *  ShoppingListFragment initially sets the onClickListener and icon of the
 *  main activity's floating action button to add a new item to the shopping
 *  list. ShoppingListFragment also contains a custom ActionMode to respond to
 *  the user selecting one or more shopping list items in the shopping list.
 *  When in its action mode, ShoppingListFragment will change the onClickList-
 *  ener and icon of the floating action button to indicate that it is now used
 *  to delete the selected items rather than to add a new one.
 *
 *  The checkout button can be either enabled or disabled, and is controlled
 *  using the property checkoutButtonIsEnabled. ShoppingListFragment observes
 *  its ShoppingListRecyclerView's checkedItems.sizeLiveData to determined
 *  whether or not to enable or disable the checkout button. If the checkout
 *  button is clicked while it is enabled, it switches to its confirmatory
 *  state to safeguard the user from checking out accidentally. If the user
 *  does not press the button again within two seconds, it will revert to its
 *  normal state. This can also be accomplished manually using the member func-
 *  tion revertCheckoutButtonToNormalState(). */
class ShoppingListFragment : Fragment() {
    private lateinit var mainActivity: MainActivity
    private lateinit var menu: Menu
    private var actionMode: ActionMode? = null
    private val handler = Handler()
    private var savedSelectionState: IntArray? = null
    private lateinit var fabIconController: TwoStateAnimatedIconController
    private var checkoutButtonLastPressTimeStamp = 0L

    private var darkGrayColor: Int = 0
    private var lightGrayColor: Int = 0
    private var blackColor: Int = 0
    private var yellowColor: Int = 0
    private lateinit var checkoutButtonNormalText: String
    private lateinit var checkoutButtonConfirmText: String

    private var checkoutButtonIsEnabled = false
        set(value) { enableCheckoutButton(value); field = value }

    init { setHasOptionsMenu(true) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.shopping_list_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity = requireActivity() as MainActivity

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sortStr = prefs.getString(mainActivity.getString(R.string.pref_shopping_list_sort),
                                      Sort.Color.toString())
        val initialSort = try { Sort.valueOf(sortStr!!) }
                          // If sortStr value doesn't match a Sort value
                          catch(e: IllegalArgumentException) { Sort.Color }
                          // If sortStr is null
                          catch(e: NullPointerException) { Sort.Color }
        recyclerView.finishInit(viewLifecycleOwner, mainActivity.shoppingListViewModel,
                                mainActivity.inventoryViewModel,
                                mainActivity.supportFragmentManager, initialSort)
        recyclerView.snackBarAnchor = mainActivity.fab

        darkGrayColor = ContextCompat.getColor(mainActivity, R.color.colorTextLightSecondary)
        lightGrayColor = ContextCompat.getColor(mainActivity, android.R.color.darker_gray)
        blackColor = ContextCompat.getColor(mainActivity, android.R.color.black)
        yellowColor = ContextCompat.getColor(mainActivity, R.color.checkoutButtonEnabledColor)
        checkoutButtonNormalText = getString(R.string.checkout_description)
        checkoutButtonConfirmText = getString(R.string.checkout_confirm_description)
        fabIconController = TwoStateAnimatedIconController.forFloatingActionButton(mainActivity.fab,
            ContextCompat.getDrawable(mainActivity, R.drawable.fab_animated_add_to_delete_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(mainActivity, R.drawable.fab_animated_delete_to_add_icon) as AnimatedVectorDrawable)

        recyclerView.selection.sizeLiveData.observe(viewLifecycleOwner, Observer { newSize ->
            if (newSize == 0) actionMode?.finish()
            else if (newSize > 0) {
                actionMode = actionMode ?: mainActivity.startSupportActionMode(actionModeCallback)
                actionMode?.title = getString(R.string.action_mode_title, newSize)
            }
        })
        recyclerView.checkedItems.sizeLiveData.observe(viewLifecycleOwner, Observer { newSize ->
            if (newSize > 0) checkoutButtonIsEnabled = true
            if (newSize == 0) {
                revertCheckoutButtonToNormalState()
                checkoutButtonIsEnabled = false
            }
        })
    }

    fun enable() {
        fabIconController.toStateA(animate = false)
        mainActivity.fab.setOnClickListener { recyclerView.addNewItem() }
        mainActivity.checkoutButton.setOnClickListener {
            if (!checkoutButtonIsEnabled) return@setOnClickListener
            val currentTime = System.currentTimeMillis()
            if (currentTime < checkoutButtonLastPressTimeStamp + 2000) {
                revertCheckoutButtonToNormalState()
                recyclerView.checkout()
            }
            else {
                checkoutButtonLastPressTimeStamp = currentTime
                mainActivity.checkoutButton.text = checkoutButtonConfirmText
                handler.removeCallbacks(::revertCheckoutButtonToNormalState)
                handler.postDelayed(::revertCheckoutButtonToNormalState, 2000)
            }
        }
        restoreRecyclerViewSelectionState()
    }

    fun disable() {
        saveRecyclerViewSelectionState()
        mainActivity.fab.setOnClickListener(null)
        mainActivity.checkoutButton.setOnClickListener(null)
        actionMode?.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (menu.size() == 0) inflater.inflate(R.menu.action_bar_menu, menu)
        this.menu = menu
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?): Boolean {
                recyclerView.searchFilter = newText
                return true
            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.setGroupVisible(R.id.shopping_list_view_menu_group, true)
        initOptionsMenuSort(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if (item.isChecked) return false
        return when (item.itemId) {
            R.id.delete_all_menu_item -> {
                recyclerView.deleteAll(); true
            } R.id.color_option -> {
                recyclerView.sort = Sort.Color
                item.isChecked = true; true
            } R.id.name_ascending_option -> {
                recyclerView.sort = Sort.NameAsc
                item.isChecked = true; true
            } R.id.name_descending_option -> {
                recyclerView.sort = Sort.NameDesc
                item.isChecked = true; true
            } R.id.amount_ascending_option -> {
                recyclerView.sort = Sort.AmountAsc
                item.isChecked = true; true
            } R.id.amount_descending_option -> {
                recyclerView.sort = Sort.AmountDesc
                item.isChecked = true; true
            } else -> mainActivity.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        disable()
        super.onSaveInstanceState(outState)
    }

    fun saveRecyclerViewSelectionState() {
        savedSelectionState = recyclerView.selection.currentState()
    }

    fun restoreRecyclerViewSelectionState() {
        savedSelectionState?.let { recyclerView.selection.restoreState(it) }
        savedSelectionState = null
    }

    private fun initOptionsMenuSort(menu: Menu) {
        menu.findItem(when (recyclerView.sort) {
            Sort.Color ->      R.id.color_option
            Sort.NameAsc ->    R.id.name_ascending_option
            Sort.NameDesc ->   R.id.name_descending_option
            Sort.AmountAsc ->  R.id.amount_ascending_option
            Sort.AmountDesc -> R.id.amount_descending_option
            else ->            R.id.color_option }).isChecked = true
    }

    private fun revertCheckoutButtonToNormalState() {
        mainActivity.checkoutButton.text = checkoutButtonNormalText
        checkoutButtonLastPressTimeStamp = 0
    }

    private fun enableCheckoutButton(enabling: Boolean) {
        if (checkoutButtonIsEnabled == enabling) return

        val bgColorAnim = ValueAnimator.ofArgb(if (enabling) lightGrayColor else yellowColor,
                                               if (enabling) yellowColor else lightGrayColor)
        bgColorAnim.addUpdateListener {
            mainActivity.checkout_button.backgroundTintList = ColorStateList.valueOf(it.animatedValue as Int)
        }
        bgColorAnim.duration = 200
        bgColorAnim.start()
        val textColorAnim = ObjectAnimator.ofArgb(mainActivity.checkout_button, "textColor",
                                                  if (enabling) blackColor else darkGrayColor)
        textColorAnim.duration = 200
        textColorAnim.start()
    }

    private val actionModeCallback = object: ActionMode.Callback {
        private var menu: Menu? = null

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.add_to_inventory_button -> {
                    recyclerView.apply{ addItemsToInventory(*selection.currentState()) }
                    true
                } else -> onOptionsItemSelected(item)
            }
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.action_bar_menu, menu)
            this.menu = menu
            menu?.setGroupVisible(R.id.shopping_list_view_menu_group, true)
            menu?.setGroupVisible(R.id.shopping_list_view_action_mode_menu_group, true)
            mainActivity.fab.setOnClickListener {
                recyclerView.deleteItems(*recyclerView.selection.currentState()) }
            fabIconController.toStateB()
            if (menu != null) initOptionsMenuSort(menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

        override fun onDestroyActionMode(mode: ActionMode?) {
            recyclerView.selection.clear()
            mainActivity.fab.setOnClickListener { recyclerView.addNewItem() }
            fabIconController.toStateA()
            initOptionsMenuSort(this@ShoppingListFragment.menu)
            actionMode = null
        }
    }
}

/*if (recyclerView.selection.isEmpty) return
        val context = this.context
        if (context != null) {
            val selectionStateFile = File(context.cacheDir, "inventory_selection_state")
            val writer = selectionStateFile.writer()
            for (id in recyclerView.selection.currentState())
                writer.write("$id,")
            writer.close()

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val prefsEditor = prefs.edit()
            val sortStr = if (recyclerView.sort != null) recyclerView.sort.toString()
            else                           Sort.Color.toString()
            prefsEditor.putString(context.getString(R.string.pref_inventory_sort), sortStr)
            prefsEditor.apply()
        }*/

/*val context = this.context
if (context != null) {
    val selectionStateFile = File(context.cacheDir, "inventory_selection_state")
    if (selectionStateFile.exists()) {
        val selectionStateString = selectionStateFile.readText().split(',')
        // size - 1 is to leave off the trailing comma
        val selectionState = IntArray(selectionStateString.size - 1) { i ->
            selectionStateString[i].toInt()
        }
        recyclerView.selection.restoreState(selectionState)
        selectionStateFile.delete()
    }
}*/