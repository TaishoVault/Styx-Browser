/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package com.jamal2367.styx.utils

import android.view.animation.Interpolator
import androidx.core.view.animation.PathInterpolatorCompat


internal class BezierEaseInterpolator : Interpolator {
    override fun getInterpolation(input: Float): Float {
        return sBezierInterpolator.getInterpolation(input)
    }

    companion object {
        private val sBezierInterpolator: Interpolator = PathInterpolatorCompat.create(0.25f, 0.1f, 0.25f, 1f)
    }
}