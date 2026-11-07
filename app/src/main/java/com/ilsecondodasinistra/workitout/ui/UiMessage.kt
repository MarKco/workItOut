package com.ilsecondodasinistra.workitout.ui

import android.content.Context
import androidx.annotation.StringRes

/** A user-facing message the ViewModel can emit without needing a Context. */
data class UiMessage(@StringRes val resId: Int, val args: List<Any> = emptyList()) {
    fun resolve(context: Context): String = context.getString(resId, *args.toTypedArray())
}
