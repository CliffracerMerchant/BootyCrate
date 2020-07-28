package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.LayerDrawable
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.widget.doAfterTextChanged
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlinx.android.synthetic.main.integer_edit_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_details_layout.view.*
import kotlinx.android.synthetic.main.shopping_list_item_layout.view.*

/**     ShoppingListItemView is a ConstraintLayout that displays the data of an
 *  ShoppingListItem instance. Its update(ShoppingListItem) function updates
 *  the contained views with the information of the ShoppingListItem instance.
 *  Its expand and collapse functions allow for an optional animation. */

class ShoppingListItemView(context: Context) : ConstraintLayout(context) {
    val isExpanded get() = expandedPrivate
    private var expandedPrivate = false
    private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    val decreaseButtonIconController: AnimatedVectorDrawableController
    val increaseButtonIconController: AnimatedVectorDrawableController
    val editButtonIconController: AnimatedVectorDrawableController
    val checkBoxBackgroundController: AnimatedVectorDrawableController
    val checkBoxCheckmarkController: AnimatedVectorDrawableController
    private val linkedItemDescriptionString = context.getString(R.string.linked_shopping_list_item_description)
    private val unlinkedItemDescriptionString = context.getString(R.string.unlinked_shopping_list_item_description)
    private val linkNowActionString = context.getString(R.string.shopping_list_item_link_now_action_description)
    private val changeLinkActionString = context.getString(R.string.shopping_list_item_change_link_action_description)
    private var normalTextColor = 0
    var itemColor: Int? = null

    /* This companion object stores the heights of the layout when expanded/
       collapsed. This prevents the expand/collapse animations from having to
       calculate these values every time the animation is started. */
    private companion object {
        var collapsedHeight = 0
        var expandedHeight = 0
        var increaseButtonFullWidth = 0
    }

    init {
        inflate(context, R.layout.shopping_list_item_layout, this)
        linkedToEdit.paintFlags = linkedToEdit.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        normalTextColor = nameEdit.currentTextColor

        decreaseButtonIconController = AnimatedVectorDrawableController.forView(
            amountOnListEdit.decreaseButton,
            context.getDrawable(R.drawable.shopping_list_animated_multiply_to_minus_icon) as AnimatedVectorDrawable,
            context.getDrawable(R.drawable.shopping_list_animated_minus_to_multiply_icon) as AnimatedVectorDrawable)
        increaseButtonIconController = AnimatedVectorDrawableController.forView(
            amountOnListEdit.increaseButton,
            context.getDrawable(R.drawable.animated_blank_to_plus_icon) as AnimatedVectorDrawable,
            context.getDrawable(R.drawable.animated_plus_to_blank_icon) as AnimatedVectorDrawable)
        editButtonIconController = AnimatedVectorDrawableController.forView(
            editButton,
            context.getDrawable(R.drawable.animated_edit_to_more_options_icon) as AnimatedVectorDrawable,
            context.getDrawable(R.drawable.animated_more_options_to_edit_icon) as AnimatedVectorDrawable)
        checkBoxBackgroundController = AnimatedVectorDrawableController.forDrawableLayer(
            checkBox.background as LayerDrawable, R.id.checkBoxBackground,
            context.getDrawable(R.drawable.animated_checkbox_unchecked_to_checked_background) as AnimatedVectorDrawable,
            context.getDrawable(R.drawable.animated_checkbox_checked_to_unchecked_background) as AnimatedVectorDrawable)
        checkBoxCheckmarkController = AnimatedVectorDrawableController.forDrawableLayer(
            checkBox.background as LayerDrawable, R.id.checkBoxCheckmark,
            context.getDrawable(R.drawable.animated_checkbox_unchecked_to_checked_checkmark) as AnimatedVectorDrawable,
            context.getDrawable(R.drawable.animated_checkbox_checked_to_unchecked_checkmark) as AnimatedVectorDrawable)

        editButton.setOnClickListener {
            if (isExpanded) //TODO: Implement more options menu
            else            expand()
        }
        collapseButton.setOnClickListener { collapse() }
        amountOnListEdit.decreaseButton.setOnClickListener { if (isExpanded) amountOnListEdit.decrement() }
        amountOnListEdit.increaseButton.setOnClickListener { if (isExpanded) amountOnListEdit.increment() }
        checkBox.setOnCheckedChangeListener { _, checked -> defaultOnCheckedChange(checked) }
        amountOnListEdit.increaseButton.layoutParams.apply { width /= 2 }

        // If the layout's children are clipped, they will suddenly appear or
        // disappear without an animation during the expand collapse animation
        clipChildren = false
    }

    fun update(item: ShoppingListItem, isExpanded: Boolean = false) {
        Log.d("update", "shoppingListItemView updated")
        collapse(false)
        nameEdit.setText(item.name)
        extraInfoEdit.setText(item.extraInfo)
        amountOnListEdit.initCurrentValue(item.amountOnList)
        amountInCartEdit.initCurrentValue(item.amountInCart)
        itemColor = item.color
        checkBoxBackgroundController.tint = item.color
        checkBox.isChecked = item.isChecked

        updateLinkedStatus(item.linkedInventoryItemId)
        if (isExpanded) expand(false)
        else            collapse(false)
    }

    fun updateLinkedStatus(newLinkedId: Long?) {
        if (newLinkedId != null) {
            linkedToIndicator.text = linkedItemDescriptionString
            linkedToEdit.text = changeLinkActionString
            amountInCartLabel.visibility = View.VISIBLE
            //amountInCartEdit.visibility = View.VISIBLE
            //For some reason setting the visibility of amountInCartEdit directly doesn't work
            amountInCartEdit.valueEdit.visibility = View.VISIBLE
            amountInCartEdit.decreaseButton.visibility = View.VISIBLE
            amountInCartEdit.increaseButton.visibility = View.VISIBLE
        } else {
            linkedToIndicator.text = unlinkedItemDescriptionString
            linkedToEdit.text = linkNowActionString
            amountInCartLabel.visibility = View.INVISIBLE
            //amountInCartEdit.visibility = View.INVISIBLE
            amountInCartEdit.valueEdit.visibility = View.INVISIBLE
            amountInCartEdit.decreaseButton.visibility = View.INVISIBLE
            amountInCartEdit.increaseButton.visibility = View.INVISIBLE
        }
    }

    fun expand(animate: Boolean = true) {
        if (expandedPrivate) return
        expandedPrivate = true
        nameEdit.isEditable = true
        amountOnListEdit.isEditable = true
        extraInfoEdit.isEditable = true
        amountInCartEdit.isEditable = true
        decreaseButtonIconController.toggleState(animate)
        increaseButtonIconController.toggleState(animate)
        editButtonIconController.toggleState(animate)

        if (animate) {
            val anim = expandCollapseAnimation(true)
            anim.doOnEnd { shoppingListItemDetailsInclude.visibility = View.VISIBLE }
            anim.start()
        } else {
            shoppingListItemDetailsInclude.visibility = View.VISIBLE
            amountOnListEdit.increaseButton.layoutParams.apply{ width = increaseButtonFullWidth }
        }
    }

    fun collapse(animate: Boolean = true) {
        if (!expandedPrivate) return
        if (nameEdit.isFocused || extraInfoEdit.isFocused ||
            amountOnListEdit.valueEdit.isFocused ||
            amountInCartEdit.valueEdit.isFocused)
                imm?.hideSoftInputFromWindow(windowToken, 0)
        expandedPrivate = false
        nameEdit.isEditable = false
        amountOnListEdit.isEditable = false
        extraInfoEdit.isEditable = false
        amountInCartEdit.isEditable = false
        decreaseButtonIconController.toStateA(animate)
        increaseButtonIconController.toStateA(animate)
        editButtonIconController.toStateA(animate)

        if (animate) {
            val anim = expandCollapseAnimation(false)
            anim.doOnEnd { shoppingListItemDetailsInclude.visibility = View.GONE }
            anim.start()
        } else {
            shoppingListItemDetailsInclude.visibility = View.GONE
            amountOnListEdit.increaseButton.layoutParams.apply { width = increaseButtonFullWidth / 2 }
        }
    }

    fun defaultOnCheckedChange(checked: Boolean, animate: Boolean = true) {
        Log.d("update", "default onCheckedChangeListener called")
        if (checked) {
            nameEdit.paintFlags = nameEdit.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            nameEdit.setTextColor(nameEdit.currentHintTextColor)
            checkBoxCheckmarkController.toStateB(animate)
            checkBoxBackgroundController.toStateB(animate)
        } else {
            nameEdit.paintFlags = nameEdit.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            nameEdit.setTextColor(normalTextColor)
            checkBoxBackgroundController.toStateA(animate)
            checkBoxCheckmarkController.toStateA(animate)
        }
    }

    private fun expandCollapseAnimation(expanding: Boolean): ValueAnimator {
        if (collapsedHeight == 0) initExpandedCollapsedHeights(expanding)

        val startHeight = if (expanding) collapsedHeight else expandedHeight
        val endHeight = if (expanding) expandedHeight else collapsedHeight
        val increaseButtonStartWidth = if (expanding) increaseButtonFullWidth / 2
                                       else           increaseButtonFullWidth
        val increaseButtonEndWidth = if (expanding) increaseButtonFullWidth
                                     else           increaseButtonFullWidth / 2
        val increaseButtonWidthChange = increaseButtonEndWidth - increaseButtonStartWidth

        shoppingListItemDetailsInclude.visibility = View.VISIBLE

        val anim = ValueAnimator.ofInt(startHeight, endHeight)
        anim.addUpdateListener {
            layoutParams.height = anim.animatedValue as Int
            amountOnListEdit.increaseButton.layoutParams.width = increaseButtonStartWidth +
                    (anim.animatedFraction * increaseButtonWidthChange).toInt()
            requestLayout()
        }
        anim.duration = 200
        anim.interpolator = FastOutSlowInInterpolator()
        return anim
    }

    private fun initExpandedCollapsedHeights(expanding: Boolean) {
        val matchParentSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(matchParentSpec, wrapContentSpec)
        if (expanding) {
            increaseButtonFullWidth = increaseButton.layoutParams.width * 2
            collapsedHeight = measuredHeight
            shoppingListItemDetailsInclude.visibility = View.VISIBLE
            measure(matchParentSpec, wrapContentSpec)
            expandedHeight = measuredHeight
        } else {
            increaseButtonFullWidth = increaseButton.layoutParams.width
            expandedHeight = measuredHeight
            shoppingListItemDetailsInclude.visibility = View.GONE
            measure(matchParentSpec, wrapContentSpec)
            collapsedHeight = measuredHeight
        }
    }
}