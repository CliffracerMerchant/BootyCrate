package com.cliffracermerchant.bootycrate

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.AnimatedVectorDrawable
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
 *  It uses lazy initialization of the extra fields displayed when expanded,
 *  and only updates them when the details are expanded. Its expand and col-
 *  lapse functions also allow for an optional animation. */
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
    private val linkedItemDescriptionString = context.getString(R.string.linked_shopping_list_item_description)
    private val unlinkedItemDescriptionString = context.getString(R.string.unlinked_shopping_list_item_description)
    private val linkNowActionString = context.getString(R.string.shopping_list_item_link_now_action_description)
    private val changeLinkActionString = context.getString(R.string.shopping_list_item_change_link_action_description)
    private val normalTextColor: Int

    /* This companion object stores the heights of the layout when expanded/
       collapsed. This prevents the expand/collapse animations from having to
       calculate these values every time the animation is started. */
    private companion object {
        var collapsedHeight = 0
        var expandedHeight = 0
    }

    init {
        inflate(context, R.layout.shopping_list_item_layout, this)
        normalTextColor = nameEdit.currentTextColor

        collapseButton.setOnClickListener { collapse() }
        // If the layout's children are clipped, they will suddenly appear or
        // disappear without an animation during the expand collapse animation
        clipChildren = false

        linkedToEdit.paintFlags = linkedToEdit.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        linkedToEdit.setOnClickListener {
            //TODO Make linkedToEdit on click listener
        }

        editButton.setOnClickListener {
            if (expanded) //TODO: Implement more options menu
            else          expand()
        }

        amountOnListEdit.decreaseButton.setOnClickListener { if (expanded) amountOnListEdit.decrement() }
        amountOnListEdit.increaseButton.setOnClickListener { if (expanded) amountOnListEdit.increment() }

        checkBox.setOnClickListener {
            if (checkBox.isChecked) {
                if (amountInCartEdit.currentValue < amountOnListEdit.currentValue)
                    amountInCartEdit.currentValue = amountOnListEdit.currentValue
            } else amountInCartEdit.currentValue = 0
        }
        checkBox.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                nameEdit.paintFlags = nameEdit.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                nameEdit.setTextColor(nameEdit.currentHintTextColor)
            } else {
                nameEdit.paintFlags = nameEdit.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                nameEdit.setTextColor(normalTextColor)
            }
        }
        // A TextWatcher is used here instead of observing amountInCartEdit's
        // liveData member so that the change is detected instantly, rather
        // than after amountInCartEdit's valueChangedNotificationTimeout delay
        amountInCartEdit.valueEdit.doAfterTextChanged {
            checkBox.isChecked = amountInCartEdit.currentValue >= amountOnListEdit.currentValue
        }
        collapse(false)
    }

    fun update(item: ShoppingListItem) {
        nameEdit.setText(item.name)
        amountOnListEdit.currentValue = item.amount
        amountInCartEdit.currentValue = item.amountInCart
        extraInfoEdit.setText(item.extraInfo)
        checkBox.isChecked = amountInCartEdit.currentValue >= amountOnListEdit.currentValue
        updateLinkedStatus(item)
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
//            amountOnListEdit.valueEdit.apply { setPaddingRelative(paddingStart, paddingTop,
//                                                                  paddingEnd / 6, paddingBottom) }
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
//            amountOnListEdit.valueEdit.apply { setPaddingRelative(paddingStart, paddingTop,
//                                                                  paddingEnd * 6, paddingBottom) }
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
        //val amountOnListStartPaddingEnd = amountOnListEdit.valueEdit.paddingEnd
        //val amountOnListPaddingEndChange = if (expanding) amountOnListStartPaddingEnd * -5 / 6
        //else amountOnListStartPaddingEnd * 5

        shoppingListItemDetailsInclude.visibility = View.VISIBLE
        amountInCartEdit.visibility = View.VISIBLE

        val anim = ValueAnimator.ofInt(startHeight, endHeight)
        anim.addUpdateListener {
            layoutParams.height = startHeight + (anim.animatedFraction * change).toInt()
            requestLayout()
//            val paddingEnd = amountOnListStartPaddingEnd +
//                             (amountOnListPaddingEndChange * anim.animatedFraction).toInt()
//            amountOnListEdit.valueEdit.apply { setPaddingRelative(paddingStart, paddingTop,
//                                                                  paddingEnd, paddingBottom) }
//            amountOnListEdit.valueEdit.requestLayout()
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

    private fun updateLinkedStatus(item: ShoppingListItem) {
        if (item.linkedInventoryItemId != null) {
            linkedToIndicator.text = linkedItemDescriptionString
            linkedToEdit.text = changeLinkActionString
            amountInCartEdit.visibility = View.INVISIBLE
        } else {
            linkedToIndicator.text = unlinkedItemDescriptionString
            linkedToEdit.text = linkNowActionString
            amountInCartEdit.visibility = View.VISIBLE
        }
    }
}