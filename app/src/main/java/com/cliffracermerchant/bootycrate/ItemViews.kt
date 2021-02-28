/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracermerchant.bootycrate

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout
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

        val verticalPadding = resources.getDimensionPixelSize(R.dimen.recycler_view_item_vertical_padding)
        setPadding(0, verticalPadding, 0, verticalPadding)
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
 * outline depending on the item's selection state. Unless setSelectedState is
 * called with the parameter animate set to false, the change in selection
 * state will be animated with a fade in or out animation.
 *
 * The interface for item expansion consists of expand, collapse, setExpanded,
 * and toggleExpanded. If subclasses need to alter the visibility of additional
 * views during expansion or collapse, they can override the function
 * onExpandedChanged with their additional changes. Like setSelectedState, set-
 * Expanded will animated the changes inside the view unless it is called with
 * the parameter animate equal to false.
 *
 * In order to allow for easier synchronization with concurrent animations out-
 * side the view, ExpandableSelectableItemView has the properties animatorCon-
 * fig, startAnimationsImmediately, and pendingAnimations. The constructor
 * parameter and property animatorConfig will determine the animator config
 * used for the view's internal animations. The default value of AnimatorCon-
 * figs.translation can be overridden in order to make sure the view's anima-
 * tions use the same config as others outside the view. The property start-
 * AnimationsImmediately determines whether the animations prepared during set-
 * Expanded will be started immediately. If it is set to false, the animations
 * will instead be stored. In this case it is up to the containing view to play
 * these animations at the same time as its own using the function runPending-
 * Animations.
 */
@Suppress("LeakingThis")
@SuppressLint("ViewConstructor")
open class ExpandableSelectableItemView<Entity: ExpandableSelectableItem>(
    context: Context,
    val animatorConfig: AnimatorConfig = AnimatorConfig.translation,
    useDefaultLayout: Boolean = true,
) : ViewModelItemView<Entity>(context, useDefaultLayout) {

    val isExpanded get() = _isExpanded
    private var _isExpanded = false
    private val gradientOutline: GradientDrawable

    var startAnimationsImmediately = true
    private val pendingAnimations = mutableListOf<Animator>()
    fun runPendingAnimations() { for (anim in pendingAnimations)
                                     anim.start()
                                 pendingAnimations.clear() }

    init {
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
        clipChildren = false
        if (useDefaultLayout) {
            ui.editButton.setOnClickListener { toggleExpanded() }
            ui.nameEdit.animatorConfig = animatorConfig
            ui.extraInfoEdit.animatorConfig = animatorConfig
            ui.amountEdit.animatorConfig = animatorConfig
        }
    }

    override fun update(item: Entity) {
        super.update(item)
        setExpanded(item.isExpanded, animate = false)
        setSelectedState(item.isSelected, animate = false)
    }

    fun select() = setSelectedState(true)
    fun deselect() = setSelectedState(false)
    fun setSelectedState(selected: Boolean, animate: Boolean = true) {
        if (animate) valueAnimatorOfInt(
            setter = gradientOutline::setAlpha,
            fromValue = if (selected) 0 else 255,
            toValue = if (selected) 255 else 0,
            config = if (selected) AnimatorConfig.fadeIn
                     else          AnimatorConfig.fadeOut
        ).start()
        else gradientOutline.alpha = if (selected) 255 else 0
    }

    fun expand() = setExpanded(true)
    fun collapse() = setExpanded(false)
    fun toggleExpanded() = setExpanded(!isExpanded)
    open fun onExpandedChanged(expanded: Boolean = true, animate: Boolean = true) { }

    fun setExpanded(expanding: Boolean = true, animate: Boolean = true) {
        _isExpanded = expanding
        if (!expanding &&
            ui.nameEdit.isFocused ||
            ui.extraInfoEdit.isFocused ||
            ui.amountEdit.ui.valueEdit.isFocused)
                inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)

     /* While a LayoutTransition would achieve the same thing as all of the following
        custom animations and would be much more readable, it is unfortunately imposs-
        ible to synchronize LayoutTransition animations with other animations because
        the LayoutTransition API does not permit pausing and resuming, or manual star-
        ting after they are prepared, for the animators it uses internally. Unless this
        is changed, the internal expand / collapse animations must be done manually in
        case they need to be synchronized with other animations. */

        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(wrapContentSpec, wrapContentSpec)
        val oldHeight = measuredHeight
        val checkBoxOldTop = ui.checkBox.top

        ui.amountEditSpacer.isVisible = !expanding
        ui.editButton.isActivated = expanding
        val heightDp = if (expanding) 88f else 46f
        ui.spacer.layoutParams.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, heightDp, resources.displayMetrics).toInt()
        ui.linkIndicator.showOrHideWithAnimation(showing = expanding)

        if (animate) {
            val checkBoxNewTop = paddingTop + (ui.spacer.layoutParams.height - ui.checkBox.height) / 2
            val checkBoxTopChange = checkBoxNewTop - checkBoxOldTop.toFloat()
            ui.checkBox.translationY = -checkBoxTopChange
            val checkBoxAnim = valueAnimatorOfFloat(setter = ui.checkBox::setTranslationY,
                                                    fromValue = -checkBoxTopChange,
                                                    toValue = 0f, config = animatorConfig)
            pendingAnimations.add(checkBoxAnim)
        }

        // If the extra info edit is gone but about to be visible, the apparent top
        // of the nameEdit will not change and therefore does not need to be animated.
        val animateNameEdit = animate && !ui.extraInfoEdit.text.isNullOrBlank()
        val nameEditAnimInfo = ui.nameEdit.setEditable(expanding, animateNameEdit)
        if (nameEditAnimInfo != null)
            setupNameHeightAnimation(expanding, nameEditAnimInfo)

        val extraInfoAnimInfo =
            if (ui.extraInfoEdit.text.isNullOrBlank()) {
                // If extraInfoEdit is blank and is being expanded, then we
                // can set its editable state before it becomes visible to
                // prevent needing to animate its change in editable state.
                if (expanding) ui.extraInfoEdit.setEditable(true, animate = false)
                if (animate) {
                    // Because nameEdit is constrained to extraInfoEdit, adding extraInfoEdit
                    // to the overlay during showOrHideWithAnimation will mess up nameEdit.
                    // To avoid this we'll add nameEdit to the overlay as well for the
                    // duration of the animation.
                    if (!expanding) overlay.add(ui.nameEdit)
                    val fadeAnim = ui.extraInfoEdit.showOrHideWithAnimation(showing = expanding)
                    if (!expanding) fadeAnim.doOnEnd {
                        overlay.remove(ui.nameEdit)
                        addView(ui.nameEdit)
                    }
                }
                else ui.extraInfoEdit.isVisible = animate
                null
            }
            else ui.extraInfoEdit.setEditable(expanding, animate)
        if (extraInfoAnimInfo != null)
            setupExtraInfoHeightAnimation(extraInfoAnimInfo, nameEditAnimInfo!!.heightChange)

        val amountEditInternalAnimInfo =
            ui.amountEdit.setValueIsDirectlyEditable(expanding, animate)
        /* For some reason pausing the amount edit internal animation here so
           that it can be resumed when the rest of the animations are played
           causes it to not work. As the delay between these animations should
           be small and unnoticeable (with this particular animation at least),
           we'll just allow it to start now and not bother to add it to
           pendingAnimations. */

        if (amountEditInternalAnimInfo != null) {
            setupAmountTranslateAnimation(expanding, amountEditInternalAnimInfo.widthChange)
         /* If the amount edit changed its left coordinate, then nameEdit and, if
            it wasn't hidden, extraInfoEdit will need to have their ends animated
            to prevent their underlines and their text from briefly overlapping
            the amount edit. */
            setupTextFieldWidthAnimations(
                endChange = amountEditInternalAnimInfo.widthChange +
                    ui.amountEditSpacer.layoutParams.width * if (expanding) -1 else 1,
                animateExtraInfo = extraInfoAnimInfo != null)
        }

        // Allow subclasses to make their changes.
        onExpandedChanged(expanding, animate)

        if (animate) {
            // edit button slide down animation
            measure(wrapContentSpec, wrapContentSpec)
            val heightChange = measuredHeight - oldHeight
            pendingAnimations.add(valueAnimatorOfFloat(
                setter = ui.editButton::setTranslationY,
                fromValue = -heightChange * 1f,
                toValue = 0f, config = animatorConfig
            ).apply { start(); pause() })

            if (startAnimationsImmediately)
                runPendingAnimations()
        }
    }

    private fun setupNameHeightAnimation(
        expanding: Boolean,
        nameEditAnimInfo: TextFieldEdit.AnimInfo
    ) {
        nameEditAnimInfo.animator.pause()
        // If the extra info edit is going to appear or disappear, then nameEdit's
        // translation animation's values will have to be adjusted by its top change.
        if (ui.extraInfoEdit.text.isNullOrBlank()) {
            val newTop = if (expanding) paddingTop else
                paddingTop - nameEditAnimInfo.heightChange / 2
            val topChange = newTop - ui.nameEdit.top

            val newStart = nameEditAnimInfo.startTranslationY - if (expanding) topChange else 0
            val newEnd = nameEditAnimInfo.endTranslationY - if (expanding) 0 else -topChange
            ui.nameEdit.translationY = newStart
            nameEditAnimInfo.animator.setFloatValues(newStart, newEnd)
            // If the ending translationY value is not zero, it needs to be set to zero
            // on the new layout after the animation has ended to avoid flickering.
            if (!expanding) nameEditAnimInfo.animator.doOnEnd {
                doOnNextLayout { ui.nameEdit.translationY = 0f }
            }
        }
        pendingAnimations.add(nameEditAnimInfo.animator)
    }

    private fun setupExtraInfoHeightAnimation(
        extraInfoAnimInfo: TextFieldEdit.AnimInfo,
        nameEditHeightChange: Int
    ) {
        extraInfoAnimInfo.animator.pause()
        // We have to adjust the extraInfoEdit starting translation by the
        // height change of the nameEdit to get the correct translation amount.
        // Without the multiplier of 0.95f the value seems to be very slightly
        // off, causing a very small flicker when the animation starts.
        // Obviously it would be better to figure out where this calculation
        // is off instead of arbitrarily changing the value to what seems to
        // be correct, but it will do for now.
        ui.extraInfoEdit.translationY -= nameEditHeightChange * 0.95f
        extraInfoAnimInfo.animator.setFloatValues(
            extraInfoAnimInfo.startTranslationY * 0.95f,
            extraInfoAnimInfo.endTranslationY)
        pendingAnimations.add(extraInfoAnimInfo.animator)
    }

    private fun setupAmountTranslateAnimation(expanding: Boolean, amountEditWidthChange: Int) {
        val leftChange = amountEditWidthChange +
            ui.amountEditSpacer.layoutParams.width * if (expanding) -1f else 1f
        val amountEditTranslateAnim = valueAnimatorOfFloat(
            setter = ui.amountEdit::setTranslationX,
            fromValue = leftChange * 1f, toValue = 0f,
            config = animatorConfig)
        pendingAnimations.add(amountEditTranslateAnim.apply { start(); pause() })
    }

    private fun setupTextFieldWidthAnimations(endChange: Int, animateExtraInfo: Boolean) {
        nameLockedWidth = ui.nameEdit.width
        if (animateExtraInfo)
            extraInfoLockedWidth = ui.extraInfoEdit.width

        pendingAnimations.add(valueAnimatorOfInt(
            setter = ui.nameEdit::setRight,
            fromValue = ui.nameEdit.right,
            toValue = ui.nameEdit.right - endChange,
            config = animatorConfig).apply {
            doOnStart { nameLockedWidth = null }
        })

        if (!animateExtraInfo) return
        pendingAnimations.add(valueAnimatorOfInt(
            setter = ui.extraInfoEdit::setRight,
            fromValue = ui.extraInfoEdit.right,
            toValue = ui.extraInfoEdit.right - endChange,
            config = animatorConfig).apply {
            doOnStart { extraInfoLockedWidth = null }
        })
    }

 /* For some reason (possibly because the text field edits internally call
    setMinHeight, which in turn calls requestLayout), both nameEdit and extra-
    InfoEdit have their width set to their new expanded width sometime after
    setExpanded but before the end animations are started, causing a visual
    flicker. The properties nameLockedWidth and extraInfoLockedWidth, when set
    to a non-null value, prevent this resize from taking place, and in turn
    prevent the flicker effect. */
    private var nameLockedWidth: Int? = null
    private var extraInfoLockedWidth: Int? = null
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        nameLockedWidth?.let {
            if (ui.nameEdit.width != it)
                ui.nameEdit.right = ui.nameEdit.left + it
        }
        extraInfoLockedWidth?.let {
            if (ui.extraInfoEdit.width != it)
                ui.extraInfoEdit.right = ui.extraInfoEdit.left + it
        }
    }

    protected fun View.showOrHideWithAnimation(showing: Boolean): Animator {
        alpha = if (showing) 0f else 1f
        if (!isVisible) isVisible = true
        val endAlpha = if (showing) 1f else 0f
        val config = if (showing) AnimatorConfig.fadeIn
                     else         AnimatorConfig.fadeOut
        val animator = valueAnimatorOfFloat(::setAlpha, alpha, endAlpha, config)
        if (!showing) {
            val parent = parent as ViewGroup
            parent.overlay.add(this)
            animator.doOnEnd {
                parent.overlay.remove(this)
                isVisible = false
                parent.addView(this)
            }
        }
        pendingAnimations.add(animator)
        return animator
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
    ExpandableSelectableItemView<ShoppingListItem>(context, AnimatorConfig.shoppingListItem)
{
    override fun update(item: ShoppingListItem) {
        ui.checkBox.initIsChecked(item.isChecked)
        setStrikeThroughEnabled(enabled = item.isChecked, animate = false)
        super.update(item)
    }

    override fun onExpandedChanged(expanded: Boolean, animate: Boolean) {
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
        ui.nameEdit.animatorConfig = AnimatorConfig.translation
        ui.extraInfoEdit.animatorConfig = AnimatorConfig.translation
        ui.amountEdit.animatorConfig = AnimatorConfig.translation
    }

    override fun update(item: InventoryItem) {
        detailsUi.addToShoppingListCheckBox.isChecked = item.addToShoppingList
        detailsUi.addToShoppingListTriggerEdit.initValue(item.addToShoppingListTrigger)
        super.update(item)
    }

    override fun onExpandedChanged(expanded: Boolean, animate: Boolean) {
        if (!expanded && detailsUi.addToShoppingListTriggerEdit.ui.valueEdit.isFocused)
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
        detailsUi.inventoryItemDetailsGroup.isVisible = expanded
    }
}