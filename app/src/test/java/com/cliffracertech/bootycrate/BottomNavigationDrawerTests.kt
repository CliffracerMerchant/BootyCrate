/* Copyright 2021 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */
package com.cliffracertech.bootycrate

import android.content.Context
import android.graphics.Rect
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.cliffracertech.bootycrate.utils.dpToPixels
import com.cliffracertech.bootycrate.view.BottomNavigationDrawer
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BottomNavigationDrawerTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val rect = Rect()
    //private lateinit var instance: BottomNavigationDrawer
    //private fun waitForAnimationsToFinish() = Shadows.shadowOf(Looper.getMainLooper()).idle()

    private fun instance(vararg attrs: Pair<Int, String>): BottomNavigationDrawer {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val attrSet = Robolectric.buildAttributeSet()
        for (attr in attrs)
            attrSet.addAttribute(attr.first, attr.second)
        return BottomNavigationDrawer(activity, attrSet.build())
    }

    @Test fun initialPeekHeight() {
        instance(Pair(R.attr.behavior_peekHeight, "50dp")).doOnNextLayout {
            it.getHitRect(rect)
            assertThat(rect.height()).isEqualTo(context.resources.dpToPixels(50f))
        }
    }

    @Test fun expandedHeight() {
        instance().doOnNextLayout {
            (it as BottomNavigationDrawer).expand()
            it.getHitRect(rect)
            assertThat(rect.height()).isEqualTo(it.height)
        }
    }

    @Test fun isHideableXMLvalues() {
        var instance = instance()
        assertThat(instance.isHideable).isEqualTo(BottomNavigationDrawer.IsHideable.Yes)

        for (value in BottomNavigationDrawer.IsHideable.values()) {
            instance = instance(Pair(R.attr.isHideable, value.ordinal.toString()))
            assertThat(instance.isHideable).isEqualTo(value)
        }
    }

    @Test fun expandCollapseHideShow() {
        val instance = instance()
        assertThat(instance.isCollapsed).isTrue()

        instance.expand()
        assertThat(instance.isExpanded).isTrue()

        instance.collapse()
        assertThat(instance.isCollapsed).isTrue()

        instance.hide()
        assertThat(instance.isHidden).isTrue()

        instance.show()
        assertThat(instance.isHidden).isFalse()
        assertThat(instance.isCollapsed).isTrue()
    }

    @Test fun isHideableValues() {
        var instance = instance(Pair(R.attr.isHideable, BottomNavigationDrawer.IsHideable.No.ordinal.toString()))
        instance.hide()
        assertThat(instance.isHidden).isFalse()

        instance = instance(Pair(R.attr.isHideable, BottomNavigationDrawer.IsHideable.OnlyByApp.ordinal.toString()))
        instance.hide()
        assertThat(instance.isHidden).isTrue()
    }
}