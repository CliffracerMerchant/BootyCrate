package com.cliffracermerchant.bootycrate

import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.inventory_view_fragment_layout.recyclerView
import java.io.File

class InventoryFragment : Fragment() {
    var actionMode: ActionMode? = null
    private var savedSelectionState: IntArray? = null
    private lateinit var fabIconController: AnimatedVectorDrawableController
    private lateinit var mainActivity: MainActivity
    private lateinit var menu: Menu

    private companion object {
        // Due to the target API level, the reset function of AnimatedVectorDrawable
        // cannot be used. Tracking whether or not the action mode has been started
        // at least once (and therefore whether or not the animated drawables used for
        // the FAB have been animated yet) allows us to set the image drawable of the
        // FAB accordingly.
        var actionModeStartedOnce = false
    }

    init { setHasOptionsMenu(true) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.inventory_view_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity = requireActivity() as MainActivity
        recyclerView.snackBarAnchor = mainActivity.fab
        recyclerView.fragmentManager = mainActivity.supportFragmentManager

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val sortStr = prefs.getString(mainActivity.getString(R.string.pref_inventory_sort),
                                      Sort.Color.toString())
        val initialSort = try { Sort.valueOf(sortStr!!) }
                          catch(e: IllegalArgumentException) { Sort.Color } // If sortStr value doesn't match a Sort value
                          catch(e: NullPointerException) { Sort.Color } // If sortStr is null
        recyclerView.setViewModels(viewLifecycleOwner, mainActivity.inventoryViewModel,
                                   mainActivity.shoppingListViewModel, initialSort)

        recyclerView.selection.sizeLiveData.observe(viewLifecycleOwner, Observer { newSize ->
            if (newSize == 0 && actionMode != null) actionMode?.finish()
            else if (newSize > 0) {
                actionMode = actionMode ?: mainActivity.startSupportActionMode(actionModeCallback)
                actionMode?.title = getString(R.string.action_mode_title, newSize)
            }
        })
    }

    fun enable() {
        fabIconController = AnimatedVectorDrawableController(mainActivity.fab,
            ContextCompat.getDrawable(mainActivity, R.drawable.fab_animated_add_to_delete_icon) as AnimatedVectorDrawable,
            ContextCompat.getDrawable(mainActivity, R.drawable.fab_animated_delete_to_add_icon) as AnimatedVectorDrawable)
        mainActivity.fab.setOnClickListener { recyclerView.addNewItem() }
        restoreRecyclerViewSelectionState()
    }

    fun disable() {
        saveRecyclerViewSelectionState()
        mainActivity.fab.setOnClickListener(null)
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
        menu.setGroupVisible(R.id.inventory_view_menu_group, true)
        initOptionsMenuSort(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
            } else -> super.onOptionsItemSelected(item)
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

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.add_to_shopping_list_button -> {
                    recyclerView.apply{ addItemsToShoppingList(*selection.currentState()) }
                    true
                }
                else -> onOptionsItemSelected(item!!)
            }
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            actionModeStartedOnce = true
            mode?.menuInflater?.inflate(R.menu.action_bar_menu, menu)
            this.menu = menu
            menu?.setGroupVisible(R.id.inventory_view_menu_group, true)
            menu?.setGroupVisible(R.id.inventory_view_action_mode_group, true)
            mainActivity.fab.setOnClickListener {
                recyclerView.apply{ deleteItems(*selection.currentState()) }}
            fabIconController.toStateB()

            if (menu != null) initOptionsMenuSort(menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = true

        override fun onDestroyActionMode(mode: ActionMode?) {
            recyclerView.selection.clear()
            //menu?.setGroupVisible(R.id.inventory_view_action_mode_group, false)
            mainActivity.fab.setOnClickListener { recyclerView.addNewItem() }
            fabIconController.toStateA()
            initOptionsMenuSort(this@InventoryFragment.menu)
            actionMode = null
        }
    }
}
