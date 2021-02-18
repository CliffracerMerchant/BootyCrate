/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cliffracermerchant.bootycrate.databinding.InventoryItemBinding
import com.cliffracermerchant.bootycrate.databinding.InventoryItemDetailsBinding
import com.cliffracermerchant.bootycrate.databinding.ViewModelItemBinding

/**
 * A layout to display the data for a ViewModelItem.
 *
 * ViewModelItemView displays the data for an instance of ViewModelItem. The
 * displayed data can be updated for a new item with the function update.
 *
 * By default ViewModelItemView inflates itself with the contents of R.layout.-
 * view_model_item_view.xml and initializes its ViewModelItemBinding member ui.
 * In case this layout needs to be overridden in a subclass, the ViewModelItem-
 * View can be constructed with the parameter useDefaultLayout equal to false.
 * If useDefaultLayout is false, it will be up to the subclass to inflate the
 * desired layout and initialize the member ui with an instance of a ViewModel-
 * ItemBinding. If the ui member is not initialized then a kotlin.Uninitialized-
 * PropertyAccessException will be thrown.
 */
@Suppress("LeakingThis")
@SuppressLint("ViewConstructor")
open class ViewModelItemView<Entity: ViewModelItem>(
    context: Context,
    useDefaultLayout: Boolean = true,
) : ConstraintLayout(context) {

    protected val inputMethodManager = inputMethodManager(context)
    protected var itemIsLinked = false

    lateinit var ui: ViewModelItemBinding

    init {
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
        if (useDefaultLayout)
            ui = ViewModelItemBinding.inflate(LayoutInflater.from(context), this)
    }

    @CallSuper open fun update(item: Entity) {
        ui.nameEdit.setText(item.name)
        ui.extraInfoEdit.setText(item.extraInfo)
        val colorIndex = item.color.coerceIn(ViewModelItem.Colors.indices)
        ui.checkBox.setColorIndex(colorIndex, animate = false)
        ui.amountEdit.initValue(item.amount)
        itemIsLinked = item.linkedItemId != null
    }
}

/**
 * A ViewModelItemView subclass that provides an interface for a selection and expansion of the view.
 *
 * ExpandableSelectableItemView will display the information of an instance of
 * ExpandableSelectableItem, while also providing an interface for expansion
 * and selection. The update override will update the view to reflect the
 * selection and expansion state of the ExpandableSelectableItem passed to it.
 *
 * The interface for selection and deselection consists of the functions
 * select, deselect, and setSelectedState. With the default background these
 * functions will give the view a surrounding gradient outline or hide the
 * outline depending on the item's selection state.
 *
 * Likewise, the interface for item expansion consists of expand, collapse,
 * setExpanded, and toggleExpanded. Because subclasses may need to alter
 * the visibility of additional views during expansion or collapse, setExpan-
 * ded is open.
 */
@Suppress("LeakingThis")
@SuppressLint("ViewConstructor")
open class ExpandableSelectableItemView<Entity: ExpandableSelectableItem>(
    context: Context,
    animatorConfig: AnimatorConfigs.Config = AnimatorConfigs.translation,
    useDefaultLayout: Boolean = true,
) : ViewModelItemView<Entity>(context, useDefaultLayout) {

    val isExpanded get() = _isExpanded
    private var _isExpanded = false
    private val gradientOutline: GradientDrawable

    init {
        layoutTransition = layoutTransition(animatorConfig)
        val background = ContextCompat.getDrawable(context, R.drawable.recycler_view_item_background) as LayerDrawable
        gradientOutline = (background.getDrawable(1) as LayerDrawable).getDrawable(0) as GradientDrawable
        gradientOutline.setTintList(null)
        gradientOutline.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        val colors = IntArray(5)
        colors[0] = ContextCompat.getColor(context, R.color.colorInBetweenPrimaryAccent2)
        colors[1] = ContextCompat.getColor(context, R.color.colorInBetweenPrimaryAccent1)
        colors[2] = ContextCompat.getColor(context, R.color.colorPrimary)
        colors[3] = colors[1]
        colors[4] = colors[0]
        gradientOutline.colors = colors
        this.background = background

        val verticalPadding = resources.getDimensionPixelSize(R.dimen.recycler_view_item_vertical_padding)
        setPadding(0, verticalPadding, 0, verticalPadding)

        if (useDefaultLayout)
            ui.editButton.setOnClickListener { toggleExpanded() }
    }

    override fun update(item: Entity) {
        super.update(item)
        setExpanded(item.isExpanded)
        setSelectedState(item.isSelected, animate = false)
    }

    fun expand() = setExpanded(true)
    fun collapse() = setExpanded(false)
    fun toggleExpanded() = if (isExpanded) collapse() else expand()

    @CallSuper open fun setExpanded(expanded: Boolean = true) {
        _isExpanded = expanded
        if (!expanded &&
            ui.nameEdit.isFocused ||
            ui.extraInfoEdit.isFocused ||
            ui.amountEdit.ui.valueEdit.isFocused)
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)

        ui.nameEdit.isEditable = expanded
        ui.amountEdit.valueIsDirectlyEditable = expanded
        if (ui.extraInfoEdit.text.isNullOrBlank()) {
            // If extraInfoEdit is blank and is being expanded then we
            // can set its editable state before it becomes visible to
            // prevent needing to animate its change in editable state.
            if (expanded) ui.extraInfoEdit.setEditable(editable = true, animate = false)
            ui.extraInfoEdit.isVisible = expanded
        }
        else ui.extraInfoEdit.isEditable = expanded
        ui.editButton.isActivated = expanded
        ui.amountEditSpacer.isVisible = !expanded
        ui.linkIndicator.isVisible = expanded && itemIsLinked
    }

    fun select() = setSelectedState(true)
    fun deselect() = setSelectedState(false)
    fun setSelectedState(selected: Boolean, animate: Boolean = true) {
        if (animate) valueAnimatorOfInt(gradientOutline::setAlpha,
                                        if (selected) 0 else 255,
                                        if (selected) 255 else 0)
                                        .apply { duration = 200L }
                                        .start()
        else gradientOutline.alpha = if (selected) 255 else 0
    }
}

/**
 * An ExpandableSelectableItemView to display the contents of a shopping list item.
 *
 * ShoppingListItemView is a ExpandableSelectableItemView subclass that dis-
 * plays the data of a ShoppingListItem instance. It has an update override
 * that updates the check state of the checkbox, it overrides the setExpanded
 * function with an implementation that toggles the checkbox between its nor-
 * mal checkbox mode and its color edit mode, and it has a convenience method
 * setStrikeThroughEnabled that will set the strike through state for both the
 * name and extra info edit at the same time.
 */
class ShoppingListItemView(context: Context) :
    ExpandableSelectableItemView<ShoppingListItem>(context, AnimatorConfigs.shoppingListItem)
{
    override fun update(item: ShoppingListItem) {
        ui.checkBox.initIsChecked(item.isChecked)
        setStrikeThroughEnabled(enabled = item.isChecked, animate = false)
        super.update(item)
    }

    override fun setExpanded(expanded: Boolean) {
        super.setExpanded(expanded)
        ui.checkBox.inColorEditMode = expanded
    }

    fun setStrikeThroughEnabled(enabled: Boolean, animate: Boolean = true) {
        ui.nameEdit.setStrikeThroughEnabled(enabled, animate)
        ui.extraInfoEdit.setStrikeThroughEnabled(enabled, animate)
    }
}

/**
 * An ExpandableSelectableItemView to display the contents of an inventory item.
 *
 * InventoryItemView is a ExpandableSelectableItemView subclass that displays
 * the data of an InventoryItem instance. It has an update override for the
 * extra fields that InventoryItem adds to its parent class, and has a set-
 * Expanded override that also shows or hides these extra fields.
 */
class InventoryItemView(context: Context) :
    ExpandableSelectableItemView<InventoryItem>(context, useDefaultLayout = false)
{
    val detailsUi: InventoryItemDetailsBinding

    init {
        val tempUi = InventoryItemBinding.inflate(LayoutInflater.from(context), this)
        ui = ViewModelItemBinding.bind(tempUi.root)
        detailsUi = InventoryItemDetailsBinding.bind(tempUi.root)
        ui.editButton.setOnClickListener { toggleExpanded() }
        ui.checkBox.inColorEditMode = true
        ui.amountEdit.minValue = 0
    }

    override fun update(item: InventoryItem) {
        detailsUi.addToShoppingListCheckBox.isChecked = item.addToShoppingList
        detailsUi.addToShoppingListTriggerEdit.initValue(item.addToShoppingListTrigger)
        super.update(item)
    }

    override fun setExpanded(expanded: Boolean) {
        super.setExpanded(expanded)
        if (!expanded && detailsUi.addToShoppingListTriggerEdit.ui.valueEdit.isFocused)
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
        detailsUi.inventoryItemDetailsGroup.isVisible = expanded
    }
}