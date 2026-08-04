package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val priority: Int = 1, // 0 = Low, 1 = Medium, 2 = High
    val category: String = "Personal", // "Personal", "Work", "Shopping", "Finance"
    val createdAt: Long = System.currentTimeMillis(),
    val itemOrder: Int = (System.currentTimeMillis() / 1000).toInt()
)
