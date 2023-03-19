/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.cliffracertech.bootycrate.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.reflect.KProperty

/** Return a NotificationManager system service from the context. */
fun notificationManager(context: Context) =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
/** Return an AlarmManager system service from the context. */
fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

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

/** Return the [Dp] amount converted to pixels, using the provided [density]. */
fun Dp.toPx(density: Density): Float = with(density) { toPx() }
/** Return the [Dp] amount rounded to the nearest pixel, using the provided [density]. */

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

fun <T> Flow<T>.collectAsState(initialValue: T, scope: CoroutineScope): State<T> {
    val state = mutableStateOf(initialValue)
    onEach { state.value = it }.launchIn(scope)
    return state
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
 * defaultValue. @param key should be an Preferences.Key<Int> instance whose
 * value indicates the ordinal of the current enum value .*/
inline fun <reified T: Enum<T>> DataStore<Preferences>.enumPreferenceFlow(
    key: Preferences.Key<Int>,
    defaultValue: T
) = data.map { prefs ->
    val index = prefs[key] ?: defaultValue.ordinal
    enumValues<T>().getOrNull(index) ?: defaultValue
}

/** Return a [State]`<T>` that contains the most recent value for the [DataStore]
 * preference pointed to by [key], with an initial value of [initialValue]. */
fun <T> DataStore<Preferences>.preferenceState(
    key: Preferences.Key<T>,
    initialValue: T,
    scope: CoroutineScope,
) : State<T> {
    val state = mutableStateOf(initialValue)
    data.map { it[key] ?: initialValue }
        .onEach { state.value = it }
        .launchIn(scope)
    return state
}

/** Return a [State]`<T>` that contains the most recent value for the
 * [DataStore] preference pointed to by [key], with a default value of
 * [defaultValue]. awaitPreferenceState will suspend until the first value
 * of the preference is returned. The provided default value will only be
 * used if the receiver [DataStore] does not have a value associated with
 * the provided key. */
suspend fun <T> DataStore<Preferences>.awaitPreferenceState(
    key: Preferences.Key<T>,
    defaultValue: T,
    scope: CoroutineScope,
) : State<T> {
    val flow = data.map { it[key] ?: defaultValue }
    val state = mutableStateOf(flow.first())
    flow.onEach { state.value = it }.launchIn(scope)
    return state
}

/** Return a [State]`<T>` that contains the most recent enum value for the
 * [DataStore] preference pointed to by [key], with an initial value
 * of [initialValue]. [key] is a [Preferences.Key]`<Int>` instance whose
 * value indicates the ordinal of the current enum value. */
inline fun <reified T: Enum<T>> DataStore<Preferences>.enumPreferenceState(
    key: Preferences.Key<Int>,
    scope: CoroutineScope,
    initialValue: T = enumValues<T>()[0],
): State<T> {
    val indexState by preferenceState(key, initialValue.ordinal, scope)
    val values = enumValues<T>()
    return derivedStateOf {
        values.getOrElse(indexState) { initialValue }
    }
}

/** Return a [State]`<T>` that contains the most recent enum value for the
 * [DataStore] preference pointed to by the parameter [key], with a default
 * value of [defaultValue]. [key] is a [Preferences.Key]`<Int>` instance
 * whose value indicates the ordinal of the current enum value.
 * awaitEnumPreferenceState will suspend until the first value of the enum
 * is read from the receiver [DataStore] object. The provided default value
 * will only be used if the receiver [DataStore] does not have a value
 * associated with the provided key. */
suspend inline fun <reified T: Enum<T>> DataStore<Preferences>.awaitEnumPreferenceState(
    key: Preferences.Key<Int>,
    scope: CoroutineScope,
    defaultValue: T = enumValues<T>()[0],
): State<T> {
    val indexState = awaitPreferenceState(key, defaultValue.ordinal, scope)
    val values = enumValues<T>()
    return derivedStateOf {
        values.getOrElse(indexState.value) { defaultValue }
    }
}