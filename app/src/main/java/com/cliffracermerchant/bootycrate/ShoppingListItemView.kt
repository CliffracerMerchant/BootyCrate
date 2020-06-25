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
    val expanded get() = expandedPrivate
    private var expandedPrivate = false
    private var imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
    private val minusToMultiplyIcon = context.getDrawable(R.drawable.shopping_list_animated_minus_to_multiply_icon)
    private val blankToPlusIcon = context.getDrawable(R.drawable.animated_blank_to_plus_icon)
    private val multiplyToMinusIcon = context.getDrawable(R.drawable.shopping_list_animated_multiply_to_minus_icon)
    private val plusToBlankIcon = context.getDrawable(R.drawable.animated_plus_to_blank_icon)
    private val editToMoreOptionsIcon = context.getDrawable(R.drawable.animated_edit_to_more_options_icon)
    private val moreOptionsToEditIcon = context.getDrawable(R.drawable.animated_more_options_to_edit_icon)
    private val checkBoxCheckedToUncheckedIcon = context.getDrawable(R.drawable.shopping_list_checkbox_checked_to_unchecked_background)
    private val checkBoxUncheckedToCheckedIcon = context.getDrawable(R.drawable.shopping_list_checkbox_unchecked_to_checked_background)
    private val linkedItemDescriptionString = context.getString(R.string.linked_shopping_list_item_description)
    private val unlinkedItemDescriptionString = context.getString(R.string.unlinked_shopping_list_item_description)
    private val linkNowActionString = context.getString(R.string.shopping_list_item_link_now_action_description)
    private val changeLinkActionString = context.getString(R.string.shopping_list_item_change_link_action_description)
    var currentColor: Int? = null

    /* This companion object stores the heights of the layout when expanded/
       collapsed. This prevents the expand/collapse animations from having to
       calculate these values every time the animation is started. */
    private companion object {
        var collapsedHeight = 0
        var expandedHeight = 0
    }

    init {
        inflate(context, R.layout.shopping_list_item_layout, this)
        linkedToEdit.paintFlags = linkedToEdit.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        editButton.setOnClickListener {
            if (expanded) //TODO: Implement more options menu
            else          expand()
        }
        collapseButton.setOnClickListener { collapse() }
        amountOnListEdit.decreaseButton.setOnClickListener { if (expanded) amountOnListEdit.decrement() }
        amountOnListEdit.increaseButton.setOnClickListener { if (expanded) amountOnListEdit.increment() }

        checkBox.setOnClickListener {
            if (checkBox.isChecked) {
                if (amountInCartEdit.currentValue < amountOnListEdit.currentValue)
                    amountInCartEdit.currentValue = amountOnListEdit.currentValue
            } else {
                amountInCartEdit.currentValue = 0
            }
        }
        val normalTextColor = nameEdit.currentTextColor
        checkBox.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                nameEdit.paintFlags = nameEdit.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                nameEdit.setTextColor(nameEdit.currentHintTextColor)
                checkBox.background = checkBoxUncheckedToCheckedIcon
                val icon = checkBox.background as LayerDrawable
                val bg = icon.getDrawable(0) as AnimatedVectorDrawable
                bg.setTint(currentColor ?: 0)
                bg.start()
                (icon.getDrawable(1) as AnimatedVectorDrawable).start()
            } else {
                nameEdit.paintFlags = nameEdit.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                nameEdit.setTextColor(normalTextColor)
                checkBox.background = checkBoxCheckedToUncheckedIcon
                val icon = checkBox.background as LayerDrawable
                val bg = icon.getDrawable(0) as AnimatedVectorDrawable
                bg.setTint(currentColor ?: 0)
                bg.start()
                (icon.getDrawable(1) as AnimatedVectorDrawable).start()
            }
        }
        // A TextWatcher is used here instead of observing the IntegerEdits'
        // liveData members so that the change is detected instantly, rather
        // than after their valueChangedNotificationTimeout delay
        amountOnListEdit.valueEdit.doAfterTextChanged {
            checkBox.isChecked = amountInCartEdit.currentValue >= amountOnListEdit.currentValue
        }
        amountInCartEdit.valueEdit.doAfterTextChanged {
            checkBox.isChecked = amountInCartEdit.currentValue >= amountOnListEdit.currentValue
        }
        collapse(false)

        // If the layout's children are clipped, they will suddenly appear or
        // disappear without an animation during the expand collapse animation
        clipChildren = false
    }

    fun update(item: ShoppingListItem) {
        nameEdit.setText(item.name)
        extraInfoEdit.setText(item.extraInfo)
        amountOnListEdit.setCurrentValueWithoutDataUpdate(item.amountOnList)
        amountInCartEdit.setCurrentValueWithoutDataUpdate(item.amountInCart)

        currentColor = item.color
        checkBox.background = if (item.amountInCart >= item.amountOnList) checkBoxCheckedToUncheckedIcon
                              else                                        checkBoxUncheckedToCheckedIcon
        (checkBox.background as LayerDrawable).getDrawable(0).setTint(currentColor ?: 0)

        updateLinkedStatus(item.linkedInventoryItemId)
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
        expandedPrivate = true
        nameEdit.isEditable = true
        amountOnListEdit.isEditable = true
        extraInfoEdit.isEditable = true
        amountInCartEdit.isEditable = true

        if (animate) {
            amountOnListEdit.decreaseButton.background = multiplyToMinusIcon
            amountOnListEdit.increaseButton.background = blankToPlusIcon
            editButton.background = editToMoreOptionsIcon
            (amountOnListEdit.decreaseButton.background as AnimatedVectorDrawable).start()
            (amountOnListEdit.increaseButton.background as AnimatedVectorDrawable).start()
            (editButton.background as AnimatedVectorDrawable).start()
            val anim = expandCollapseAnimation(true)
            anim.doOnEnd { shoppingListItemDetailsInclude.visibility = View.VISIBLE }
            anim.start()
        } else {
            amountOnListEdit.decreaseButton.background = minusToMultiplyIcon
            amountOnListEdit.increaseButton.background = plusToBlankIcon
            editButton.background = moreOptionsToEditIcon
            shoppingListItemDetailsInclude.visibility = View.VISIBLE
            amountOnListEdit.increaseButton.layoutParams.apply{ width *= 2 }
        }
    }

    fun collapse(animate: Boolean = true) {
        expandedPrivate = false
        nameEdit.isEditable = false
        amountOnListEdit.isEditable = false
        extraInfoEdit.isEditable = false
        amountInCartEdit.isEditable = false

        if (animate) {
            amountOnListEdit.decreaseButton.background = minusToMultiplyIcon
            amountOnListEdit.increaseButton.background = plusToBlankIcon
            editButton.background = moreOptionsToEditIcon
            (amountOnListEdit.decreaseButton.background as AnimatedVectorDrawable).start()
            (amountOnListEdit.increaseButton.background as AnimatedVectorDrawable).start()
            (editButton.background as AnimatedVectorDrawable).start()
            val anim = expandCollapseAnimation(false)
            anim.doOnEnd { shoppingListItemDetailsInclude.visibility = View.GONE }
            anim.start()
        } else {
            amountOnListEdit.decreaseButton.background = multiplyToMinusIcon
            amountOnListEdit.increaseButton.background = blankToPlusIcon
            editButton.background = editToMoreOptionsIcon
            shoppingListItemDetailsInclude.visibility = View.GONE
            amountOnListEdit.increaseButton.layoutParams.apply { width /= 2 }
        }
        /*when {
            nameEdit.isFocused -> nameEdit.clearFocus()
            amountInCartEdit.valueEdit.isFocused -> amountInCartEdit.valueEdit.clearFocus()
            amountOnListEdit.valueEdit.isFocused -> amountOnListEdit.valueEdit.clearFocus()
            extraInfoEdit.isFocused -> nameEdit.clearFocus()
        }*/
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun expandCollapseAnimation(expanding: Boolean): ValueAnimator {
        if (collapsedHeight == 0) initExpandedCollapsedHeights(expanding)

        val startHeight = if (expanding) collapsedHeight else expandedHeight
        val endHeight = if (expanding) expandedHeight else collapsedHeight
        val change = endHeight - startHeight
        val increaseButtonStartWidth = amountOnListEdit.increaseButton.layoutParams.width
        val increaseButtonWidthChange = if (expanding) increaseButtonStartWidth
                                        else           -increaseButtonStartWidth / 2

        shoppingListItemDetailsInclude.visibility = View.VISIBLE

        val anim = ValueAnimator.ofInt(startHeight, endHeight)
        anim.addUpdateListener {
            layoutParams.height = startHeight + (anim.animatedFraction * change).toInt()
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
            collapsedHeight = measuredHeight
            shoppingListItemDetailsInclude.visibility = View.VISIBLE
            measure(matchParentSpec, wrapContentSpec)
            expandedHeight = measuredHeight
        } else {
            expandedHeight = measuredHeight
            shoppingListItemDetailsInclude.visibility = View.GONE
            measure(matchParentSpec, wrapContentSpec)
            collapsedHeight = measuredHeight
        }
    }
}