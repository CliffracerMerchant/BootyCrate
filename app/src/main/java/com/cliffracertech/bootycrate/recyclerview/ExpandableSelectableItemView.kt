/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.recyclerview

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.ViewPropertyAnimator
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.database.BootyCrateItem
import com.cliffracertech.bootycrate.utils.AnimatorConfig
import com.cliffracertech.bootycrate.utils.applyConfig
import com.cliffracertech.bootycrate.utils.intValueAnimator
import com.cliffracertech.bootycrate.view.AnimatedStrikeThroughTextFieldEdit

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
@SuppressLint("ViewConstructor", "Recycle")
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
        isLinkedToAnotherItem = item.isLinked
        if (isExpanded)
            ui.linkIndicator.isVisible = isLinkedToAnotherItem
    }

    /** Update the visibility of the isLinked indicator. */
    fun updateIsLinked(isLinked: Boolean, animate: Boolean = true) {
        isLinkedToAnotherItem = isLinked
        if (isExpanded) updateIsLinkedIndicatorState(isLinked, animate, translate = false)
    }

    private var _isInSelectedState = false
    val isInSelectedState get() = _isInSelectedState
    fun select() = setSelectedState(true)
    fun deselect() = setSelectedState(false)
    fun setSelectedState(selected: Boolean, animate: Boolean = true) {
        _isInSelectedState = selected
        if (animate) intValueAnimator(setter = gradientOutline::setAlpha,
                                      from = gradientOutline.alpha,
                                      to = if (selected) 255 else 0,
                                      config = animatorConfig).start()
        else gradientOutline.alpha = if (selected) 255 else 0
    }

    fun toggleExpanded() = setExpanded(!isExpanded)
    open fun onExpandedChanged(expanded: Boolean = true, animate: Boolean = true) { }

    override fun setExpanded(expanding: Boolean, animate: Boolean) {
     /* Both LayoutTransition and TransitionManager.beginDelayedTransition
        unfortunately don't seem to animate all the expand/collapse changes
        correctly, and do not provide a way to delay the animations so that
        they can be synchronized with the RecyclerView animations. MotionLayout
        had horrific performance in this case on both emulated and real devices
        when tested (around 4-5 fps). Unless another alternative presents itself,
        the internal expand / collapse animations must be done manually. */
        _isExpanded = expanding

        val nameAnimInfoAndNewHeight = updateNameEditState(expanding, animate)
        val newNameHeight = nameAnimInfoAndNewHeight?.newHeight ?: 0
        val extraInfoAnimInfoAndNewHeight = updateExtraInfoState(expanding, animate, newNameHeight)
        updateAmountEditState(expanding, animate)
        if (animate) adjustNameExtraInfoAndCheckbox(expanding, nameAnimInfoAndNewHeight!!,
                                                    extraInfoAnimInfoAndNewHeight!!)
        linkedIndicatorShouldBeVisible = expanding && isLinkedToAnotherItem
        updateIsLinkedIndicatorState(linkedIndicatorShouldBeVisible, animate, translate = true)
        onExpandedChanged(expanding, animate)
        updateEditButtonState(expanding, animate)
        if (animate && startAnimationsImmediately)
            runPendingAnimations()
    }

    private data class TextFieldAnimInfoAndNewHeight(
        val animInfo: AnimatedStrikeThroughTextFieldEdit.AnimInfo,
        val newHeight: Int)

    /** Update the editable state of nameEdit, animating if param animate == true,
     * and return the TextFieldEdit.AnimInfo for the animation and the new height
     * of the nameEdit, or null if no animation occurred. */
    private fun updateNameEditState(
        expanding: Boolean,
        animate: Boolean
    ): TextFieldAnimInfoAndNewHeight? {
        val animInfo = ui.nameEdit.setEditable(expanding, animate, false) ?: return null
        pendingAnimations.add(animInfo.translateAnimator)
        pendingAnimations.add(animInfo.underlineAnimator)
        val minHeight = if (!expanding) 0 else
            resources.getDimensionPixelSize(R.dimen.editable_text_field_min_height)
        val newHeight = maxOf(ui.nameEdit.measuredHeight, minHeight)
        return TextFieldAnimInfoAndNewHeight(animInfo, newHeight)
    }

    /** Update the editable state of extraInfoEdit, animating if param animate == true and
     * the extraInfoEdit is not blank, and returning the TextFieldEdit.AnimInfo for the
     * animation and the new height of the extraInfoEdit, or null if no animation occurred. */
    private fun updateExtraInfoState(
        expanding: Boolean,
        animate: Boolean,
        nameNewHeight: Int
    ): TextFieldAnimInfoAndNewHeight? {
        val extraInfoIsBlank = ui.extraInfoEdit.text.isNullOrBlank()
        val animInfo = ui.extraInfoEdit.setEditable(expanding, animate, false)
        if (!animate) {
            ui.extraInfoEdit.isVisible = expanding || !extraInfoIsBlank
            return null
        }
        pendingAnimations.add(animInfo!!.translateAnimator)
        pendingAnimations.add(animInfo.underlineAnimator)

        if (extraInfoIsBlank) {
            ui.extraInfoEdit.isVisible = true
            val anim = ui.extraInfoEdit.animate().applyConfig(animatorConfig)
                                            .alpha(if (expanding) 1f else 0f)
            if (!expanding) anim.withEndAction {
                if (ui.extraInfoEdit.alpha == 0f)
                    ui.extraInfoEdit.isVisible = false
            }
            pendingViewPropAnimations.add(anim)
        }

        // We have to adjust the extraInfoEdit starting translation by the
        // height change of the nameEdit to get the correct translation amount.
        val adjust = ui.nameEdit.height - nameNewHeight.toFloat()
        ui.extraInfoEdit.translationY += adjust
        animInfo.adjustTranslationStartEnd(adjust, 0f)

        val newHeight = if (extraInfoIsBlank && !expanding) 0 else
            maxOf(ui.extraInfoEdit.measuredHeight, if (!expanding) 0 else
                resources.getDimensionPixelSize(R.dimen.editable_text_field_min_height))
        return TextFieldAnimInfoAndNewHeight(animInfo, newHeight)
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
        nameAnimInfoAndNewHeight: TextFieldAnimInfoAndNewHeight,
        extraInfoAnimInfoAndNewHeight: TextFieldAnimInfoAndNewHeight
    ) {
        // adjust name and extra info translation amounts
        val nameAndExtraInfoNewHeight = nameAnimInfoAndNewHeight.newHeight +
                                        extraInfoAnimInfoAndNewHeight.newHeight
        val nameAndExtraInfoWillBeSmallerThanCheckbox = (nameAndExtraInfoNewHeight) < ui.checkBox.height

        val extraInfoIsDisappearing = ui.extraInfoEdit.text.isNullOrBlank() && !expanding
        val nameNewTop = paddingTop + if (!nameAndExtraInfoWillBeSmallerThanCheckbox) 0
                                      else (ui.checkBox.height - nameAndExtraInfoNewHeight) / 2
        val nameTopChange = nameNewTop - ui.nameEdit.top
        if (nameTopChange != 0) {
            val adjust = nameTopChange * -1f
            if (!extraInfoIsDisappearing) {
                ui.nameEdit.translationY += adjust
                ui.extraInfoEdit.translationY += adjust
                nameAnimInfoAndNewHeight.animInfo.adjustTranslationStartEnd(adjust, 0f)
                extraInfoAnimInfoAndNewHeight.animInfo.adjustTranslationStartEnd(adjust, 0f)
            } else {
                nameAnimInfoAndNewHeight.animInfo.adjustTranslationStartEnd(0f, -adjust)
                nameAnimInfoAndNewHeight.animInfo.translateAnimator
                    .doOnEnd { ui.nameEdit.translationY = 0f }
            }
        }

        // animate checkbox top change, if any
        val checkBoxNewTop = paddingTop + if (nameAndExtraInfoWillBeSmallerThanCheckbox) 0
                                          else (nameAndExtraInfoNewHeight - ui.checkBox.height) / 2
        val checkBoxTopChange = checkBoxNewTop - ui.checkBox.top.toFloat()
        ui.checkBox.translationY -= checkBoxTopChange
        val anim = ui.checkBox.animate().translationY(0f).applyConfig(animatorConfig)
        pendingViewPropAnimations.add(anim)
    }

    private var linkedIndicatorShouldBeVisible: Boolean = false
    /** Update the visibility of the isLinked indicator, using an alpha
     * animation if param animate == true, and additionally animating
     * the translationY if param translate == true. */
    private fun updateIsLinkedIndicatorState(showing: Boolean, animate: Boolean, translate: Boolean) {
        linkedIndicatorShouldBeVisible = showing && isLinkedToAnotherItem
        val endAlpha = if (linkedIndicatorShouldBeVisible) 1f else 0f
        val transYEnd = ui.linkIndicator.layoutParams.height /
                if (linkedIndicatorShouldBeVisible) 1f else 2f
        if (!animate) {
            ui.linkIndicator.isVisible = linkedIndicatorShouldBeVisible
            ui.linkIndicator.alpha = endAlpha
            if (translate) ui.linkIndicator.translationY = transYEnd
            return
        }
        ui.linkIndicator.isVisible = true
        val anim = ui.linkIndicator.animate().alpha(endAlpha)
            .withLayer().applyConfig(animatorConfig)
            .withEndAction { ui.linkIndicator.isVisible = !linkedIndicatorShouldBeVisible ||
                                                          ui.linkIndicator.alpha != 0f }
        if (translate) anim.translationY(transYEnd)
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

 /** For some reason both nameEdit and extraInfoEdit have their width set
    to their new expanded width sometime after setExpanded but before the
    end animations are started, causing a visual flicker. The property
    textFieldLockedRight, when set to a non-null value, prevents this
    resize from taking place, and in turn prevents the flicker. */
    private var textFieldLockedRight: Int? = null
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val textFieldLockedRight = textFieldLockedRight ?: return
        ui.nameEdit.let { it.layout(it.left, it.top, textFieldLockedRight, it.bottom) }
        ui.extraInfoEdit.let { it.layout(it.left, it.top, textFieldLockedRight, it.bottom) }
    }
}