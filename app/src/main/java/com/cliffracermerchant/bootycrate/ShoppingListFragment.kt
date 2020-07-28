package com.cliffracermerchant.bootycrate

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.shopping_list_fragment_layout.recyclerView


class ShoppingListFragment : Fragment() {
    private var actionMode: ActionMode? = null
    private var savedSelectionState: IntArray? = null
    private lateinit var fabIconController: AnimatedVectorDrawableController
    private var checkoutButtonLastPressTimeStamp = 0L
    private val handler = Handler()
    private lateinit var checkoutButtonNormalText: String
    private lateinit var checkoutButtonConfirmText: String
    private lateinit var mainActivity: MainActivity
    private lateinit var menu: Menu
    private var actionModeStartedOnce = false

//    private companion object {
//        // Due to the target API level, the reset function of AnimatedVectorDrawable
//        // cannot be used. Tracking whether or not the action mode has been started
//        // at least once (and therefore whether or not the animated drawables used for
//        // the FAB have been animated yet) allows us to set the image drawable of the
//        // FAB accordingly.
//
//    }

    init { setHasOptionsMenu(true) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.shopping_list_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity = requireActivity() as MainActivity
        recyclerView.snackBarAnchor = mainActivity.fab
        recyclerView.fragmentManager = mainActivity.supportFragmentManager

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sortStr = prefs.getString(mainActivity.getString(R.string.pref_shopping_list_sort),
                                      Sort.Color.toString())
        val initialSort = try { Sort.valueOf(sortStr!!) }
                          catch(e: IllegalArgumentException) { Sort.Color } // If sortStr value doesn't match a Sort value
                          catch(e: NullPointerException) { Sort.Color } // If sortStr is null
        recyclerView.setViewModels(viewLifecycleOwner, mainActivity.shoppingListViewModel,
                                   mainActivity.inventoryViewModel, initialSort)

        checkoutButtonNormalText = getString(R.string.checkout_description)
        checkoutButtonConfirmText = getString(R.string.checkout_confirm_description)

        recyclerView.selection.sizeLiveData.observe(viewLifecycleOwner, Observer { newSize ->
            if (newSize == 0) actionMode?.finish()
            else if (newSize > 0) {
                actionMode = actionMode ?: mainActivity.startSupportActionMode(actionModeCallback)
                actionMode?.title = getString(R.string.action_mode_title, newSize)
            }
        })
        recyclerView.checkedItems.sizeLiveData.observe(viewLifecycleOwner, Observer { newSize ->
            if (newSize > 0) mainActivity.checkoutButtonIsEnabled = true
            if (newSize == 0) {
                revertCheckoutButtonToNormalState()
                mainActivity.checkoutButtonIsEnabled = false
            }
        })
    }

    fun revertCheckoutButtonToNormalState() {
        mainActivity.checkoutButton.text = checkoutButtonNormalText
        checkoutButtonLastPressTimeStamp = 0
    }

    fun enable() {
        fabIconController = AnimatedVectorDrawableController.forFloatingActionButton(
            mainActivity.fab,
            ContextCompat.getDrawable(mainActivity, R.drawable.fab_animated_add_to_delete_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(mainActivity, R.drawable.fab_animated_delete_to_add_icon) as AnimatedVectorDrawable)
        mainActivity.fab.setOnClickListener { recyclerView.addNewItem() }
        mainActivity.checkoutButton.setOnClickListener {
            if (!mainActivity.checkoutButtonIsEnabled) return@setOnClickListener
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
            Sort.Color -> R.id.color_option
            Sort.NameAsc -> R.id.name_ascending_option
            Sort.NameDesc -> R.id.name_descending_option
            Sort.AmountAsc -> R.id.amount_ascending_option
            Sort.AmountDesc -> R.id.amount_descending_option
            else -> R.id.color_option }).isChecked = true
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
            actionModeStartedOnce = true
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