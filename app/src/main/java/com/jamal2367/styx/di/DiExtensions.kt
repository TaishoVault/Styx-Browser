@file:JvmName("Injector")

package com.jamal2367.styx.di

import android.content.Context
import androidx.fragment.app.Fragment
import com.jamal2367.styx.BrowserApp

/**
 * The [AppComponent] attached to the application [Context].
 */
val Context.injector: AppComponent
    get() = (applicationContext as BrowserApp).applicationComponent

/**
 * The [AppComponent] attached to the context, note that the fragment must be attached.
 */
val Fragment.injector: AppComponent
    get() = (context?.applicationContext as BrowserApp).applicationComponent


