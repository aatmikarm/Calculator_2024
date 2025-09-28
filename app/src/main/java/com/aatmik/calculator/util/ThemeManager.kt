package com.aatmik.calculator.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.aatmik.calculator.R

object ThemeManager {
    private const val THEME_PREF = "theme_preferences"
    private const val THEME_KEY = "selected_theme"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"
    const val THEME_DEFAULT = "default"
    const val THEME_RED = "red"
    const val THEME_GREEN = "green"
    const val THEME_BLUE = "blue"
    const val THEME_PURPLE = "purple"
    const val THEME_PINK = "pink"

    fun saveTheme(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE)
        prefs.edit().putString(THEME_KEY, theme).apply()
        applyTheme(theme)
    }

    fun getSavedTheme(context: Context): String {
        val prefs = context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE)
        return prefs.getString(THEME_KEY, THEME_DEFAULT) ?: THEME_DEFAULT
    }

    fun applyTheme(theme: String) {
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Color themes use light mode
        }
    }

    fun getThemeStyle(context: Context): Int {
        val theme = getSavedTheme(context)
        return when (theme) {
            THEME_LIGHT -> R.style.Theme_Calculator_Light
            THEME_DARK -> R.style.Theme_Calculator_Dark
            THEME_RED -> R.style.Theme_Calculator_Red
            THEME_GREEN -> R.style.Theme_Calculator_Green
            THEME_BLUE -> R.style.Theme_Calculator_Blue
            THEME_PURPLE -> R.style.Theme_Calculator_Purple
            THEME_PINK -> R.style.Theme_Calculator_Pink
            else -> R.style.Theme_Calculator // Your default theme from manifest
        }
    }

    fun initializeTheme(context: Context) {
        val savedTheme = getSavedTheme(context)
        applyTheme(savedTheme)
    }

    fun getThemeDisplayName(theme: String): String {
        return when (theme) {
            THEME_LIGHT -> "Light Mode"
            THEME_DARK -> "Dark Mode"
            THEME_SYSTEM -> "Follow System"
            THEME_DEFAULT -> "Default (Orange)"
            THEME_RED -> "Red Theme"
            THEME_GREEN -> "Green Theme"
            THEME_BLUE -> "Blue Theme"
            THEME_PURPLE -> "Purple Theme"
            THEME_PINK -> "Pink Theme"
            else -> "Default"
        }
    }
}