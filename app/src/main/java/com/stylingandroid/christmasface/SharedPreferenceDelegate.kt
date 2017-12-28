package com.stylingandroid.christmasface

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private class SharedPreferenceDelegate<T>(
        private val context: Context,
        private val defaultValue: T,
        private val getter: SharedPreferences.(String, T) -> T,
        private val setter: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
        private val key: String
) : ReadWriteProperty<Any, T> {

    private val safeContext: Context by lazyFast { context.applicationContext }

    private val sharedPreferences: SharedPreferences by lazyFast {
        PreferenceManager.getDefaultSharedPreferences(safeContext)
    }

    override fun getValue(thisRef: Any, property: KProperty<*>) =
            sharedPreferences
                    .getter(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) =
            sharedPreferences
                    .edit()
                    .setter(key, value)
                    .apply()
}

@Suppress("UNCHECKED_CAST")
fun <T> bindSharedPreference(context: Context, key: String, defaultValue: T): ReadWriteProperty<Any, T> =
        when (defaultValue) {
            is Boolean ->
                SharedPreferenceDelegate(
                        context, defaultValue, SharedPreferences::getBoolean, SharedPreferences.Editor::putBoolean, key)
            is Int ->
                SharedPreferenceDelegate(
                        context, defaultValue, SharedPreferences::getInt, SharedPreferences.Editor::putInt, key)
            is Long ->
                SharedPreferenceDelegate(
                        context, defaultValue, SharedPreferences::getLong, SharedPreferences.Editor::putLong, key)
            is Float ->
                SharedPreferenceDelegate(
                        context, defaultValue, SharedPreferences::getFloat, SharedPreferences.Editor::putFloat, key)
            is String ->
                SharedPreferenceDelegate(
                        context, defaultValue, SharedPreferences::getString, SharedPreferences.Editor::putString, key)
            else -> throw IllegalArgumentException()
        } as ReadWriteProperty<Any, T>
