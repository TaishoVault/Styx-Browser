package com.jamal2367.styx.extensions

import android.graphics.*
import androidx.core.graphics.createBitmap

/**
 * Creates and returns a new favicon which is the same as the provided favicon but with horizontal
 * and vertical padding of 4dp
 *
 * @return the padded bitmap.
 */
fun Bitmap.pad(): Bitmap {
    return this
}

private val desaturatedPaint = Paint().apply {
    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
        setSaturation(0.5f)
    })
}

/**
 * Desaturates a [Bitmap] to 50% grayscale. Note that a new bitmap will be created.
 */
fun Bitmap.desaturate(): Bitmap = createBitmap(width, height).also {
    Canvas(it).drawBitmap(this, 0f, 0f, desaturatedPaint)
}
