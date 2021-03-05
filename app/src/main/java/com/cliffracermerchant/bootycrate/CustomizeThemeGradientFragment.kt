/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.cliffracermerchant.bootycrate.databinding.CustomizeThemeGradientFragmentBinding

class CustomizeThemeGradientFragment : Fragment() {
    private lateinit var ui: CustomizeThemeGradientFragmentBinding
    private lateinit var fgGradientBuilder: GradientBuilder
    private lateinit var bgGradientBuilder: GradientBuilder
    private val hslColor = floatArrayOf(0f, 1f, 0.65f)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = CustomizeThemeGradientFragmentBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as GradientStyledMainActivity
        val topActionBar = activity.ui.topActionBar
        fgGradientBuilder = activity.topFgGradientBuilderCopy
        bgGradientBuilder = activity.topBgGradientBuilderCopy
        val blendColor = ContextCompat.getColor(activity, R.color.colorRecyclerViewItemLight)
        val blendAlpha = 150

        ui.gradientSlider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val arcDegrees = 60f
                for (i in fgGradientBuilder.colors.indices) {
                    hslColor[0] = progress - arcDegrees / fgGradientBuilder.colors.size * i
                    if (hslColor[0] < 0f) hslColor[0] += 360f
                    fgGradientBuilder.colors[i] = ColorUtils.HSLToColor(hslColor)
                    bgGradientBuilder.colors[i] = ColorUtils.compositeColors(
                        ColorUtils.setAlphaComponent(fgGradientBuilder.colors[i], blendAlpha), blendColor)
                }
                topActionBar.borderGradient = fgGradientBuilder.buildRadialGradient()
                topActionBar.backgroundGradient = bgGradientBuilder.buildRadialGradient()
                topActionBar.invalidate()
            }
        })

        activity.ui.checkoutButton.isEnabled = true
        activity.ui.checkoutButton.setOnClickListener(null)
        activity.ui.addButton.setOnClickListener(null)
        activity.ui.bottomNavigationBar.menu.getItem(0).isEnabled = false
        activity.ui.bottomNavigationBar.menu.getItem(3).isEnabled = false
        activity.showBottomAppBar(true)
    }

    override fun onDestroyView() {
        val context = context ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val prefKey = context.getString(R.string.pref_theme_gradient_value)
        prefs.edit().putInt(prefKey, ui.gradientSlider.progress).apply()

        val activity = requireActivity() as MainActivity
        activity.showBottomAppBar(false)
        activity.ui.addButton.isEnabled = true
        activity.ui.bottomNavigationBar.menu.getItem(0).isEnabled = true
        activity.ui.bottomNavigationBar.menu.getItem(3).isEnabled = true
        activity.ui.bottomNavigationBar.isEnabled = true
        super.onDestroyView()
    }
}