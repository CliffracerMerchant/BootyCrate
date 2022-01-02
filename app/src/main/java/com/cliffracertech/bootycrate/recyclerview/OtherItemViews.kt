/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewPropertyAnimator
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.ListItem
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.databinding.ListItemBinding
import com.cliffracertech.bootycrate.databinding.InventoryItemBinding
import com.cliffracertech.bootycrate.databinding.InventoryItemDetailsBinding
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.applyConfig
import com.cliffracertech.bootycrate.utils.setPadding

/**
 * A layout to display the data for a ListItem.
 *
 * ListItemView displays the data for an instance of ListItem. The displayed
 * data can be updated for a new item with the open function update. The
 * function updateContentDescriptions likewise updates the contentDescription
 * fields for child views so that they are unique for each item. Both update
 * and updateContentDescriptions are open functions and should be overridden
 * in subclasses that add new fields to the displayed items.
 *
 * By default ListItemView inflates itself with the contents of R.layout.list_item.xml
 * and initializes its ListItemBinding member ui. In case this layout needs to
 * be overridden in a subclass, the ListItemView can be constructed with the
 * parameter useDefaultLayout equal to false. If useDefaultLayout is false, it
 * will be up to the subclass to inflate the desired layout and initialize the
 * member ui with an instance of a ListItemBinding. If the ui member is not
 * initialized, a kotlin.UninitializedPropertyAccessException will be thrown.
 */
@SuppressLint("ViewConstructor")
open class ListItemView<T: ListItem>(
    context: Context,
    useDefaultLayout: Boolean = true,
) : ConstraintLayout(context) {

    lateinit var ui: ListItemBinding

    init {
        if (useDefaultLayout) {
            ui = ListItemBinding.inflate(LayoutInflater.from(context), this)
            // Initializing isVisible to false here ensures that the extraInfoEdit
            // is hidden even if the first bound item's extraInfo field is blank.
            // If the first bound item's extraInfo field is not blank, then the
            // extraInfoEdit's visibility will be set in the call to setExtraInfoText.
            ui.extraInfoEdit.isVisible = false
            ui.extraInfoEdit.alpha = 0f
        }
        background = ContextCompat.getDrawable(context, R.drawable.list_item)
        val verticalPadding = resources.getDimension(R.dimen.recycler_view_item_vertical_padding)
        setPadding(top = (verticalPadding * 2f / 3f).toInt(),
                   bottom = (verticalPadding * 4f / 3f).toInt())
    }

    @CallSuper open fun update(item: T) {
        ui.nameEdit.setText(item.name)
        setExtraInfoText(item.extraInfo)
        val colorIndex = item.color.coerceIn(ListItem.Colors.indices)
        ui.checkBox.initColorIndex(colorIndex)
        ui.amountEdit.initValue(item.amount)
        isSelected = item.isSelected
        updateContentDescriptions(item.name)
    }

    @CallSuper open fun updateContentDescriptions(itemName: String) {
        ui.checkBox.checkBoxContentDescription =
            context.getString(R.string.item_checkbox_description, itemName)
        ui.checkBox.editColorContentDescription =
            context.getString(R.string.edit_item_color_description, itemName)
        ui.amountEdit.ui.decreaseButton.contentDescription = context.getString(R.string.item_amount_decrease_description, itemName)
        ui.amountEdit.ui.increaseButton.contentDescription = context.getString(R.string.item_amount_increase_description, itemName)
        ui.amountEditLabel.text = context.getString(R.string.item_amount_description, itemName)
        ui.extraInfoEdit.hint = context.getString(R.string.item_extra_info_description, itemName)
    }

    fun select() { isSelected = true }
    fun deselect() { isSelected = false }

    /** Update the text of name edit, while also updating the contentDescriptions
     * of child views to take into account the new name. */
    fun setNameText(newName: String) {
        if (newName == ui.nameEdit.text.toString()) return
        ui.nameEdit.setText(newName)
        updateContentDescriptions(newName)
    }

    /** Update the text of the extra info edit, while also updating the extra info
     * edit's visibility, if needed, to account for the new text. It is recommended
     * to use this function rather than changing the extra info edit's text directly
     * to ensure that its visibility is set correctly. */
    fun setExtraInfoText(newText: String) {
        if (newText == ui.extraInfoEdit.text.toString()) return
        ui.extraInfoEdit.setText(newText)
        if (ui.extraInfoEdit.text.isNullOrBlank() == ui.extraInfoEdit.isVisible) {
            ui.extraInfoEdit.isVisible = !ui.extraInfoEdit.isVisible
            ui.extraInfoEdit.alpha = if (ui.extraInfoEdit.isVisible) 1f else 0f
        }
    }
}



/**
 * An ExpandableItemView to display the contents of a shopping list item.
 *
 * ShoppingListItemView is a ExpandableItemView subclass that displays the data
 * of a ShoppingListItem instance. It has an update override that updates the
 * check state of the checkbox, it overrides the setExpanded function with an
 * implementation that toggles the checkbox between its normal checkbox mode
 * and its color edit mode, and it has a convenience method setStrikeThroughEnabled
 * that will set the strike through state for both the name and extra info edit
 * at the same time.
 */
@SuppressLint("ViewConstructor")
class ShoppingListItemView(context: Context, animatorConfig: AnimatorConfig? = null) :
    ExpandableItemView<ShoppingListItem>(context, animatorConfig)
{
    init { ui.checkBox.onCheckedChangedListener = ::setStrikeThroughEnabled }

    override fun update(item: ShoppingListItem) {
        ui.checkBox.initIsChecked(item.isChecked)
        setStrikeThroughEnabled(enabled = item.isChecked, animate = false)
        super.update(item)
    }

    override fun onExpandedChanged(expanded: Boolean, animate: Boolean) {
        super.onExpandedChanged(expanded, animate)
        ui.checkBox.setInColorEditMode(expanded, animate)
    }

    fun setStrikeThroughEnabled(enabled: Boolean) = setStrikeThroughEnabled(enabled, true)
    private fun setStrikeThroughEnabled(enabled: Boolean, animate: Boolean) {
        ui.nameEdit.setStrikeThroughEnabled(enabled, animate)
        ui.extraInfoEdit.setStrikeThroughEnabled(enabled, animate)
    }
}



/**
 * An ExpandableItemView to display the contents of an inventory item.
 *
 * InventoryItemView is a ExpandableItemView subclass that displays the data of
 * an InventoryItem instance. It has an update override for the extra fields
 * that InventoryItem adds to its parent class, and has a onExpandedChanged
 * override that also shows or hides these extra fields.
 */
@SuppressLint("ViewConstructor")
class InventoryItemView(context: Context, animatorConfig: AnimatorConfig? = null) :
    ExpandableItemView<InventoryItem>(context, animatorConfig, useDefaultLayout = false)
{
    val detailsUi: InventoryItemDetailsBinding
    private var pendingDetailsAnimation: ViewPropertyAnimator? = null

    init {
        val tempUi = InventoryItemBinding.inflate(LayoutInflater.from(context), this)
        ui = ListItemBinding.bind(tempUi.root)
        detailsUi = InventoryItemDetailsBinding.bind(tempUi.root)

        ui.extraInfoEdit.isVisible = false
        ui.extraInfoEdit.alpha = 0f
        detailsUi.inventoryItemDetailsLayout.isVisible = false
        detailsUi.inventoryItemDetailsLayout.alpha = 0f

        ui.editButton.setOnClickListener { toggleExpanded() }
        ui.checkBox.setInColorEditMode(true, animate = false)
        ui.amountEdit.minValue = 0
        this.animatorConfig = animatorConfig
    }

    override fun update(item: InventoryItem) {
        super.update(item)
        detailsUi.autoAddToShoppingListCheckBox.initIsChecked(item.autoAddToShoppingList)
        detailsUi.autoAddToShoppingListCheckBox.initColorIndex(item.color)
        detailsUi.autoAddToShoppingListAmountEdit.initValue(item.autoAddToShoppingListAmount)
    }

    override fun updateContentDescriptions(itemName: String) {
        super.updateContentDescriptions(itemName)
        detailsUi.autoAddToShoppingListCheckBox.checkBoxContentDescription =
            context.getString(R.string.item_auto_add_to_shopping_list_checkbox_description, itemName)
        detailsUi.autoAddToShoppingListAmountEdit.ui.decreaseButton.contentDescription =
            context.getString(R.string.item_auto_add_to_shopping_list_amount_decrease_description, itemName)
        detailsUi.autoAddToShoppingListAmountEdit.ui.increaseButton.contentDescription =
            context.getString(R.string.item_auto_add_to_shopping_list_amount_increase_description, itemName)
        detailsUi.autoAddToShoppingListAmountEdit.contentDescription =
            context.getString(R.string.item_auto_add_to_shopping_list_amount_description, itemName)
    }

    private var showingDetails = false
    override fun onExpandedChanged(expanded: Boolean, animate: Boolean) {
        super.onExpandedChanged(expanded, animate)
        val endTranslation = if (expanded) 0f else
            detailsUi.autoAddToShoppingListCheckBox.layoutParams.height * -1f

        detailsUi.inventoryItemDetailsLayout.apply {
            showingDetails = expanded
            if (!animate) {
                isVisible = expanded
                alpha = if (expanded) 1f else 0f
                translationY = endTranslation
            } else {
                isVisible = true
                if (!expanded) this@InventoryItemView.overlay.add(this)
                val anim = animate().applyConfig(animatorConfig).withLayer()
                                    .alpha(if (expanded) 1f else 0f)
                                    .translationY(endTranslation)
                if (!expanded) anim.withEndAction {
                    if (!showingDetails && alpha == 0f) {
                        isVisible = false
                        this@InventoryItemView.overlay.remove(this)
                        this@InventoryItemView.addView(this)
                    }
                }
                pendingDetailsAnimation = anim
            }
        }
    }

    override fun runPendingAnimations() {
        super.runPendingAnimations()
        pendingDetailsAnimation?.start()
        pendingDetailsAnimation = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // The details layout might be in the overlay when this measure occurs.
        // If this occurs when the extra details layout is fading in, we still
        // want its height to count towards the height of the entire item view.
        if (heightMeasureSpec != MeasureSpec.UNSPECIFIED || !showingDetails) {
            layoutParams.height = LayoutParams.WRAP_CONTENT
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else if (detailsUi.inventoryItemDetailsLayout.parent !== this) {
            val detailsHeight = detailsUi.inventoryItemDetailsLayout.measuredHeight
            setMeasuredDimension(measuredWidth, measuredHeight + detailsHeight)
            layoutParams.height = measuredHeight
        }
    }
}