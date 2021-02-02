/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.integer_edit.view.*
import kotlinx.android.synthetic.main.viewmodel_item.view.*

/** A layout to display the contents of a shopping list item.
 *
 *  ShoppingListItemView is a ExpandableSelectableItemView subclass that
 *  inflates a layout resource to display the data of a ShoppingListItem
 *  instance. Its update override updates the contained views with the informa-
 *  tion of the provided ShoppingListItem. It also overrides the setExpanded
 *  function with an implementation that shows or hides the shopping list
 *  item's extra details. */
class ShoppingListItemView(context: Context, attrs: AttributeSet? = null) :
    ExpandableSelectableItemView<ShoppingListItem>(context, attrs)
{
    var color get() = checkBox.color
        set(value) { checkBox.color = value }
    var colorIndex get() = ViewModelItem.Colors.indexOf(color)
                   set(value) { val index = value.coerceIn(ViewModelItem.Colors.indices)
                                checkBox.color = ViewModelItem.Colors[index] }
    private val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
    private var itemIsLinked = false

    init {
        LayoutInflater.from(context).inflate(R.layout.viewmodel_item, this, true)
        editButton.setOnClickListener { toggleExpanded() }

        layoutTransition = defaultLayoutTransition()
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun update(item: ShoppingListItem) {
        nameEdit.setText(item.name)
        extraInfoEdit.setText(item.extraInfo)
        val colorIndex = item.color.coerceIn(ViewModelItem.Colors.indices)
        checkBox.setColor(ViewModelItem.Colors[colorIndex], animate = false)
        amountEdit.initValue(item.amount)
        itemIsLinked = item.linkedItemId != null
        checkBox.onCheckedChangedListener = null
        checkBox.isChecked = item.isChecked
        checkBox.onCheckedChangedListener = { checked -> setStrikeThroughEnabled(checked) }
        setStrikeThroughEnabled(checked = item.isChecked, animate = false)
        super.update(item)
    }

    override fun setExpanded(expanded: Boolean) {
        super.setExpanded(expanded)
        if (!expanded && nameEdit.isFocused || extraInfoEdit.isFocused ||
            amountEdit.valueEdit.isFocused)
                imm?.hideSoftInputFromWindow(windowToken, 0)

        checkBox.inColorEditMode = expanded
        nameEdit.isEditable = expanded
        if (extraInfoEdit.text.isNullOrBlank()) {
            // If extraInfoEdit is blank and is being expanded then we
            // can set its editable state before it becomes visible to
            // prevent needing to animate its change in editable state.
            if (expanded) extraInfoEdit.setEditable(editable = true, animate = false)
            extraInfoEdit.visibility = if (expanded) View.VISIBLE else View.GONE
        }
        else extraInfoEdit.isEditable = expanded
        amountEdit.valueIsDirectlyEditable = expanded
        editButton.isActivated = expanded
        amountEditSpacer.visibility = if (expanded) View.GONE else View.VISIBLE
        linkIndicator.visibility = if (expanded && itemIsLinked) View.VISIBLE else View.GONE
    }

    fun setStrikeThroughEnabled(checked: Boolean, animate: Boolean = true) {
        nameEdit.setStrikeThroughEnabled(checked, animate)
        extraInfoEdit.setStrikeThroughEnabled(checked, animate)
    }
}