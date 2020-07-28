
package com.cliffracermerchant.bootycrate

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isEmpty
import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.ShapePath

class BottomAppBar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs) {
    private val arcQuarter = 90f
    private val angleRight = 0f
    private val angleDown = 90f
    private val angleLeft = 180f
    private val angleUp = 270f

    enum class CradleAlignmentMode { Start, Center, End }

    private val cradleLayoutId: Int
    lateinit var cradleLayout: ViewGroup

    private val materialShapeDrawable = MaterialShapeDrawable()
    var cradleStartEndMargin: Int
    var cradleContentsMargin: Int
    val cradleAlignmentMode: CradleAlignmentMode
    var cradleWidth: Int = 0
    var cradleDepth: Int
    var cradleHorizontalOffset: Int = 0
    var cradleTopCornerRadius: Int
    var cradleBottomCornerRadius: Int

    init {
        clipChildren = false
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomAppBar)

        cradleAlignmentMode = CradleAlignmentMode.values()[
            a.getInt(R.styleable.BottomAppBar_cradleAlignmentMode, CradleAlignmentMode.Center.ordinal)]
        cradleLayoutId = a.getResourceId(R.styleable.BottomAppBar_cradleLayoutId, -1)
        cradleDepth = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleDepth, 0)
        cradleTopCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleTopCornerRadius, 0)
        cradleBottomCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleBottomCornerRadius, 0)
        cradleContentsMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleContentsMargin, 0)
        val backgroundTint = a.getColor(R.styleable.BottomAppBar_backgroundTint, 0)
        cradleStartEndMargin = a.getDimensionPixelOffset(R.styleable.BottomAppBar_cradleStartEndMargin, 90)
        a.recycle()

        materialShapeDrawable.shapeAppearanceModel = ShapeAppearanceModel.builder().
                                                     setTopEdge(TopEdgeTreatment()).build()
        materialShapeDrawable.tintList = ColorStateList.valueOf(backgroundTint)
        background = materialShapeDrawable

        viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val parent = parent as ViewGroup?
                cradleLayout = if (parent?.findViewById<ViewGroup>(cradleLayoutId) != null)
                                   parent.findViewById(cradleLayoutId)
                               else LinearLayout(context)
                // Setting cradleLayout.layoutParams.width here is necessary to
                // prevent the cradle being drawn incorrectly the first time due to the
                // layoutParams.width being 0
                cradleLayout.layoutParams.width = cradleLayout.width
                redrawCradle()
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    fun redrawCradle() {
        if (cradleLayout.isEmpty()) return

        cradleWidth = cradleLayout.layoutParams.width + 2 * cradleContentsMargin
        cradleHorizontalOffset = when (cradleAlignmentMode) {
            CradleAlignmentMode.Start ->  left + cradleStartEndMargin
            CradleAlignmentMode.Center -> left + (width - cradleWidth) / 2
            CradleAlignmentMode.End ->    right - cradleStartEndMargin - cradleWidth
        }
        cradleLayout.x = left + cradleHorizontalOffset.toFloat() + cradleContentsMargin
        cradleLayout.y = top + cradleDepth.toFloat() - height - cradleContentsMargin
        materialShapeDrawable.invalidateSelf()
    }

    inner class TopEdgeTreatment : EdgeTreatment() {
        override fun getEdgePath(length: Float, center: Float,
                                 interpolation: Float, shapePath: ShapePath) {
            val start = cradleHorizontalOffset.toFloat()
            val end = start + cradleWidth
            val cradleVerticalSideLength = cradleDepth - cradleTopCornerRadius -
                                           cradleBottomCornerRadius.toFloat()

            shapePath.lineTo(start - cradleTopCornerRadius, 0f)
            shapePath.addArc(/*left*/       start - 2 * cradleTopCornerRadius,
                             /*top*/        0f,
                             /*right*/      start,
                             /*bottom*/     2f * cradleTopCornerRadius,
                             /*startAngle*/ angleUp,
                             /*sweepAngle*/ arcQuarter)
            if (cradleVerticalSideLength > 0)
                shapePath.lineTo(start, cradleDepth - cradleBottomCornerRadius.toFloat())
            shapePath.addArc(/*left*/       start,
                             /*top*/        cradleDepth - 2f * cradleBottomCornerRadius,
                             /*right*/      start + 2 * cradleBottomCornerRadius,
                             /*bottom*/     cradleDepth.toFloat(),
                             /*startAngle*/ angleLeft,
                             /*sweepAngle*/ -arcQuarter)
            shapePath.lineTo(end - cradleBottomCornerRadius, cradleDepth.toFloat())
            shapePath.addArc(/*left*/       end - 2 * cradleBottomCornerRadius,
                             /*top*/        cradleDepth - 2f * cradleBottomCornerRadius,
                             /*right*/      end,
                             /*bottom*/     cradleDepth.toFloat(),
                             /*startAngle*/ angleDown,
                             /*sweepAngle*/ -arcQuarter)
            if (cradleVerticalSideLength > 0)
                shapePath.lineTo(end, cradleDepth - cradleBottomCornerRadius.toFloat())
            shapePath.addArc(/*left*/       end,
                             /*top*/        0f,
                             /*right*/      end + 2 * cradleTopCornerRadius,
                             /*bottom*/     2f * cradleTopCornerRadius,
                             /*startAngle*/ angleLeft,
                             /*sweepAngle*/ arcQuarter)
            shapePath.lineTo(length, 0f)
        }
    }
}