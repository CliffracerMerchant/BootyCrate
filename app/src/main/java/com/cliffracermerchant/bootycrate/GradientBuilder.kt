/* Copyright 2020 Nicholas Hochstetler
 * You may not use this file except in compliance with the Apache License
 * Version 2.0, obtainable at http://www.apache.org/licenses/LICENSE-2.0
 * or in the file LICENSE in the project's root directory. */

package com.cliffracermerchant.bootycrate

import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.min

/** A factory to create instances of Android.Graphics.LinearGradient or RadialGradient.
 *
 *  GradientBuilder creates instances of LinearGradient or RadialGradient using
 *  its public functions buildLinearGradient or buildRadialGradient. All of the
 *  properties of the gradients can be set in the constructor, or using the
 *  standard factory pattern methods that return the GradientBuilder instance
 *  being acted upon.
 *
 *  Property    LinearGradient                  RadialGradient
 *  -------------------------------------------------------------------
 *  x1, y1      coordinates of start point      coordinates of center
 *  x2, y2      coordinates of end point        x radius and y radius
 *  colors      colors to be used               same
 *  stops       stop points for the colors      same
 *  tileMode    Shader.TileMode to use          same */
data class GradientBuilder(
    var x1: Float = 0f,
    var y1: Float = 0f,
    var x2: Float = 1f,
    var y2: Float = 1f,
    var colors: IntArray = IntArray(0),
    var stops: FloatArray? = null,
    var tileMode: Shader.TileMode = Shader.TileMode.CLAMP
) {
    private val matrix = Matrix()
    fun setX1(x1: Float) = apply { this.x1 = x1 }
    fun setY1(y1: Float) = apply { this.y1 = y1 }
    fun setX2(x2: Float) = apply { this.x2 = x2 }
    fun setY2(y2: Float) = apply { this.y2 = y2 }
    fun setColors(colors: IntArray) = apply { this.colors = colors.clone() }
    fun setStops(stops: FloatArray) = apply { this.stops = stops.clone() }
    fun setTileMode(tileMode: Shader.TileMode) = apply { this.tileMode = tileMode }

    fun buildLinearGradient() = LinearGradient(x1, y1, x2, y2, colors, stops, tileMode)
    fun buildRadialGradient(): RadialGradient {
        if (x2 == y2)
            return RadialGradient(x1, y1, x2, colors, stops, tileMode)

        matrix.reset()
        val xScale = if (x2 > y2) x2 / y2 else 1f
        val yScale = if (y2 > x2) y2 / x2 else 1f
        matrix.setScale(xScale, yScale)
        val gradient = RadialGradient(x1 / xScale, y1 / yScale, min(x2, y2),
                                      colors, stops, tileMode)
        gradient.setLocalMatrix(matrix)
        return gradient
    }

    override fun equals(other: Any?): Boolean {
        if (other !is GradientBuilder) return false
        return super.equals(other) &&
               colors.contentEquals(other.colors) &&
               stops.contentEquals(other.stops)
    }

    override fun hashCode() =
        Triple(super.hashCode(), colors.hashCode(), stops.hashCode()).hashCode()
}
