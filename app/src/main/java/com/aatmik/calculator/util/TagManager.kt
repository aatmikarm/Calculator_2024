// util/TagManager.kt
package com.aatmik.calculator.util

import android.content.Context
import android.content.SharedPreferences
import com.aatmik.calculator.model.Tag
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TagManager {
    private const val PREF_NAME = "calculator_tags"
    private const val KEY_CUSTOM_TAGS = "custom_tags"

    private val gson = Gson()

    /**
     * Get SharedPreferences instance
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get all tags (default + custom)
     */
    fun getAllTags(context: Context): List<Tag> {
        val defaultTags = Tag.getDefaultTags()
        val customTags = getCustomTags(context)
        return defaultTags + customTags
    }

    /**
     * Get only custom tags
     */
    fun getCustomTags(context: Context): List<Tag> {
        val json = getPrefs(context).getString(KEY_CUSTOM_TAGS, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<Tag>>() {}.type
            gson.fromJson<List<Tag>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Add a new custom tag
     */
    fun addCustomTag(context: Context, tagName: String, color: String = "#FF6B6B"): Tag {
        val customTags = getCustomTags(context).toMutableList()

        // Generate unique ID
        val tagId = "custom_${System.currentTimeMillis()}"
        val newTag = Tag(
            id = tagId,
            name = tagName,
            color = color,
            isCustom = true
        )

        customTags.add(newTag)

        // Save to SharedPreferences
        val json = gson.toJson(customTags)
        getPrefs(context).edit().putString(KEY_CUSTOM_TAGS, json).apply()

        return newTag
    }

    /**
     * Delete a custom tag
     */
    fun deleteCustomTag(context: Context, tagId: String) {
        val customTags = getCustomTags(context).toMutableList()
        customTags.removeAll { it.id == tagId }

        // Save to SharedPreferences
        val json = gson.toJson(customTags)
        getPrefs(context).edit().putString(KEY_CUSTOM_TAGS, json).apply()
    }

    /**
     * Get tag by ID
     */
    fun getTagById(context: Context, tagId: String): Tag? {
        return getAllTags(context).find { it.id == tagId }
    }

    /**
     * Get random color for new tag
     */
    fun getRandomColor(): String {
        val colors = listOf(
            "#FF6B6B", "#4ECDC4", "#FFE66D", "#A8DADC",
            "#95E1D3", "#F38181", "#AA96DA", "#FCBAD3",
            "#FD79A8", "#74B9FF", "#A29BFE", "#FD79A8"
        )
        return colors.random()
    }
}