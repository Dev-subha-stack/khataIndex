package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TodoDatabase
import com.example.data.TodoItem
import com.example.data.TodoRepository
import com.example.data.KhataContact
import com.example.data.KhataTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Material 3 Dynamic Task Card Themes - Expanded to 10 beautiful color palettes!
enum class TaskCardTheme(
    val displayName: String, 
    val primaryColor: Long, 
    val containerColor: Long, 
    val textColor: Long, 
    val borderColor: Long
) {
    VIOLET("Lavender Dreams", 0xFF6750A4, 0xFFF3EDF7, 0xFF1D1B20, 0xFFCAC4D0),
    EMERALD("Fresh Emerald", 0xFF00875A, 0xFFEBF7F2, 0xFF003823, 0xFFB3E3D1),
    OCEAN("Ocean Breeze", 0xFF006494, 0xFFE1F5FE, 0xFF001E30, 0xFFB2DFDB),
    SUNSET("Warm Sunset", 0xFFC25100, 0xFFFFF3E0, 0xFF4A1C00, 0xFFFFCC80),
    CHARCOAL("Classic Charcoal", 0xFF2D3142, 0xFFF0F1F3, 0xFF1B1D22, 0xFFCFD8DC),
    ROSE("Pink Blossom", 0xFFB82C5E, 0xFFFDF2F5, 0xFF3D0618, 0xFFF5D6E0),
    ROYAL("Royal Indigo", 0xFF3F51B5, 0xFFE8EAF6, 0xFF1A237E, 0xFFC5CAE9),
    TERRACOTTA("Terracotta Clay", 0xFFD84315, 0xFFFBE9E7, 0xFF4E1505, 0xFFFFCCBC),
    MINT("Spearmint Teal", 0xFF00796B, 0xFFE0F2F1, 0xFF002420, 0xFFB2DFDB),
    GOLDEN("Golden Honey", 0xFFFF8F00, 0xFFFFF8E1, 0xFF3E2723, 0xFFFFE082)
}

// Interactive sorting configurations
enum class TaskSortOption(val displayName: String) {
    MANUAL("Manual (Drag & Drop)"),
    CREATED_DATE("Date Created (Newest)"),
    DUE_DATE("Due Date (Soonest)"),
    PRIORITY_HIGH_TO_LOW("Priority (High to Low)"),
    PRIORITY_LOW_TO_HIGH("Priority (Low to High)"),
    TITLE_A_Z("Alphabetical (A-Z)"),
    STATUS("Completion Status")
}

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TodoRepository

    // Search and filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _selectedPriorityFilter = MutableStateFlow<Int?>(null) // null = All, 0 = Low, 1 = Medium, 2 = High
    val selectedPriorityFilter = _selectedPriorityFilter.asStateFlow()

    // Sorting State
    private val _selectedSortOption = MutableStateFlow(TaskSortOption.MANUAL)
    val selectedSortOption = _selectedSortOption.asStateFlow()

    // Task Card Theme State
    private val _selectedTheme = MutableStateFlow(TaskCardTheme.VIOLET)
    val selectedTheme = _selectedTheme.asStateFlow()

    // Reminders states and configurations
    private val _areRemindersEnabled = MutableStateFlow(true)
    val areRemindersEnabled = _areRemindersEnabled.asStateFlow()

    init {
        val database = TodoDatabase.getDatabase(application)
        val todoDao = database.todoDao()
        val khataDao = database.khataDao()
        repository = TodoRepository(todoDao, khataDao)
    }

    // Theme Switcher Operation
    fun selectTheme(theme: TaskCardTheme) {
        _selectedTheme.value = theme
    }

    // Sort control operation
    fun setSortOption(sortOption: TaskSortOption) {
        _selectedSortOption.value = sortOption
    }

    // Exposed filtered and sorted todo items Flow
    val filteredTodos: StateFlow<List<TodoItem>> = combine(
        repository.allTodos,
        _searchQuery,
        _selectedCategoryFilter,
        _selectedPriorityFilter,
        _selectedSortOption
    ) { todos, query, category, priority, sortOption ->
        todos.filter { item ->
            val matchesQuery = item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || item.category == category
            val matchesPriority = priority == null || item.priority == priority
            matchesQuery && matchesCategory && matchesPriority
        }.sortedWith { a, b ->
            when (sortOption) {
                TaskSortOption.MANUAL -> a.itemOrder.compareTo(b.itemOrder)
                TaskSortOption.CREATED_DATE -> b.createdAt.compareTo(a.createdAt) // Newest first
                TaskSortOption.DUE_DATE -> {
                    val dueA = a.dueDate ?: Long.MAX_VALUE
                    val dueB = b.dueDate ?: Long.MAX_VALUE
                    dueA.compareTo(dueB)
                }
                TaskSortOption.PRIORITY_HIGH_TO_LOW -> b.priority.compareTo(a.priority)
                TaskSortOption.PRIORITY_LOW_TO_HIGH -> a.priority.compareTo(b.priority)
                TaskSortOption.TITLE_A_Z -> a.title.lowercase().compareTo(b.title.lowercase())
                TaskSortOption.STATUS -> a.isCompleted.compareTo(b.isCompleted)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filter controls
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Categories filter
    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    // Priority filter
    fun setPriorityFilter(priority: Int?) {
        _selectedPriorityFilter.value = priority
    }

    // Todo Database operations
    fun addTodo(title: String, description: String, priority: Int, category: String, dueDate: Long?) {
        viewModelScope.launch {
            val currentTodos = repository.allTodos.first()
            val maxOrder = currentTodos.maxOfOrNull { it.itemOrder } ?: 0
            val newItem = TodoItem(
                title = title,
                description = description,
                priority = priority,
                category = category,
                dueDate = dueDate,
                itemOrder = maxOrder + 1
            )
            repository.insert(newItem)

            // Trigger notification for upcoming or overdue tasks if reminders are enabled
            if (dueDate != null && _areRemindersEnabled.value) {
                val now = System.currentTimeMillis()
                val oneDayMillis = 24 * 60 * 60 * 1000L
                if (dueDate in now..(now + oneDayMillis)) {
                    showNotification(
                        getApplication(),
                        "⏰ Upcoming Task: $title",
                        "Due in less than 24 hours! Category: $category"
                    )
                } else if (dueDate < now) {
                    showNotification(
                        getApplication(),
                        "⚠️ Overdue Task Created: $title",
                        "The task's due date is already in the past!"
                    )
                }
            }
        }
    }

    fun toggleTodoCompleted(todo: TodoItem) {
        viewModelScope.launch {
            val updated = todo.copy(isCompleted = !todo.isCompleted)
            repository.update(updated)
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.delete(todo)
        }
    }

    fun clearCompletedTodos() {
        viewModelScope.launch {
            repository.clearCompleted()
        }
    }

    // Toggle reminders
    fun toggleRemindersEnabled() {
        _areRemindersEnabled.value = !_areRemindersEnabled.value
    }

    // List of overdue tasks
    val overdueTasks: StateFlow<List<TodoItem>> = combine(
        repository.allTodos,
        _areRemindersEnabled
    ) { todos, enabled ->
        if (!enabled) emptyList()
        else todos.filter { !it.isCompleted && it.dueDate != null && it.dueDate!! < System.currentTimeMillis() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // List of upcoming tasks
    val upcomingTasks: StateFlow<List<TodoItem>> = combine(
        repository.allTodos,
        _areRemindersEnabled
    ) { todos, enabled ->
        if (!enabled) emptyList()
        else {
            val now = System.currentTimeMillis()
            val oneDayMillis = 24 * 60 * 60 * 1000L
            todos.filter { !it.isCompleted && it.dueDate != null && it.dueDate!! in now..(now + oneDayMillis) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Swap orders of two tasks in manual drag & drop reorder mode
    fun swapTodoOrders(fromItem: TodoItem, toItem: TodoItem) {
        viewModelScope.launch {
            val fromOrder = fromItem.itemOrder
            val toOrder = toItem.itemOrder
            
            if (fromOrder == toOrder) {
                val list = repository.allTodos.first()
                val sortedList = list.sortedWith(compareBy<TodoItem> { it.itemOrder }.thenBy { it.createdAt })
                val fromIndex = sortedList.indexOfFirst { it.id == fromItem.id }
                val toIndex = sortedList.indexOfFirst { it.id == toItem.id }
                
                if (fromIndex != -1 && toIndex != -1) {
                    for (i in sortedList.indices) {
                        val item = sortedList[i]
                        var targetOrder = i
                        if (i == fromIndex) {
                            targetOrder = toIndex
                        } else if (i == toIndex) {
                            targetOrder = fromIndex
                        }
                        if (item.itemOrder != targetOrder || item.id == fromItem.id || item.id == toItem.id) {
                            repository.update(item.copy(itemOrder = targetOrder))
                        }
                    }
                }
            } else {
                val updatedFrom = fromItem.copy(itemOrder = toOrder)
                val updatedTo = toItem.copy(itemOrder = fromOrder)
                repository.update(updatedFrom)
                repository.update(updatedTo)
            }
        }
    }

    // KhataBook (Ledger Book) Integration States & Functions
    val khataSellers: StateFlow<List<KhataContact>> = repository.getContactsByType("SELLER")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val khataCustomers: StateFlow<List<KhataContact>> = repository.getContactsByType("CUSTOMER")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allKhataTransactions: StateFlow<List<KhataTransaction>> = repository.allKhataTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getTransactionsForContact(contactId: Int): kotlinx.coroutines.flow.Flow<List<KhataTransaction>> {
        return repository.getTransactionsForContact(contactId)
    }

    fun addKhataContact(name: String, phone: String, type: String) {
        viewModelScope.launch {
            repository.insertContact(KhataContact(name = name, phone = phone, type = type))
        }
    }

    fun deleteKhataContact(contactId: Int) {
        viewModelScope.launch {
            repository.deleteContactById(contactId)
        }
    }

    fun addKhataTransaction(contactId: Int, description: String, amount: Double, type: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                KhataTransaction(
                    contactId = contactId,
                    description = description,
                    amount = amount,
                    type = type
                )
            )
        }
    }

    fun deleteKhataTransaction(transaction: KhataTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun clearAllKhataData() {
        viewModelScope.launch {
            repository.clearAllKhataData()
        }
    }

    // Trigger test reminder
    fun triggerTestReminder() {
        if (!_areRemindersEnabled.value) return
        val context = getApplication<Application>()
        showNotification(
            context,
            "⏰ Task Reminder",
            "This is a preview reminder for your task! Complete your tasks to stay on track."
        )
    }
}

// Notification Helper function
fun showNotification(context: android.content.Context, title: String, content: String) {
    try {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "todo_reminders"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Todo Reminders",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for todo upcoming and overdue task reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
