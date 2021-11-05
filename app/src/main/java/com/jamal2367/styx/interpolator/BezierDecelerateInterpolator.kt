/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package com.jamal2367.styx.interpolator

import android.view.animation.Interpolator
import androidx.core.view.animation.PathInterpolatorCompat

/**
 * Smooth bezier curve interpolator.
 */
class BezierDecelerateInterpolator : Interpolator {

    private val interpolator = PathInterpolatorCompat.create(0.25f, 0.1f, 0.25f, 1f)

    override fun getInterpolation(input: Float): Float = interpolator.getInterpolation(input)

}
