/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewPropertyAnimator
import androidx.annotation.CallSuper
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.BootyCrateItem
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.add
import com.cliffracertech.bootycrate.utils.applyConfig
import com.cliffracertech.bootycrate.view.AnimatedStrikeThroughTextFieldEdit

/**
 * A BootyCrateItemView subclass that provides an interface for a selection and expansion of the view.
 *
 * ExpandableSelectableItemView extends BootyCrateItemView by providing an
 * interface for expansion of extra details, a custom background to indicate a
 * selected state, and with an update override that will update the view to
 * reflect the selection and expansion state of the BootyCrateItem passed to it.
 * The selected state is set using the View property isSelected. ExpandableSelectableItemView
 * indicates its selected state by drawing a gradient outline around its edge.
 *
 * The interface for item expansion consists of expand, collapse, setExpanded,
 * and toggleExpanded. If subclasses need to alter the visibility of additional
 * views during expansion or collapse, they can override the function
 * onExpandedChanged with their additional changes. setExpanded will animate
 * the changes inside the view unless it is called with the parameter animate
 * set to false.
 *
 * In order to allow for easier synchronization with concurrent animations
 * outside the view, all of ExpandableSelectableItemView's internal animations
 * use the AnimatorConfig defined by the property animatorConfig. If delaying
 * the animations is also required to synchronize them with other animations,
 * the property startAnimationsImmediately can be set to false. In this case
 * the animations will be prepared and stored, and can be played by calling
 * runPendingAnimations.
 */
@SuppressLint("ViewConstructor", "Recycle")
open class ExpandableSelectableItemView<T: BootyCrateItem>(
    context: Context,
    animatorConfig: AnimatorConfig? = null,
    useDefaultLayout: Boolean = true,
) : BootyCrateItemView<T>(context, useDefaultLayout),
    ExpandableItemAnimator.ExpandableRecyclerViewItem
{
    var isExpanded = false
        private set
    private var isLinkedToAnotherItem = false

    var animatorConfig: AnimatorConfig? = null
        set(value) { field = value
                     ui.nameEdit.animatorConfig = value
                     ui.extraInfoEdit.animatorConfig = value
                     ui.amountEdit.animatorConfig = value }
    var startAnimationsImmediately = true

    private val pendingAnimations = mutableListOf<Animator>()
    private val pendingViewPropAnimations = mutableListOf<ViewPropertyAnimator>()

    override fun runPendingAnimations() { pendingAnimations.forEach { it.start() }
                                          pendingAnimations.clear()
                                          pendingViewPropAnimations.forEach { it.start() }
                                          pendingViewPropAnimations.clear() }

    init {
        background = ContextCompat.getDrawable(context, R.drawable.recycler_view_item)
        clipChildren = false
        if (useDefaultLayout) {
            ui.editButton.setOnClickListener { toggleExpanded() }
            this.animatorConfig = animatorConfig
        }
    }

    override fun update(item: T) {
        super.update(item)
        setExpanded(item.isExpanded, animate = false)
        isSelected = item.isSelected
        isLinkedToAnotherItem = item.isLinked
        if (isExpanded)
            ui.linkIndicator.isVisible = isLinkedToAnotherItem
    }

    override fun updateContentDescriptions(itemName: String) {
        super.updateContentDescriptions(itemName)
        val editDescResId = if (isExpanded) R.string.collapse_item_description
                            else            R.string.edit_item_description
        ui.editButton.contentDescription = context.getString(editDescResId, itemName)
        ui.linkIndicator.contentDescription = context.getString(R.string.item_is_linked_description, itemName)
    }

    /** Update the visibility of the isLinked indicator. */
    fun updateIsLinked(isLinked: Boolean, animate: Boolean = true) {
        isLinkedToAnotherItem = isLinked
        if (isExpanded) updateIsLinkedIndicatorState(isLinked, animate, translate = false)
    }

    fun select() { isSelected = true }
    fun deselect() { isSelected = false }

    fun toggleExpanded() = setExpanded(!isExpanded)
    @CallSuper open fun onExpandedChanged(expanded: Boolean = true, animate: Boolean = true) {
        val itemName = ui.nameEdit.text.toString()
        val descResId = if (expanded) R.string.collapse_item_description
                        else          R.string.edit_item_description
        ui.editButton.contentDescription = context.getString(descResId, itemName)
    }

    override fun setExpanded(expanding: Boolean, animate: Boolean) {
        if (isExpanded == expanding) return
     /* Both LayoutTransition and TransitionManager.beginDelayedTransition
        unfortunately don't seem to animate all the expand/collapse changes
        correctly, and do not provide a way to delay the animations so that
        they can be synchronized with the RecyclerView animations. MotionLayout
        had horrific performance in this case on both emulated and real devices
        when tested (around 4-5 fps). Unless another alternative presents itself,
        the internal expand / collapse animations must be done manually. */
        isExpanded = expanding

        val nameAnimInfo = updateNameEditState(expanding, animate)
        val extraInfoAnimInfo = updateExtraInfoState(expanding, animate)
        updateAmountEditState(expanding, animate)
        adjustNameExtraInfoAndCheckbox(expanding, nameAnimInfo, extraInfoAnimInfo)
        linkedIndicatorShouldBeVisible = expanding && isLinkedToAnotherItem
        updateIsLinkedIndicatorState(linkedIndicatorShouldBeVisible, animate, translate = true)
        onExpandedChanged(expanding, animate)
        updateEditButtonState(expanding, animate)
        if (animate && startAnimationsImmediately)
            runPendingAnimations()
    }

    /** The cached new value for the height of the nameEdit; only to
     *  be used in the body of setExpanded or one of its sub-functions. */
    private var nameNewHeight = 0
    /** Update the editable state of nameEdit, animating if param
     *  animate == true, and return the TextFieldEdit.AnimInfo for
     *  the animation, or null if no animation occurred. */
    private fun updateNameEditState(
        expanding: Boolean,
        animate: Boolean
    ): AnimatedStrikeThroughTextFieldEdit.AnimInfo? {
        val animInfo = ui.nameEdit.setEditable(
            editable = expanding,
            animate = animate,
            startAnimationsImmediately = false
        ) ?: return null

        pendingAnimations.add(animInfo.translateAnimator)
        pendingAnimations.add(animInfo.underlineAnimator)
        val minHeight = if (!expanding) 0 else
            resources.getDimensionPixelSize(R.dimen.editable_text_field_min_height)
        nameNewHeight = maxOf(ui.nameEdit.measuredHeight, minHeight)
        return animInfo
    }

    /** The cached new value for the height of the extraInfoEdit; only to
     *  be used in the body of setExpanded or one of its sub-functions. */
    private var extraInfoNewHeight = 0
    private var hidingExtraInfo = false
    /** Update the editable state of extraInfoEdit, animating if param animate ==
     *  true and the extraInfoEdit is not blank, and returning the TextFieldEdit.AnimInfo
     *  for the animation, or null if no animation occurred. */
    private fun updateExtraInfoState(
        expanding: Boolean,
        animate: Boolean
    ): AnimatedStrikeThroughTextFieldEdit.AnimInfo? {
        val extraInfoIsBlank = ui.extraInfoEdit.text.isNullOrBlank()
        val extraInfoUnderlineUpdateMode =
            if (!extraInfoIsBlank)
                AnimatedStrikeThroughTextFieldEdit.UnderlineAlphaUpdateMode.Animate
            else if (expanding)
                AnimatedStrikeThroughTextFieldEdit.UnderlineAlphaUpdateMode.BeforeAnimation
            else
                AnimatedStrikeThroughTextFieldEdit.UnderlineAlphaUpdateMode.AfterAnimation

        val animInfo = ui.extraInfoEdit.setEditable(
            editable = expanding,
            animate = animate,
            underlineAlphaUpdateMode = extraInfoUnderlineUpdateMode,
            startAnimationsImmediately = false)

        if (!animate) {
            ui.extraInfoEdit.isVisible = expanding || !extraInfoIsBlank
            ui.extraInfoEdit.alpha = if (expanding || !extraInfoIsBlank) 1f else 0f
            ui.extraInfoEdit.translationY = 0f
            return null
        }
        pendingAnimations.add(animInfo!!.translateAnimator)
        pendingAnimations.add(animInfo.underlineAnimator)

        if (extraInfoIsBlank) {
            hidingExtraInfo = !expanding
            ui.extraInfoEdit.isVisible = true
            val anim = ui.extraInfoEdit.animate().applyConfig(animatorConfig)
                                            .alpha(if (expanding) 1f else 0f)
            if (!expanding) anim.withEndAction {
                if (ui.extraInfoEdit.alpha == 0f && hidingExtraInfo) {
                    ui.extraInfoEdit.isVisible = false
                    hidingExtraInfo = false
                }
            }
            pendingViewPropAnimations.add(anim)
        }

        // We have to adjust the extraInfoEdit starting translation by the
        // height change of the nameEdit to get the correct translation amount.
        val adjust = ui.nameEdit.height - nameNewHeight.toFloat()
        ui.extraInfoEdit.translationY += adjust
        animInfo.adjustTranslationStartEnd(adjust, 0f)

        val minHeight = if (!expanding) 0 else
            resources.getDimensionPixelSize(R.dimen.editable_text_field_min_height)
        extraInfoNewHeight = maxOf(ui.extraInfoEdit.measuredHeight, minHeight)
        return animInfo
    }

    /** Update the editable state of amountEdit, animating if param animate == true. */
    private fun updateAmountEditState(expanding: Boolean, animate: Boolean) {
        ui.amountEditSpacer.isVisible = !expanding
        val amountEditAnimInfo = ui.amountEdit.setValueIsFocusable(
            focusable = expanding,
            animate = animate,
            startAnimationsImmediately = false
        ) ?: return
        pendingAnimations.addAll(amountEditAnimInfo.animators)

        // IntegerEdit's internal translation animation will only take into account
        // its width change. We have to adjust the start values to take into account
        // the amountEdit's left/start change as well.
        val amountNewRight = if (expanding) ui.editButton.right
                             else           ui.editButton.left
        val amountNewLeft = amountNewRight - amountEditAnimInfo.newWidth
        val amountLeftChange = amountNewLeft - ui.amountEdit.left
        ui.amountEdit.translationX -= amountLeftChange
        val anim = ui.amountEdit.animate().translationX(0f).applyConfig(animatorConfig)
        pendingViewPropAnimations.add(anim)

        // Because their ends are constrained to amountEdit's start, nameEdit and
        // extraInfoEdit will need to have their end values animated as well.
        val start = ui.nameEdit.right
        val end = start - ui.amountEdit.translationX.toInt()
        textFieldLockedRight = start
        ValueAnimator.ofInt(start, end).apply {
            applyConfig(animatorConfig)
            doOnEnd { if (textFieldLockedRight == end) textFieldLockedRight = null }
            addUpdateListener { textFieldLockedRight = it.animatedValue as Int
                                ui.nameEdit.right = it.animatedValue as Int
                                ui.extraInfoEdit.right = it.animatedValue as Int }
            pendingAnimations.add(this)
        }
    }

    /** Adjust the name and extra info edit translation animations to take into
     * account the change in their top values, and animate the checkbox's top change. */
    private fun adjustNameExtraInfoAndCheckbox(
        expanding: Boolean,
        nameAnimInfo: AnimatedStrikeThroughTextFieldEdit.AnimInfo?,
        extraInfoAnimInfo: AnimatedStrikeThroughTextFieldEdit.AnimInfo?
    ) {
        if (nameAnimInfo == null || extraInfoAnimInfo == null) return
        // Adjust name and extra info translation amounts by the change in the top value of the nameEdit.
        // The values that end in Before represent the given value (e.g. the new top value
        // of the name edit) before the extra info disappears, if it is going to. The values
        // ending in After represent the same value after the extra info disappears.
        val textFieldsNewHeightBefore = nameNewHeight + extraInfoNewHeight
        val extraInfoIsDisappearing = ui.extraInfoEdit.text.isNullOrBlank() && !expanding
        val textFieldsNewHeightAfter = nameNewHeight + if (extraInfoIsDisappearing) 0
                                                       else extraInfoNewHeight

        val textFieldsWillBeShorterThanCheckboxBefore = textFieldsNewHeightBefore < ui.checkBox.height
        val textFieldsWillBeShorterThanCheckboxAfter = textFieldsNewHeightAfter < ui.checkBox.height

        val nameNewTopAfter = paddingTop + if (!textFieldsWillBeShorterThanCheckboxAfter) 0
                                           else (ui.checkBox.height - textFieldsNewHeightAfter) / 2

        val nameTopChange = nameNewTopAfter - ui.nameEdit.top.toFloat()
        ui.nameEdit.translationY -= nameTopChange
        ui.extraInfoEdit.translationY -= nameTopChange
        nameAnimInfo.adjustTranslationStartEnd(-nameTopChange, 0f)
        extraInfoAnimInfo.adjustTranslationStartEnd(-nameTopChange, 0f)

        if (extraInfoIsDisappearing) {
            // If the extra info is disappearing, the translation amounts additionally
            // need to be modified by the difference in the name edit's top value before
            // and after the extraInfoEdit disappears.
            val nameNewTopBefore = paddingTop + if (!textFieldsWillBeShorterThanCheckboxBefore) 0
                                                else (ui.checkBox.height - textFieldsNewHeightBefore) / 2
            val startEndAdjust = nameNewTopAfter - nameNewTopBefore.toFloat()

            ui.nameEdit.translationY += startEndAdjust
            ui.extraInfoEdit.translationY += startEndAdjust
            // For some reason the top change is sometimes different by 1f when the view
            // is collapsing compared to when it is expanding. This adjustment of 1f for
            // the end value corrects for this. Obviously this arbitrary adjustment isn't
            // ideal, but in the interest of not wasting time on such a small bug, it
            // will have to do for now.
            nameAnimInfo.adjustTranslationStartEnd(startEndAdjust, startEndAdjust + 1f)
            extraInfoAnimInfo.adjustTranslationStartEnd(startEndAdjust, startEndAdjust + 1f)
            nameAnimInfo.translateAnimator.doOnEnd { ui.nameEdit.translationY = 0f }
            extraInfoAnimInfo.translateAnimator.doOnEnd { ui.nameEdit.translationY = 0f }
        }

        // Animate checkbox top change, if any
        val checkBoxNewTopAfter = paddingTop + if (textFieldsWillBeShorterThanCheckboxAfter) 0
                                               else (textFieldsNewHeightAfter - ui.checkBox.height) / 2
        val startAdjust = checkBoxNewTopAfter - ui.checkBox.top.toFloat()
        val startEndAdjust = if (!extraInfoIsDisappearing) 0f else {
            val checkBoxNewTopBefore = paddingTop + if (textFieldsWillBeShorterThanCheckboxBefore) 0
                                                    else (textFieldsNewHeightBefore - ui.checkBox.height) / 2
            checkBoxNewTopAfter - checkBoxNewTopBefore.toFloat()
        }
        ui.checkBox.translationY -= startAdjust - startEndAdjust
        val anim = ui.checkBox.animate().translationY(startEndAdjust).applyConfig(animatorConfig)
                                        .withEndAction { ui.checkBox.translationY = 0f }
        pendingViewPropAnimations.add(anim)
    }

    private var linkedIndicatorShouldBeVisible = false
    /** Update the visibility of the isLinked indicator, using an alpha
     * animation if param animate == true, and additionally animating
     * the translationY if param translate == true. */
    private fun updateIsLinkedIndicatorState(showing: Boolean, animate: Boolean, translate: Boolean) {
        linkedIndicatorShouldBeVisible = showing && isLinkedToAnotherItem
        val endAlpha = if (linkedIndicatorShouldBeVisible) 1f else 0f
        val endTransY = ui.linkIndicator.layoutParams.height /
                if (linkedIndicatorShouldBeVisible) 1f else 2f
        if (!animate) {
            ui.linkIndicator.isVisible = linkedIndicatorShouldBeVisible
            ui.linkIndicator.alpha = endAlpha
            if (translate) ui.linkIndicator.translationY = endTransY
            return
        }
        if (!translate && showing)
            ui.linkIndicator.translationY = endTransY
        ui.linkIndicator.isVisible = true
        val anim = ui.linkIndicator.animate().alpha(endAlpha)
            .withLayer().applyConfig(animatorConfig)
            .withEndAction { ui.linkIndicator.isVisible = linkedIndicatorShouldBeVisible ||
                                                          ui.linkIndicator.alpha != 0f }
        if (translate) anim.translationY(endTransY)
        pendingViewPropAnimations.add(anim)
    }

    private fun updateEditButtonState(expanding: Boolean, animate: Boolean) {
        // editButton uses a state list animator with state_activated as the trigger.
        if (!animate)
            ui.editButton.isActivated = expanding
        else {
            val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            measure(wrapContentSpec, wrapContentSpec)
            val heightChange = measuredHeight - height
            ui.editButton.translationY -= heightChange
            pendingViewPropAnimations.add(ui.editButton.animate()
                .translationY(0f).applyConfig(animatorConfig)
                .withStartAction { ui.editButton.isActivated = expanding })
        }
    }

    // This onMeasure override makes the view not count the extra info edit towards
    // its measured height if the extra info edit is in the process of being hidden.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (heightMeasureSpec != MeasureSpec.UNSPECIFIED || !hidingExtraInfo) {
            layoutParams.height = LayoutParams.WRAP_CONTENT
            return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            val height = paddingTop + paddingBottom + maxOf(ui.checkBox.height, nameNewHeight)
            setMeasuredDimension(measuredWidth, height)
            layoutParams.height = height
        }
    }

    /** For some reason both nameEdit and extraInfoEdit have their width set
     * to their new expanded width sometime after setExpanded but before the
     * end animations are started, causing a visual flicker. The property
     * textFieldLockedRight, when set to a non-null value, prevents this
     * resize from taking place, and in turn prevents the flicker. */
    private var textFieldLockedRight: Int? = null
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val textFieldLockedRight = textFieldLockedRight ?: return
        ui.nameEdit.let { it.layout(it.left, it.top, textFieldLockedRight, it.bottom) }
        ui.extraInfoEdit.let { it.layout(it.left, it.top, textFieldLockedRight, it.bottom) }
    }
}