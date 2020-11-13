/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout

/** A ConstraintLayout subclass that provides an interface for a selectable and expandable view.
 *
 *  ExpandableSelectableItemView is an abstract class that provides an inter-
 *  face for visual selection and deselection via the functions select(),
 *  deselect(), and setSelectedState. These functions will set the background
 *  color of the view to either the value of the attr colorSelected or null
 *  depending on the item's selection state. Likewise, the interface for item
 *  expansion consists of expand(), collapse(), and setExpanded. Because the
 *  class is abstract, subclasses will have to override setExpanded with an
 *  implementation that expands or collapses the appropriate child views.
 *
 *  Update will update the view to reflect the data contained in the instance
 *  of Entity passed to it. The default implementation takes care of initial-
 *  ization of the selected and expanded states, so derived classes should
 *  always call the super implementation of update when defining their own.*/
abstract class ExpandableSelectableItemView<Entity: ExpandableSelectableItem>(context: Context) :
    ConstraintLayout(ContextThemeWrapper(context, R.style.RecyclerViewItemStyle))
{
    private var selectedColor = 0
    private var itemNormalBackgroundColor = 0

    init {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorSelected, typedValue, true)
        selectedColor = typedValue.data
        context.theme.resolveAttribute(R.attr.recyclerViewItemColor, typedValue, true)
        itemNormalBackgroundColor = typedValue.data
    }

    open fun update(item: Entity) {
        setExpanded(item.isExpanded)
        setSelectedState(item.isSelected, animate = false)
    }

    fun expand() = setExpanded(true)
    fun collapse() = setExpanded(false)
    abstract fun setExpanded(expanded: Boolean = true)

    fun select() = setSelectedState(true)
    fun deselect() = setSelectedState(false)
    fun setSelectedState(selected: Boolean, animate: Boolean = true) {
        val bgColor = if (selected) selectedColor
                      else itemNormalBackgroundColor
        if (!animate) background.setTint(bgColor)
        else {
            val startColor = if (selected) itemNormalBackgroundColor
                             else          selectedColor
            ObjectAnimator.ofArgb(background, "tint", startColor, bgColor).start()
        }
    }
}