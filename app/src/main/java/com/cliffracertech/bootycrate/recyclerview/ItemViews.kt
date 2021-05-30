/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.inputmethod.InputMethodManager
import androidx.annotation.CallSuper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.BootyCrateItem
import com.cliffracertech.bootycrate.database.InventoryItem
import com.cliffracertech.bootycrate.database.ShoppingListItem
import com.cliffracertech.bootycrate.databinding.BootyCrateItemBinding
import com.cliffracertech.bootycrate.databinding.InventoryItemBinding
import com.cliffracertech.bootycrate.databinding.InventoryItemDetailsBinding
import com.cliffracertech.bootycrate.utils.*

/**
 * A layout to display the data for a BootyCrateItem.
 *
 * BootyCrateItemView displays the data for an instance of BootyCrateItem. The
 * displayed data can be updated for a new item with the function update.
 *
 * By default BootyCrateItemView inflates itself with the contents of
 * R.layout.booty_crate_item.xml and initializes its BootyCrateItemBinding
 * member ui. In case this layout needs to be overridden in a subclass, the
 * BootyCrateItemView can be constructed with the parameter useDefaultLayout
 * equal to false. If useDefaultLayout is false, it will be up to the subclass
 * to inflate the desired layout and initialize the member ui with an instance
 * of a BootyCrateItemBinding. If the ui member is not initialized then a
 * kotlin.UninitializedPropertyAccessException will be thrown.
 */
@Suppress("LeakingThis")
@SuppressLint("ViewConstructor")
open class BootyCrateItemView<T: BootyCrateItem>(
    context: Context,
    useDefaultLayout: Boolean = true,
) : ConstraintLayout(context) {

    protected val inputMethodManager = inputMethodManager(context)
    protected var isLinkedToAnotherItem = false

    lateinit var ui: BootyCrateItemBinding

    init {
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 ViewGroup.LayoutParams.WRAP_CONTENT)
        if (useDefaultLayout)
            ui = BootyCrateItemBinding.inflate(LayoutInflater.from(context), this)

        val verticalPadding = resources.getDimensionPixelSize(R.dimen.recycler_view_item_vertical_padding)
        setPadding(0, verticalPadding, 0, verticalPadding)
    }

    @CallSuper open fun update(item: T) {
        ui.nameEdit.setText(item.name)
        ui.extraInfoEdit.setText(item.extraInfo)
        val colorIndex = item.color.coerceIn(BootyCrateItem.Colors.indices)
        ui.checkBox.initColorIndex(colorIndex)
        ui.amountEdit.initValue(item.amount)
        isLinkedToAnotherItem = item.linked
    }
}



/**
 * A BootyCrateItemView subclass that provides an interface for a selection and expansion of the view.
 *
 * ExpandableSelectableItemView extends BootyCrateItemView by providing an
 * interface for expansion and selection, and with an update override that
 * will update the view to reflect the selection and expansion state of the
 * BootyCrateItem passed to it.
 *
 * The interface for selection and deselection consists of the functions
 * isInSelectedState, select, deselect, and setSelectedState. With the default
 * background these functions will give the view a surrounding gradient outline
 * or hide the outline depending on the item's selection state. Unless
 * setSelectedState is called with the parameter animate set to false, the
 * change in selection state will be animated with a fade in or out animation.
 * Note that setSelected and isSelected are part of the Android framework's
 * View's API, and have nothing to do with the selection API added by
 * ExpandableSelectableItemView.
 *
 * The interface for item expansion consists of expand, collapse, setExpanded,
 * and toggleExpanded. If subclasses need to alter the visibility of additional
 * views during expansion or collapse, they can override the function
 * onExpandedChanged with their additional changes. Like setSelectedState,
 * setExpanded will animate the changes inside the view unless it is called
 * with the parameter animate equal to false.
 *
 * In order to allow for easier synchronization with concurrent animations
 * outside the view, all of ExpandableSelectableItemView's internal animations
 * use the AnimatorConfig defined by the property animatorConfig. If delaying
 * the animations is also required to synchronize them with other animations,
 * the property startAnimationsImmediately can be set to false. In this case
 * the animations will be prepared and stored, and can be played by calling
 * runPendingAnimations.
 */
@SuppressLint("ViewConstructor")
open class ExpandableSelectableItemView<T: BootyCrateItem>(
    context: Context,
    animatorConfig: AnimatorConfig? = null,
    useDefaultLayout: Boolean = true,
) : BootyCrateItemView<T>(context, useDefaultLayout),
    ExpandableItemAnimator.ExpandableRecyclerViewItem
{
    val isExpanded get() = _isExpanded
    private var _isExpanded = false
    private val gradientOutline: GradientDrawable

    var animatorConfig: AnimatorConfig? = null
        set(value) { field = value
                     ui.nameEdit.animatorConfig = value
                     ui.extraInfoEdit.animatorConfig = value
                     ui.amountEdit.animatorConfig = value }
    var startAnimationsImmediately = true

    private val pendingAnimations = mutableListOf<Animator>()

    override fun runPendingAnimations() { pendingAnimations.forEach { it.start() }
                                          pendingAnimations.clear() }

    init {
        val background = ContextCompat.getDrawable(context, R.drawable.recycler_view_item_background) as LayerDrawable
        gradientOutline = (background.getDrawable(1) as LayerDrawable).getDrawable(0) as GradientDrawable
        this.background = background
        clipChildren = false
        if (useDefaultLayout) {
            ui.editButton.setOnClickListener { toggleExpanded() }
            this.animatorConfig = animatorConfig
        }
    }

    override fun update(item: T) {
        super.update(item)
        setExpanded(item.isExpanded, animate = false)
        setSelectedState(item.isSelected, animate = false)
    }

    private var _isInSelectedState = false
    val isInSelectedState get() = _isInSelectedState
    fun select() = setSelectedState(true)
    fun deselect() = setSelectedState(false)
    fun setSelectedState(selected: Boolean, animate: Boolean = true) {
        _isInSelectedState = selected
        if (animate) intValueAnimator(gradientOutline::setAlpha,
                                      gradientOutline.alpha,
                                      if (selected) 255 else 0,
                                      animatorConfig).start()
        else gradientOutline.alpha = if (selected) 255 else 0
    }

    fun toggleExpanded() = setExpanded(!isExpanded)
    open fun onExpandedChanged(expanded: Boolean = true, animate: Boolean = true) { }

    private fun View.clearFocusAndHideSoftInput(imm: InputMethodManager) {
        // Clearing the focus before hiding the soft input prevents a flickering
        // issue when the view is collapsed on some older API levels.
        if (!isFocused) return
        clearFocus()
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun setExpanded(expanding: Boolean, animate: Boolean) {
        _isExpanded = expanding
        if (!expanding) inputMethodManager?.let {
            ui.nameEdit.clearFocusAndHideSoftInput(it)
            ui.extraInfoEdit.clearFocusAndHideSoftInput(it)
            ui.amountEdit.ui.valueEdit.clearFocusAndHideSoftInput(it)
        }

     /* While a LayoutTransition would achieve the same thing as all of the following
        custom animations and would be much more readable, it is unfortunately
        impossible to synchronize LayoutTransition animations with other animations
        because the LayoutTransition API does not permit pausing and resuming, or manual
        starting after they are prepared, for the animators it uses internally. Unless
        this is changed, the internal expand / collapse animations must be done manually
        in case they need to be synchronized with other animations. */
        val editableTextFieldHeight = resources.getDimension(R.dimen.editable_text_field_min_height)
        ui.spacer.layoutParams.height = (editableTextFieldHeight * if (expanding) 2 else 1).toInt()

        if (isLinkedToAnotherItem)
            if (!animate) ui.linkIndicator.isVisible = expanding
            else ui.linkIndicator.showOrHideViaOverlay(showing = expanding)
        if (animate) setupCheckBoxAnimation()
        val nameEditHeightChange = updateNameEditState(expanding, animate)
        updateExtraInfoState(expanding, animate, nameEditHeightChange)
        updateAmountEditState(expanding, animate)
        onExpandedChanged(expanding, animate)
        updateEditButtonState(expanding, animate)

        if (animate && startAnimationsImmediately)
            runPendingAnimations()
    }

    private fun setupCheckBoxAnimation() {
        val checkBoxNewTop = paddingTop + (ui.spacer.layoutParams.height - ui.checkBox.height) / 2
        val checkBoxTopChange = checkBoxNewTop - ui.checkBox.top.toFloat()
        ui.checkBox.translationY = -checkBoxTopChange
        pendingAnimations.add(floatValueAnimator(ui.checkBox::setTranslationY,
                                                 -checkBoxTopChange, 0f,
                                                 animatorConfig))
    }

    /** Update the editable state of nameEdit, animating if param animate == true,
     * and return the height change of the nameEdit, or 0 if no animation occurred. */
    private fun updateNameEditState(expanding: Boolean, animate: Boolean): Int {
        val nameEditAnimInfo = ui.nameEdit.setEditable(expanding, animate, false) ?: return 0
        pendingAnimations.add(nameEditAnimInfo.translateAnimator)
        pendingAnimations.add(nameEditAnimInfo.underlineAnimator)
        // If the extra info edit is going to appear or disappear, then nameEdit's
        // translation animation's values will have to be adjusted by its top change.
        if (ui.extraInfoEdit.text.isNullOrBlank()) {
            val newTop = if (expanding) paddingTop
            else paddingTop - nameEditAnimInfo.heightChange / 2
            val topChange = newTop - ui.nameEdit.top

            val transYStartAdjust = if (expanding) -topChange.toFloat() else 0f
            val transYEndAdjust = if (expanding) 0f else topChange.toFloat()
            ui.nameEdit.translationY += transYStartAdjust
            nameEditAnimInfo.adjustTranslationStartEnd(transYStartAdjust, transYEndAdjust)
            // If the ending translationY value is not zero, it needs to be set to zero
            // on the new layout after the animation has ended to avoid flickering.
            if (!expanding) nameEditAnimInfo.translateAnimator.doOnEnd {
                doOnNextLayout { ui.nameEdit.translationY = 0f }
            }
        }
        return nameEditAnimInfo.heightChange
    }

    /** Update the editable state of extraInfoEdit, animating if
     * param animate == true and the extraInfoEdit is not blank. */
    private fun updateExtraInfoState(expanding: Boolean, animate: Boolean, nameHeightChange: Int) {
        val extraInfoIsBlank = ui.extraInfoEdit.text.isNullOrBlank()
        val animInfo = ui.extraInfoEdit.setEditable(expanding, animate, startAnimationsImmediately = false)
        if (!animate) {
            // Since we have already set the editable state, if no animation
            // is needed we can just set the visibility and exit early.
            ui.extraInfoEdit.isVisible = expanding || !extraInfoIsBlank
            return
        }

        pendingAnimations.add(animInfo!!.translateAnimator)
        pendingAnimations.add(animInfo.underlineAnimator)
        if (!extraInfoIsBlank) {
            // We have to adjust the extraInfoEdit starting translation by the
            // height change of the nameEdit to get the correct translation amount.
            ui.extraInfoEdit.translationY -= nameHeightChange
            animInfo.adjustTranslationStartEnd(-nameHeightChange.toFloat(), 0f)
        }

        if (extraInfoIsBlank) {
            val anim = ui.extraInfoEdit.showOrHideViaOverlay(showing = expanding)
            // Because nameEdit is constrained to extraInfoEdit, adding extra-
            // InfoEdit to the overlay during showOrHideViaOverlay will alter
            // nameEdit's position. To avoid this we'll add nameEdit to the over-
            // lay as well for the duration of the animation.
            if (!expanding) {
                overlay.add(ui.nameEdit)
                anim.doOnEnd { overlay.remove(ui.nameEdit)
                               addView(ui.nameEdit) }
            }
        }
    }

    /** Update the editable state of amountEdit, animating if param animate == true. */
    private fun updateAmountEditState(expanding: Boolean, animate: Boolean) {
        val amountEditAnimInfo = ui.amountEdit.setValueIsFocusable(
            focusable = expanding,
            animate = animate,
            startAnimationsImmediately = false
        ) ?: return
        pendingAnimations.addAll(amountEditAnimInfo.animators)

        // IntegerEdit's internal animation will only take into account its
        // width change. We have to make another translate animation to take
        // into account the amountEdit's left/start change.
        ui.amountEditSpacer.isVisible = !expanding
        val amountEndChange = ui.amountEditSpacer.layoutParams.width * if (expanding) 1 else -1
        val amountLeftChange = amountEndChange - amountEditAnimInfo.widthChange
        ui.amountEdit.translationX = -amountLeftChange.toFloat()
        val amountEditTranslateAnim = floatValueAnimator(ui.amountEdit::setTranslationX,
                                                         ui.amountEdit.translationX, 0f,
                                                         animatorConfig)
        pendingAnimations.add(amountEditTranslateAnim)

        // Because their ends are constrained to amountEdit's start, nameEdit
        // and, if it wasn't hidden, extraInfoEdit will need to have their end
        // values animated as well.
        nameLockedWidth = ui.nameEdit.width
        pendingAnimations.add(intValueAnimator(
            setter = ui.nameEdit::setRight,
            from = ui.nameEdit.right,
            to = ui.nameEdit.right + amountLeftChange,
            config = animatorConfig
        ).apply { doOnStart { nameLockedWidth = null } })

        if (ui.extraInfoEdit.text.isNullOrBlank()) return
        extraInfoLockedWidth = ui.extraInfoEdit.width
        pendingAnimations.add(intValueAnimator(
            setter = ui.extraInfoEdit::setRight,
            from = ui.extraInfoEdit.right,
            to = ui.extraInfoEdit.right + amountLeftChange,
            config = animatorConfig
        ).apply { doOnStart { extraInfoLockedWidth = null } })
    }

    private fun updateEditButtonState(expanding: Boolean, animate: Boolean) {
        val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(wrapContentSpec, wrapContentSpec)
        val heightChange = measuredHeight - height
        // editButton uses a state list animator with state_activated as the trigger.
        if (!animate)
            ui.editButton.isActivated = expanding
        else {
            ui.editButton.translationY = -heightChange.toFloat()
            val editButtonAnim = floatValueAnimator(ui.editButton::setTranslationY,
                                                    -heightChange.toFloat(), 0f,
                                                    animatorConfig)
            editButtonAnim.doOnStart { ui.editButton.isActivated = expanding }
            pendingAnimations.add(editButtonAnim)
        }
    }

    /**
     * Show or hide the child view with a fade in or out animation, and return the animator.
     *
     * showOrHideViaOverlay differs from a simple fade in/out animation
     * in that it temporarily removes fading out views from their parent so
     * that change appearing/disappearing animations in the parent view can
     * play concurrently with the fade out animation.
     *
     * Because removing the view from its parent can affect sibling views,
     * the fade in/out animator is returned to aid in synchronizing the
     * animation with countermeasures the parent might employ to hide the
     * effects of temporarily removing the child.
     */
    protected fun View.showOrHideViaOverlay(showing: Boolean): Animator {
        alpha = if (showing) 0f else 1f
        isVisible = true
        val animator = floatValueAnimator(::setAlpha, alpha, if (showing) 1f else 0f, animatorConfig)
        if (!showing) {
            val parent = parent as ViewGroup
            parent.overlay.add(this)
            animator.doOnEnd { parent.overlay.remove(this)
                               isVisible = false
                               parent.addView(this) }
        }
        pendingAnimations.add(animator)
        return animator
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
}



/**
 * An ExpandableSelectableItemView to display the contents of a shopping list item.
 *
 * ShoppingListItemView is a ExpandableSelectableItemView subclass that
 * displays the data of a ShoppingListItem instance. It has an update override
 * that updates the check state of the checkbox, it overrides the setExpanded
 * function with an implementation that toggles the checkbox between its normal
 * checkbox mode and its color edit mode, and it has a convenience method
 * setStrikeThroughEnabled that will set the strike through state for both the
 * name and extra info edit at the same time.
 */
@SuppressLint("ViewConstructor")
class ShoppingListItemView(context: Context, animatorConfig: AnimatorConfig? = null) :
    ExpandableSelectableItemView<ShoppingListItem>(context, animatorConfig)
{
    init { ui.checkBox.onCheckedChangedListener = ::setStrikeThroughEnabled }

    override fun update(item: ShoppingListItem) {
        ui.checkBox.initIsChecked(item.isChecked)
        setStrikeThroughEnabled(enabled = item.isChecked, animate = false)
        super.update(item)
    }

    override fun onExpandedChanged(expanded: Boolean, animate: Boolean) {
        ui.checkBox.setInColorEditMode(expanded, animate)
    }

    fun setStrikeThroughEnabled(enabled: Boolean) = setStrikeThroughEnabled(enabled, true)
    private fun setStrikeThroughEnabled(enabled: Boolean, animate: Boolean) {
        ui.nameEdit.setStrikeThroughEnabled(enabled, animate)
        ui.extraInfoEdit.setStrikeThroughEnabled(enabled, animate)
    }
}



/**
 * An ExpandableSelectableItemView to display the contents of an inventory item.
 *
 * InventoryItemView is a ExpandableSelectableItemView subclass that displays
 * the data of an InventoryItem instance. It has an update override for the
 * extra fields that InventoryItem adds to its parent class, and has a
 * setExpanded override that also shows or hides these extra fields.
 */
@SuppressLint("ViewConstructor")
class InventoryItemView(context: Context, animatorConfig: AnimatorConfig? = null) :
    ExpandableSelectableItemView<InventoryItem>(context, animatorConfig, useDefaultLayout = false)
{
    val detailsUi: InventoryItemDetailsBinding
    private var pendingDetailsAnimation: ViewPropertyAnimator? = null

    init {
        val tempUi = InventoryItemBinding.inflate(LayoutInflater.from(context), this)
        ui = BootyCrateItemBinding.bind(tempUi.root)
        detailsUi = InventoryItemDetailsBinding.bind(tempUi.root)
        ui.editButton.setOnClickListener { toggleExpanded() }
        ui.checkBox.setInColorEditMode(true, animate = false)
        ui.amountEdit.minValue = 0
        this.animatorConfig = animatorConfig
    }

    override fun update(item: InventoryItem) {
        detailsUi.autoAddToShoppingListCheckBox.initIsChecked(item.autoAddToShoppingList)
        detailsUi.autoAddToShoppingListCheckBox.initColorIndex(item.color)
        detailsUi.autoAddToShoppingListAmountEdit.initValue(item.autoAddToShoppingListAmount)
        super.update(item)
    }

    override fun onExpandedChanged(expanded: Boolean, animate: Boolean) {
        if (!expanded && detailsUi.autoAddToShoppingListAmountEdit.ui.valueEdit.isFocused)
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
        val view = detailsUi.inventoryItemDetailsLayout
        if (!animate)
            view.isVisible = expanded
        else {
            val translationAmount = ui.checkBox.height * (if (expanded) 1f else -1f)

            if (expanded)
                view.translationY = -translationAmount
            else overlay.add(view)
            view.alpha = if (expanded) 0f else 1f
            view.isVisible = true

            val anim = view.animate().applyConfig(animatorConfig)
                .withLayer().alpha(if (expanded) 1f else 0f)
                .translationY(if (expanded) 0f else translationAmount)
            if (!expanded) anim.withEndAction {
                view.translationY = 0f
                view.isVisible = false
                overlay.remove(view)
                addView(view)
            }
            pendingDetailsAnimation = anim
        }
    }

    override fun runPendingAnimations() {
        super.runPendingAnimations()
        pendingDetailsAnimation?.start()
        pendingDetailsAnimation = null
    }
}