package com.cbruegg.agendafortodoist.util

import android.graphics.Color
import android.support.annotation.ColorInt
import android.support.v7.widget.RecyclerView
import android.support.wear.widget.WearableLinearLayoutManager
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class CenterScrollLayoutCallback(
        /**
         * Invoked for applying effects with the first parameter
         * being the child and the second parameter being the distance to the center
         * divided by the parent height.
         */
        private val distanceListeners: List<(View, Float) -> Unit> = emptyList()
) : WearableLinearLayoutManager.LayoutCallback() {

    override fun onLayoutFinished(child: View, parent: RecyclerView) {
        // Figure out % progress from top to bottom
        val centerOffset = child.height / 2.0f / parent.height
        val yRelativeToCenterOffset = child.y / parent.height + centerOffset

        // Normalize for center
        val distanceToCenter = Math.abs(0.5f - yRelativeToCenterOffset)

        distanceListeners.forEach { it(child, distanceToCenter) }
    }

}

class ScaleListener : (View, Float) -> Unit {

    private val maxScale = 0.65f

    override fun invoke(child: View, distanceToCenter: Float) {
        // Adjust to the maximum scale
        val scale = 1 - Math.min(maxScale, distanceToCenter)

        child.scaleX = scale
        child.scaleY = scale
    }
}

/**
 * The further away from the center the TextView goes,
 * the more the color goes from [from] to [to].
 */
class ColorScaleListener(
        @ColorInt val from: Int,
        @ColorInt val to: Int,
        private val textViewSelector: (View) -> TextView
) : (View, Float) -> Unit {

    private val fromHsv = FloatArray(3).apply { Color.colorToHSV(from, this) }
    private val toHsv = FloatArray(3).apply { Color.colorToHSV(to, this) }

    override fun invoke(child: View, distanceToCenter: Float) {
        val max = Math.max(distanceToCenter, 0.6f) // We don't know how far this can go off-center
        val progress = (distanceToCenter) / max

        val interpolated = FloatArray(3) { fromHsv[it] + ((toHsv[it] - fromHsv[it]) * progress) }
        val newColor = Color.HSVToColor(interpolated)
        textViewSelector(child).setTextColor(newColor)
    }

}