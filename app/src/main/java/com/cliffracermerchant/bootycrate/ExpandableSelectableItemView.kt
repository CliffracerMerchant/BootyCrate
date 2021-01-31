/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat

/** A ConstraintLayout subclass that provides an interface for a selectable and expandable view.
 *
 *  ExpandableSelectableItemView will display the information of an instance of
 *  ExpandableSelectableItem, while also providing an interface for expansion
 *  and selection. The function update will update the view to reflect the data
 *  contained in the instance of Entity passed to it. The default implementa-
 *  tion takes care of initialization of the selected and expanded states, so
 *  derived classes should always call the super implementation of update when
 *  defining their own.
 *
 *  The interface for selection and deselection consists of the functions
 *  select, deselect, and setSelectedState. These functions will give the view
 *  a surrounding gradient outline or hide the outline depending on the item's
 *  selection state.
 *
 *  Likewise, the interface for item expansion consists of expand, collapse,
 *  and setExpanded. While ExpandableSelectableItemView is not abstract, sub-
 *  classes will want to override setExpanded with an implementation that
 *  expands or collapses the appropriate child views. */
open class ExpandableSelectableItemView<Entity: ExpandableSelectableItem>(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(ContextThemeWrapper(context, R.style.RecyclerViewItemStyle), attrs) {
    val isExpanded get() = _isExpanded
    private var _isExpanded = false
    private val gradientOutline get() = ((background as? LayerDrawable)?.getDrawable(1) as? LayerDrawable)?.getDrawable(0) as? GradientDrawable

    init {
        gradientOutline?.setTintList(null)
        gradientOutline?.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        val colors = IntArray(5)
        colors[0] = ContextCompat.getColor(context, R.color.colorInBetweenPrimaryAccent2)
        colors[1] = ContextCompat.getColor(context, R.color.colorInBetweenPrimaryAccent1)
        colors[2] = ContextCompat.getColor(context, R.color.colorPrimary)
        colors[3] = colors[1]
        colors[4] = colors[0]
        gradientOutline?.colors = colors
    }

    open fun update(item: Entity) {
        setExpanded(item.isExpanded)
        setSelectedState(item.isSelected, animate = false)
    }

    fun expand() = setExpanded(true)
    fun collapse() = setExpanded(false)
    open fun setExpanded(expanded: Boolean = true) {
        _isExpanded = expanded
    }
    fun toggleExpanded() = if (isExpanded) collapse() else expand()

    fun select() = setSelectedState(true)
    fun deselect() = setSelectedState(false)
    fun setSelectedState(selected: Boolean, animate: Boolean = true) {
        if (animate) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
            ObjectAnimator.ofInt(gradientOutline, "alpha",
                                 if (selected) 0 else 255,
                                 if (selected) 255 else 0).apply {
                duration = 200L
                doOnEnd { setLayerType(LAYER_TYPE_NONE, null) }
                start()
            }
        }
        else gradientOutline?.alpha = if (selected) 255 else 0
    }
}