package com.stylingandroid.christmasface

import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup

fun <T> lazyFast(operation: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    operation()
}

fun Context.hasPermissions(vararg permissions: String, func: () -> Unit) {
    if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
        func()
    }
}

fun Context.lacksPermissions(vararg permissions: String, func: () -> Unit) {
    if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
        func()
    }
}

fun ViewGroup.forEach(function: (view: View) -> Unit) {
    for (i in 0 until childCount) {
        function(getChildAt(i))
    }
}
