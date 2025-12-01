package com.aatmik.calculator.model

data class Tag(
    val id: String,
    val name: String,
    val color: String = "#FF6B6B",
    val isCustom: Boolean = false
) {
    companion object {
        // Predefined tags
        fun getDefaultTags(): List<Tag> {
            return listOf(
                Tag(id = "work", name = "Work", color = "#4ECDC4", isCustom = false),
                Tag(id = "personal", name = "Personal", color = "#FFE66D", isCustom = false),
                Tag(id = "shopping", name = "Shopping", color = "#FF6B6B", isCustom = false),
                Tag(id = "bills", name = "Bills", color = "#A8DADC", isCustom = false),
                Tag(id = "investment", name = "Investment", color = "#95E1D3", isCustom = false),
                Tag(id = "travel", name = "Travel", color = "#F38181", isCustom = false)
            )
        }
    }
}