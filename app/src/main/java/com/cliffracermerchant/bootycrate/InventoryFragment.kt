package com.cliffracermerchant.bootycrate

import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.inventory_view_fragment_layout.recyclerView
import java.io.File

class InventoryFragment : Fragment() {

    companion object {
        val instance = InventoryFragment()
        // This is used to make sure that the selection state
        // is not restored on a fresh restart of the app
        var ranOnce = false
    }
    private var deleteToAddIcon: Drawable? = null
    private var addToDeleteIcon: Drawable? = null
    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: InventoryViewModel

    init { setHasOptionsMenu(true) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.inventory_view_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity = requireActivity() as MainActivity
        recyclerView.setViewModels(viewLifecycleOwner, mainActivity.inventoryViewModel,
                                   mainActivity.shoppingListViewModel)
        recyclerView.snackBarAnchor = mainActivity.bottom_app_bar

        addToDeleteIcon = mainActivity.getDrawable(R.drawable.fab_animated_delete_to_add_icon) as AnimatedVectorDrawable
        deleteToAddIcon = mainActivity.getDrawable(R.drawable.fab_animated_add_to_delete_icon) as AnimatedVectorDrawable
        mainActivity.fab.setImageDrawable(deleteToAddIcon)
        mainActivity.fab.setOnClickListener { recyclerView.addNewItem() }

        recyclerView.selection.sizeLiveData.observe(viewLifecycleOwner, Observer { newSize ->
            if (newSize == 0 && mainActivity.actionMode != null) mainActivity.actionMode?.finish()
            else if (newSize > 0) {
                if (mainActivity.actionMode == null)
                    mainActivity.actionMode = mainActivity.startSupportActionMode(actionModeCallback)
                mainActivity.actionMode?.title = getString(R.string.action_mode_title, newSize)
            }
        })

        if (!ranOnce) { ranOnce = true; return }
        val selectionStateFile = File(mainActivity.cacheDir, "inventory_selection_state")
        if (selectionStateFile.exists()) {
            val selectionStateString = selectionStateFile.readText().split(',')
            // size - 1 is to leave off the trailing comma
            val selectionState = IntArray(selectionStateString.size - 1) { i ->
                selectionStateString[i].toInt() }
            recyclerView.selection.restoreState(selectionState)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.setGroupVisible(R.id.inventory_view_menu_group, true)
        menu.setGroupVisible(R.id.shopping_list_view_menu_group, false)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val selectionStateFile = File(mainActivity.cacheDir, "inventory_selection_state")
        val writer = selectionStateFile.writer()
        for (id in recyclerView.selection.currentState())
            writer.write("$id,")
        writer.close()
        super.onSaveInstanceState(outState)
    }

    private val actionModeCallback = object: ActionMode.Callback {
        private var menu: Menu? = null

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.change_sorting_button -> true
                R.id.add_to_shopping_list_button -> {
                    recyclerView.apply{ addItemsToShoppingList(*selection.currentState()) }
                    true
                }
                else -> true
            }
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.action_bar_menu, menu)
            this.menu = menu
            menu?.setGroupVisible(R.id.inventory_view_menu_group, true)
            menu?.setGroupVisible(R.id.inventory_view_action_mode_group, true)
            mainActivity.fab.setOnClickListener {
                recyclerView.apply{ deleteItems(*selection.currentState()) }}
            mainActivity.fab.setImageDrawable(deleteToAddIcon)
            (mainActivity.fab.drawable as AnimatedVectorDrawable).start()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = true

        override fun onDestroyActionMode(mode: ActionMode?) {
            recyclerView.selection.clear()
            menu?.setGroupVisible(R.id.inventory_view_action_mode_group, false)
            mainActivity.fab.setOnClickListener { recyclerView.addNewItem() }
            mainActivity.fab.setImageDrawable(addToDeleteIcon)
            (mainActivity.fab.drawable as AnimatedVectorDrawable).start()
            mainActivity.actionMode = null
        }
    }

}
