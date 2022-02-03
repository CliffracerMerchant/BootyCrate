/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cliffracertech.bootycrate.R
import com.cliffracertech.bootycrate.activity.NavViewActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

/** Return a NotificationManager system service from the context. */
fun notificationManager(context: Context) =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
/** Return an AlarmManager system service from the context. */
fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

/** An object that, once initialized by calling init with an instance of Context,
 * can be used to either hide or show the soft input given a view instance using
 * the functions hide and show, and showWithDelay. */
object SoftKeyboard {
    private lateinit var imm: InputMethodManager
    fun init(context: Context) {
        imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    fun hide(view: View) = imm.hideSoftInputFromWindow(view.windowToken, 0)
    fun show(view: View) = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    /** Show the soft input after a given delay. which is useful when the soft input
     * should appear alongside a popup alert dialog (for some reason, requesting the
     * soft input to show at the same time as the dialog does not work). */
    fun showWithDelay(view: View, delay: Long = 50L) {
        view.handler.postDelayed({
            view.requestFocus()
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, delay)
    }
}

fun View.setHeight(height: Int) { bottom = top + height }

/** Return the provided dp amount in terms of pixels. */
fun Resources.dpToPixels(dp: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics)

/** Return the provided sp amount in terms of pixels. */
fun Resources.spToPixels(sp: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics)

/** Return the provided dp amount in terms of pixels. */
fun Context.dpToPixels(dp: Float) = resources.dpToPixels(dp)
/** Return the provided sp amount in terms of pixels. */
fun Context.spToPixels(sp: Float) = resources.spToPixels(sp)

private val typedValue = TypedValue()
/** Resolve the current theme's value for the provided int attribute. */
fun Resources.Theme.resolveIntAttribute(attr: Int): Int {
    resolveAttribute(attr, typedValue, true)
    return typedValue.data
}

/** Return the IntArray pointed to by @param arrayResId, resolving theme attributes if necessary. */
fun Context.getIntArray(arrayResId: Int): IntArray {
    val ta = resources.obtainTypedArray(arrayResId)
    val array = IntArray(ta.length()) {
        if (ta.peekValue(it).type == TypedValue.TYPE_ATTRIBUTE)
            theme.resolveIntAttribute(ta.peekValue(it).data)
        else ta.getColor(it, 0)
    }
    ta.recycle()
    return array
}

/** Add the nullable element to the list if it is not null, or do nothing otherwise. */
fun <T> MutableList<T>.add(element: T?) { if (element != null) add(element) }

val <T: View>BottomSheetBehavior<T>.isExpanded get() = state == BottomSheetBehavior.STATE_EXPANDED
val <T: View>BottomSheetBehavior<T>.isCollapsed get() = state == BottomSheetBehavior.STATE_COLLAPSED
val <T: View>BottomSheetBehavior<T>.isDragging get() = state == BottomSheetBehavior.STATE_DRAGGING
val <T: View>BottomSheetBehavior<T>.isSettling get() = state == BottomSheetBehavior.STATE_SETTLING
val <T: View>BottomSheetBehavior<T>.isHidden get() = state == BottomSheetBehavior.STATE_HIDDEN

/** Perform the given block without the receiver's LayoutTransition.
 * This is useful when changes need to be made instantaneously. */
fun ViewGroup.withoutLayoutTransition(block: () -> Unit) {
    val layoutTransitionBackup = layoutTransition
    layoutTransition = null
    block()
    layoutTransition = layoutTransitionBackup
}


/** A LinearLayout that allows settings a max height with the XML attribute maxHeight. */
class MaxHeightLinearLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    var maxHeight = -1
        set(value) { field = value; invalidate() }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightLinearLayout)
        maxHeight = a.getDimensionPixelSize(R.styleable.MaxHeightLinearLayout_maxHeight, -1)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxHeightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, maxHeightSpec)
    }
}

/** Set the View's padding, using the current values as defaults
 * so that not every value needs to be specified. */
fun View.setPadding(
    start: Int = paddingStart,
    top: Int = paddingTop,
    end: Int = paddingEnd,
    bottom: Int = paddingTop
) = setPaddingRelative(start, top, end, bottom)

/** Call the provided block each time the LifecycleOwner receiver
 * enters Lifecycle.State.STARTED, and cancel the block when the
 * receiver's Lifecycle.State falls below this level. */
fun LifecycleOwner.repeatWhenStarted(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED, block)
    }
}

/** Recollect the @param flow with the provided @param action each time the
 * receiver LifecycleOwner enters Lifecycle.State.STARTED, and cancel the
 * flow collection when the receiver's LifecycleState falls below this level. */
fun <T>LifecycleOwner.recollectWhenStarted(
    flow: Flow<T>,
    action: suspend (T) -> Unit) =
    repeatWhenStarted {
        flow.collect { action(it) }
    }

/** An extension function that allows a StateFlow<T> to
 * act as a delegate for an immutable value of type T. */
operator fun <T> StateFlow<T>.getValue(
    thisRef: Any,
    property: KProperty<*>
) = value

/** An extension function that, when used with a corresponding
 * StateFlow<T>.getValue implementation, allows a MutableStateFlow<T>
 * to act as a delegate for a mutable variable of type T. */
operator fun <T> MutableStateFlow<T>.setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: T
) { this.value = value }

/** Return a Flow<T> that contains the most recent value for the DataStore
 * preference pointed to by @param key, with a default value of @param
 * defaultValue. */
fun <T> DataStore<Preferences>.preferenceFlow(
    key: Preferences.Key<T>,
    defaultValue: T,
) = data.map { it[key] ?: defaultValue }

/** Return a Flow<T> that contains the most recent enum value for the DataStore
 * preference pointed to by @param key, with a default value of @param
 * defaultValue. @param key should be an Preferences.Key<Int> value whose
 * */
inline fun <reified T: Enum<*>> DataStore<Preferences>.enumPreferenceFlow(
    key: Preferences.Key<Int>,
    defaultValue: T
) = data.map { prefs ->
    val index = prefs[key] ?: defaultValue.ordinal
    enumValues<T>().getOrNull(index) ?: defaultValue
}

/** Return a MutableStateFlow<T> that contains the most recent value for the
 * preference pointed to by the parameter key, with a default value equal to
 * the parameter defaultValue. Changes to the returned MutableStateFlow's
 * value property will automatically be written to the receiver DataStore
 * object. */
fun <T> DataStore<Preferences>.mutablePreferenceFlow(
    key: Preferences.Key<T>,
    scope: CoroutineScope,
    defaultValue: T,
) = MutableStateFlow(defaultValue).apply {
    scope.launch {
        value = data.first()[key] ?: defaultValue
        collect { newValue ->
            edit { it[key] = newValue }
        }
    }
}

/** Return a MutableStateFlow<T> that contains the most recent enum value for
 * the Int preference pointed to by the parameter key, with a default value of
 * the parameter defaultValue. Changes to the returned MutableStateFlow's value
 * property will automatically be written to the receiver DataStore object. The
 * Preferences.Key<Int> should point to the preference that stores the index of
 * the current enum value's index.*/
inline fun <reified T: Enum<*>> DataStore<Preferences>.mutableEnumPreferenceFlow(
    key: Preferences.Key<Int>,
    scope: CoroutineScope,
    defaultValue: T,
) = MutableStateFlow(defaultValue).apply {
    scope.launch {
        val firstIndex = data.first()[key] ?: defaultValue.ordinal
        value = enumValues<T>()[firstIndex]
        collect { newValue ->
            edit { it[key] = newValue.ordinal }
        }
    }
}

/** Get the menu item at @param index, or null if the index is out of bounds. */
fun Menu.getItemOrNull(index: Int) = try { getItem(index) }
                                     catch (e: IndexOutOfBoundsException) { null }

/** A holder of a string resource, which is resolved by calling the method
 * resolve with a Context instance. Thanks to this SO post at
 * https://stackoverflow.com/a/65967451 for the idea. */
class StringResource(
    private val string: String?,
    @StringRes val stringResId: Int = 0,
    private val args: ArrayList<Any>?
) {
    data class Id(val id: Int)

    constructor(string: String): this(string, 0, null)
    constructor(@StringRes stringResId: Int): this(null, stringResId, null)
    constructor(@StringRes stringResId: Int, stringVar: String):
        this(null, stringResId, arrayListOf(stringVar))
    constructor(@StringRes stringResId: Int, intVar: Int):
        this(null, stringResId, arrayListOf(intVar))
    constructor(@StringRes stringResId: Int, stringVarId: Id):
        this(null, stringResId, arrayListOf(stringVarId))

    fun resolve(context: Context) = string ?: when(args) {
        null -> context.getString(stringResId)
        else -> {
            for (i in args.indices) {
                val it = args[i]
                if (it is Id)
                    args[i] = context.getString(it.id)
            }
            context.getString(stringResId, *args.toArray())
        }
    }
}

/** Replace the receiver fragment with the provided Fragment instance in the
 * containing activity. This function will not do anything if the fragment
 * is not attached to an activity or if the fragment's view's direct parent
 * is not the fragment container for the activity. If the fragment's activity
 * is an instance of NavViewActivity, it will execute the activity's
 * addSecondaryFragment method instead. */
fun Fragment.replaceSelfWith(fragment: Fragment) {
    val activity = activity ?: return
    if (activity is NavViewActivity)
        activity.addSecondaryFragment(fragment)
    else {
        val containerId = (view?.parent as? ViewGroup)?.id ?: return
        activity.supportFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null).commit()
    }
}