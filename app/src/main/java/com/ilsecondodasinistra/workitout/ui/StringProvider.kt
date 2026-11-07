package com.ilsecondodasinistra.workitout.ui

import android.content.Context
import androidx.annotation.StringRes

/** Lets ViewModels build localized text without holding a Context. */
fun interface StringProvider {
    fun get(@StringRes resId: Int, vararg args: Any): String
}

fun Context.stringProvider() = StringProvider { resId, args -> getString(resId, *args) }
