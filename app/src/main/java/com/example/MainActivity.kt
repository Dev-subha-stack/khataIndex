package com.example

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.DragHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TodoItem
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TodoAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodoAppScreen(viewModel: TodoViewModel = viewModel()) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    var permissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    "android.permission.POST_NOTIFICATIONS"
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var showNotificationSettingsPrompt by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionGranted = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission enabled!", Toast.LENGTH_SHORT).show()
        } else {
            showNotificationSettingsPrompt = true
        }
    }

    LaunchedEffect(Unit) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!permissionGranted) {
                requestPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
            } else if (!areNotificationsEnabled) {
                showNotificationSettingsPrompt = true
            }
        } else {
            if (!areNotificationsEnabled) {
                showNotificationSettingsPrompt = true
            }
        }
    }

    // State Collection from ViewModel
    val todos by viewModel.filteredTodos.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedPriorityFilter by viewModel.selectedPriorityFilter.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val selectedSortOption by viewModel.selectedSortOption.collectAsState()

    // Task Reminders states
    val overdueTasks by viewModel.overdueTasks.collectAsState()
    val upcomingTasks by viewModel.upcomingTasks.collectAsState()
    val areRemindersEnabled by viewModel.areRemindersEnabled.collectAsState()

    // Local UI controllers
    var currentTab by remember { mutableStateOf(0) }
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyScreen by remember { mutableStateOf(false) }

    // Categories list
    val categories = listOf("All", "Personal", "Work", "Shopping", "Finance", "Wellness")

    // Stats calculations
    val totalCount = todos.size
    val completedCount = todos.count { it.isCompleted }
    val pendingCount = totalCount - completedCount
    val completionPercent = if (totalCount > 0) (completedCount * 100) / totalCount else 0

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFFEF7FF), // Material 3 clean dynamic light surface
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFFF7F2FA),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        label = { Text("Task Planner", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Tasks",
                                tint = if (currentTab == 0) Color(selectedTheme.primaryColor) else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(selectedTheme.primaryColor),
                            selectedTextColor = Color(selectedTheme.primaryColor),
                            indicatorColor = Color(selectedTheme.primaryColor).copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        label = { Text("Khata Ledger", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Khata Ledger",
                                tint = if (currentTab == 1) Color(selectedTheme.primaryColor) else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(selectedTheme.primaryColor),
                            selectedTextColor = Color(selectedTheme.primaryColor),
                            indicatorColor = Color(selectedTheme.primaryColor).copy(alpha = 0.15f)
                        )
                    )
                }
            },
            floatingActionButton = {
                if (currentTab == 0) {
                    FloatingActionButton(
                        onClick = { showAddTodoDialog = true },
                        containerColor = Color(selectedTheme.primaryColor), // Styled by custom dynamic theme
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(bottom = 20.dp, end = 8.dp)
                            .testTag("add_todo_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Task",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // 1. App Header Title & Settings Trigger Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Secure Planner",
                            color = Color(0xFF1D1B20), // Primary black text
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "100% Offline Task Vault",
                            color = Color(selectedTheme.primaryColor), // Themed subtitle
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(selectedTheme.primaryColor).copy(alpha = 0.15f))
                            .testTag("app_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "App Settings",
                            tint = Color(selectedTheme.primaryColor),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (currentTab == 0) {
                    // 2. Active Reminders warnings (Overdue / Upcoming Within 24 Hours)
                if (areRemindersEnabled) {
                    if (overdueTasks.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF8B4B4))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Overdue alert",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "⚠️ Task Overdue Reminder",
                                        color = Color(0xFF9B1C1C),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "You have ${overdueTasks.size} overdue task(s)! Please action immediately.",
                                        color = Color(0xFF9B1C1C).copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    if (upcomingTasks.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "Upcoming alert",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "⏰ Upcoming Task Reminder",
                                        color = Color(0xFF92400E),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "You have ${upcomingTasks.size} task(s) due within 24 hours.",
                                        color = Color(0xFF92400E).copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("search_todo_input"),
                    placeholder = { Text("Search tasks...", color = Color(0xFF49454F)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF49454F)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color(0xFF49454F))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(selectedTheme.primaryColor),
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedContainerColor = Color(0xFFF3EDF7),
                        unfocusedContainerColor = Color(0xFFF3EDF7),
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Filters & Tasks List Block
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Quick Stats Dashboard Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(selectedTheme.primaryColor).copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedTheme.borderColor).copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📊 Vault Statistics",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1D1B20)
                                    )
                                    Text(
                                        text = "$completedCount/$totalCount Completed",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(selectedTheme.primaryColor)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                // Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(if (totalCount > 0) completedCount.toFloat() / totalCount else 0f)
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(selectedTheme.primaryColor))
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Pending", fontSize = 10.sp, color = Color.Gray)
                                        Text("$pendingCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Success Rate", fontSize = 10.sp, color = Color.Gray)
                                        Text("$completionPercent%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(selectedTheme.primaryColor))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Total Tasks", fontSize = 10.sp, color = Color.Gray)
                                        Text("$totalCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                                    }
                                }
                            }
                        }
                    }

                    // Category list header
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Filter Category",
                                    color = Color(0xFF1D1B20),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedCategoryFilter != "All" || selectedPriorityFilter != null) {
                                    Text(
                                        text = "Reset Filters",
                                        color = Color(selectedTheme.primaryColor),
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            viewModel.setCategoryFilter("All")
                                            viewModel.setPriorityFilter(null)
                                        }
                                    )
                                }
                            }

                            // Horizontal Flow Chips
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { category ->
                                    val isSelected = selectedCategoryFilter == category
                                    val chipBg = if (isSelected) Color(selectedTheme.primaryColor).copy(alpha = 0.15f) else Color.Transparent
                                    val chipText = if (isSelected) Color(selectedTheme.primaryColor) else Color(0xFF49454F)
                                    val chipBorderColor = if (isSelected) Color(selectedTheme.primaryColor) else Color(0xFF79747E)

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(chipBg)
                                            .border(1.dp, chipBorderColor, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setCategoryFilter(category) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("category_filter_$category")
                                    ) {
                                        Text(
                                            text = category,
                                            color = chipText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Priority selector chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Priority: ",
                                    color = Color(0xFF49454F),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                listOf(
                                    Pair(null, "All"),
                                    Pair(0, "Low"),
                                    Pair(1, "Med"),
                                    Pair(2, "High")
                                ).forEach { (priorityVal, label) ->
                                    val isSelected = selectedPriorityFilter == priorityVal
                                    val chipBg = if (isSelected) Color(selectedTheme.primaryColor).copy(alpha = 0.15f) else Color.Transparent
                                    val chipText = if (isSelected) Color(selectedTheme.primaryColor) else Color(0xFF49454F)
                                    val chipBorderColor = if (isSelected) Color(selectedTheme.primaryColor) else Color(0xFF79747E)

                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(chipBg)
                                            .border(1.dp, chipBorderColor, RoundedCornerShape(6.dp))
                                            .clickable { viewModel.setPriorityFilter(priorityVal) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .testTag("priority_filter_${label.lowercase()}")
                                    ) {
                                        Text(
                                            text = label,
                                            color = chipText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Interactive Sorting Dropdown Control and Export Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = Color(selectedTheme.primaryColor),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sort: ",
                                        color = Color(0xFF49454F),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    var isSortMenuExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(selectedTheme.primaryColor).copy(alpha = 0.1f))
                                                .clickable { isSortMenuExpanded = true }
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${selectedSortOption.displayName}  ▼",
                                                color = Color(selectedTheme.primaryColor),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = isSortMenuExpanded,
                                            onDismissRequest = { isSortMenuExpanded = false },
                                            modifier = Modifier.background(Color(0xFFFEF7FF))
                                        ) {
                                            TaskSortOption.values().forEach { option ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = option.displayName,
                                                            color = Color(0xFF1D1B20),
                                                            fontSize = 13.sp,
                                                            fontWeight = if (selectedSortOption == option) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    onClick = {
                                                        viewModel.setSortOption(option)
                                                        isSortMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick share/export tasks button
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF3EDF7))
                                        .clickable {
                                            if (todos.isEmpty()) {
                                                Toast.makeText(context, "No tasks to share!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val formatted = todos.joinToString("\n") { item ->
                                                    val status = if (item.isCompleted) "[✓]" else "[ ]"
                                                    val priorityText = when (item.priority) {
                                                        2 -> "High"
                                                        1 -> "Medium"
                                                        else -> "Low"
                                                    }
                                                    "$status ${item.title} (${item.category} - Priority: $priorityText)"
                                                }
                                                clipboardManager.setText(AnnotatedString("📋 My Task List:\n$formatted"))
                                                Toast.makeText(context, "Copied list to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF49454F),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Export List",
                                        color = Color(0xFF49454F),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Main Tasks List items
                    if (todos.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color(selectedTheme.primaryColor).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = Color(selectedTheme.primaryColor),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No tasks found!",
                                    color = Color(0xFF1D1B20),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Create a task or reset active search filters.",
                                    color = Color(0xFF49454F),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(todos, key = { _, item -> item.id }) { index, item ->
                            TodoCardItem(
                                todo = item,
                                theme = selectedTheme,
                                isManualSortEnabled = selectedSortOption == TaskSortOption.MANUAL,
                                currentIndex = index,
                                listSize = todos.size,
                                onDragAndSwap = { fromIdx, toIdx ->
                                    if (fromIdx in todos.indices && toIdx in todos.indices) {
                                        viewModel.swapTodoOrders(todos[fromIdx], todos[toIdx])
                                    }
                                },
                                onToggleComplete = { viewModel.toggleTodoCompleted(item) },
                                onDelete = { viewModel.deleteTodo(item) }
                            )
                        }
                    }

                    // Bottom Privacy / Version Footer
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp, bottom = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Secure Planner Pro v1.2.0 (Stable)",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
                } else {
                    KhataBookDashboard(viewModel = viewModel, theme = selectedTheme)
                }
            }
        }

        // Modal Add Todo Dialog
        if (showAddTodoDialog) {
            AddTodoDialog(
                theme = selectedTheme,
                onDismiss = { showAddTodoDialog = false },
                onAddTodo = { title, desc, prio, cat, due ->
                    viewModel.addTodo(title, desc, prio, cat, due)
                    showAddTodoDialog = false
                    Toast.makeText(context, "Task created securely!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Modal App Settings, Theme Switcher, About & Privacy Dialog
        if (showSettingsDialog) {
            AppSettingsDialog(
                viewModel = viewModel,
                onDismiss = { showSettingsDialog = false },
                onRequestPrivacy = {
                    showSettingsDialog = false
                    showPrivacyPolicyScreen = true
                },
                permissionGranted = permissionGranted,
                onRequestNotificationPermission = {
                    requestPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                }
            )
        }

        // Auto Prompt Startup Notification Permission Fallback Dialog
        if (showNotificationSettingsPrompt) {
            AlertDialog(
                onDismissRequest = { showNotificationSettingsPrompt = false },
                title = {
                    Text("🔔 Enable Notifications", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1D1B20))
                },
                text = {
                    Text(
                        "To receive real-time task reminders and ledger updates, please enable notifications for KhataIndex in your system settings.",
                        fontSize = 14.sp,
                        color = Color(0xFF49454F)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showNotificationSettingsPrompt = false
                            val intent = Intent().apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                } else {
                                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                                    putExtra("app_package", context.packageName)
                                    putExtra("app_uid", context.applicationInfo.uid)
                                }
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open settings. Please enable them manually.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Open Settings", fontWeight = FontWeight.Bold, color = Color(selectedTheme.primaryColor))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showNotificationSettingsPrompt = false }
                    ) {
                        Text("Later", color = Color.Gray)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White
            )
        }

        // Dedicated Full-screen Privacy Policy Overlay
        AnimatedVisibility(
            visible = showPrivacyPolicyScreen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            PrivacyPolicyScreen(
                theme = selectedTheme,
                onDismiss = { showPrivacyPolicyScreen = false }
            )
        }
    }
}

@Composable
fun TodoCardItem(
    todo: TodoItem,
    theme: TaskCardTheme,
    isManualSortEnabled: Boolean = false,
    currentIndex: Int = 0,
    listSize: Int = 0,
    onDragAndSwap: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    // Priority color mapping
    val priorityColor = when (todo.priority) {
        2 -> Color(0xFFEF4444) // High - Red
        1 -> Color(0xFFF59E0B) // Med - Amber
        else -> Color(0xFF10B981) // Low - Emerald
    }

    val priorityLabel = when (todo.priority) {
        2 -> "High"
        1 -> "Medium"
        else -> "Low"
    }

    val themePrimary = Color(theme.primaryColor)
    val themeContainer = Color(theme.containerColor)
    val themeText = Color(theme.textColor)
    val themeBorder = Color(theme.borderColor)

    val titleColor by animateColorAsState(
        targetValue = if (todo.isCompleted) themeText.copy(alpha = 0.5f) else themeText,
        label = "TitleColor"
    )

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thresholdPx = remember { with(density) { 64.dp.toPx() } }

    val currentOnDragAndSwap by rememberUpdatedState(onDragAndSwap)
    val currentIdx by rememberUpdatedState(currentIndex)
    val currentSize by rememberUpdatedState(listSize)

    val dragModifier = if (isManualSortEnabled) {
        Modifier.pointerInput(todo.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    isDragging = true
                    dragOffsetY = 0f
                },
                onDragEnd = {
                    isDragging = false
                    dragOffsetY = 0f
                },
                onDragCancel = {
                    isDragging = false
                    dragOffsetY = 0f
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffsetY += dragAmount.y
                    if (dragOffsetY > thresholdPx && currentIdx < currentSize - 1) {
                        currentOnDragAndSwap(currentIdx, currentIdx + 1)
                        dragOffsetY -= thresholdPx
                    } else if (dragOffsetY < -thresholdPx && currentIdx > 0) {
                        currentOnDragAndSwap(currentIdx, currentIdx - 1)
                        dragOffsetY += thresholdPx
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = if (isDragging) 1.05f else 1f
                scaleY = if (isDragging) 1.05f else 1f
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
            }
            .then(dragModifier)
            .testTag("todo_item_card"),
        colors = CardDefaults.cardColors(containerColor = themeContainer),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (todo.isCompleted) themeBorder.copy(alpha = 0.4f) else themeBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isManualSortEnabled) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = themeText.copy(alpha = 0.4f),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp)
                )
            }

            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = themePrimary,
                    uncheckedColor = themeText.copy(alpha = 0.6f),
                    checkmarkColor = Color.White
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("todo_checkbox")
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = todo.title,
                    color = titleColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (todo.description.isNotEmpty()) {
                    Text(
                        text = todo.description,
                        color = if (todo.isCompleted) themeText.copy(alpha = 0.5f) else themeText.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Tag with themed accent
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(themePrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = todo.category,
                            color = themePrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Priority indicator dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = priorityLabel,
                        color = themeText.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )

                    // Due Date
                    if (todo.dueDate != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = themeText.copy(alpha = 0.6f),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val format = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                        Text(
                            text = format.format(Date(todo.dueDate)),
                            color = themeText.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Delete Action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_todo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task",
                    tint = themeText.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    theme: TaskCardTheme,
    onDismiss: () -> Unit,
    onAddTodo: (title: String, desc: String, priority: Int, category: String, dueDate: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(1) } // Default Medium
    var category by remember { mutableStateOf("Personal") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Personal", "Work", "Shopping", "Finance", "Wellness")
    val context = LocalContext.current
    val themePrimary = Color(theme.primaryColor)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFEF7FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "New Secure Task",
                    color = Color(0xFF1D1B20),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title", color = Color(0xFF49454F)) },
                    placeholder = { Text("Enter title", color = Color(0xFF49454F).copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themePrimary,
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_todo_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)", color = Color(0xFF49454F)) },
                    placeholder = { Text("Enter details", color = Color(0xFF49454F).copy(alpha = 0.6f)) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themePrimary,
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category selection dropdown
                Text(text = "Category", color = Color(0xFF49454F), fontSize = 12.sp)
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = category,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themePrimary,
                            unfocusedBorderColor = Color(0xFF79747E),
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFFFEF7FF))
                    ) {
                        categories.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection, color = Color(0xFF1D1B20)) },
                                onClick = {
                                    category = selection
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Priority selection chips
                Text(text = "Priority Level", color = Color(0xFF49454F), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Triple(0, "Low", Color(0xFF10B981)),
                        Triple(1, "Medium", Color(0xFFF59E0B)),
                        Triple(2, "High", Color(0xFFEF4444))
                    ).forEach { (level, name, color) ->
                        val isSelected = priority == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, Color(0xFF79747E), RoundedCornerShape(8.dp))
                                .clickable { priority = level }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) color else Color(0xFF49454F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date Picker row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Due Date", color = Color(0xFF49454F), fontSize = 12.sp)
                        val dateText = if (dueDate == null) {
                            "No due date"
                        } else {
                            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(dueDate!!))
                        }
                        Text(text = dateText, color = Color(0xFF1D1B20), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedCal = Calendar.getInstance()
                                    selectedCal.set(year, month, dayOfMonth)
                                    dueDate = selectedCal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themePrimary.copy(alpha = 0.15f),
                            contentColor = themePrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Choose", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF49454F))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                            } else {
                                onAddTodo(title, description, priority, category, dueDate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("submit_todo_button")
                    ) {
                        Text("Save Task", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AppSettingsDialog(
    viewModel: TodoViewModel,
    onDismiss: () -> Unit,
    onRequestPrivacy: () -> Unit,
    permissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val areRemindersEnabled by viewModel.areRemindersEnabled.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFEF7FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Settings",
                        color = Color(0xFF1D1B20),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Dialog", tint = Color(0xFF49454F))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Material 3 Dynamic Color Palette Switcher
                Text(
                    text = "🎨 Color Theme",
                    color = Color(selectedTheme.primaryColor),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Personalize your planners with customized palette styles.",
                    color = Color(0xFF49454F),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive row of colored dots
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskCardTheme.values().forEach { theme ->
                        val isSelected = selectedTheme == theme
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(theme.containerColor))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color(theme.primaryColor) else Color(0xFF79747E).copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.selectTheme(theme) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(theme.primaryColor))
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(10.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active: ${selectedTheme.displayName}",
                    color = Color(0xFF49454F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFCAC4D0).copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.height(16.dp))

                // 2. Task Reminders section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(selectedTheme.primaryColor),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Reminders",
                                color = Color(0xFF1D1B20),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Overdue task alerts",
                                color = Color(0xFF49454F),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = areRemindersEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.toggleRemindersEnabled()
                            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted) {
                                onRequestNotificationPermission()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(selectedTheme.primaryColor),
                            checkedTrackColor = Color(selectedTheme.primaryColor).copy(alpha = 0.25f)
                        )
                    )
                }

                if (areRemindersEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.triggerTestReminder() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(selectedTheme.primaryColor).copy(alpha = 0.1f),
                            contentColor = Color(selectedTheme.primaryColor)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Notification", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFCAC4D0).copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.height(16.dp))

                // 3. Info & Privacy Buttons (Minimalist style)
                Text(
                    text = "🔒 Offline & Private",
                    color = Color(0xFF1D1B20),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All planner records remain stored locally on your physical device.",
                    color = Color(0xFF49454F),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onRequestPrivacy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(selectedTheme.primaryColor).copy(alpha = 0.12f),
                        contentColor = Color(selectedTheme.primaryColor)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Read Privacy Policy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFCAC4D0).copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.height(16.dp))

                // App Info Footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Secure Planner Pro v1.2.0", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1D1B20))
                    Text("Designed by Subhajit Roy", fontSize = 10.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(selectedTheme.primaryColor)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(
    theme: TaskCardTheme,
    onDismiss: () -> Unit
) {
    val themePrimary = Color(theme.primaryColor)
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFEF7FF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            // Header with Close/Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(themePrimary.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Go Back",
                        tint = themePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Privacy Policy Vault",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D1B20)
                    )
                    Text(
                        text = "Last updated: July 2026",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Detailed Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Welcome to Secure Planner Pro",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themePrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "We take your privacy with absolute seriousness. Secure Planner Pro is designed as a completely offline-first personal planner app. There is NO backend server, NO data analytics tracking, and NO third-party ad networks (completely Ad-Free). Here is the comprehensive disclosure of how your information is handled.",
                    fontSize = 13.sp,
                    color = Color(0xFF49454F),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                PrivacySectionHeader(title = "1. Personal Information Collection")
                PrivacySectionBody(
                    text = "We collect absolutely zero personally identifiable information (PII). You are not required to create an account, register an email address, verify your phone number, or connect any social profile to use the application."
                )

                PrivacySectionHeader(title = "2. Task Data and Local Storage")
                PrivacySectionBody(
                    text = "All data created inside the app (including task titles, detailed descriptions, due dates, categories, priorities, and completed states) is saved strictly on your local physical device. \n\nWe utilize Android's built-in SQLite database engine managed by the Room Persistence Library. No task data is transmitted over the internet, and no cloud-synchronization components are active."
                )

                PrivacySectionHeader(title = "3. Zero Third-Party Tracker SDKs")
                PrivacySectionBody(
                    text = "Unlike traditional apps, Secure Planner Pro has completely REMOVED Google AdSense, AdMob, Firebase Analytics, and any telemetry scripts. There are no tracking scripts or audience measurement networks running in the background. Your behavior, task habits, and schedule remain 100% private to you."
                )

                PrivacySectionHeader(title = "4. Device Permissions Explained")
                PrivacySectionBody(
                    text = "• Local System Notifications: Used strictly to schedule alerts for overdue tasks or upcoming deadlines. These notifications are processed completely locally by the Android operating system and do not use any cloud messaging services. \n\n• Boot Completed: Used to re-register scheduled task alerts upon system restart."
                )

                PrivacySectionHeader(title = "5. Data Erasure and Lifecycle")
                PrivacySectionBody(
                    text = "Since all data is saved locally on your device, you have complete control over its lifecycle. You can wipe your data at any time by: \n\n1. Clearing individual tasks inside the app list.\n2. Selecting 'Clear Completed Tasks' in the interface.\n3. Going to Android Settings -> Apps -> Secure Planner -> Storage -> Clear Data. \n\nUninstalling the application will automatically purge the entire database permanently."
                )

                PrivacySectionHeader(title = "6. Security Architecture")
                PrivacySectionBody(
                    text = "Secure Planner Pro operates within the standard Android application secure container sandbox. This isolating mechanism ensures that no other third-party applications installed on your device can inspect, read, or tamper with your tasks or settings database."
                )

                PrivacySectionHeader(title = "7. Contact and Support")
                PrivacySectionBody(
                    text = "If you have any questions or require support regarding your offline planner application, you may contact our lead developer:\n\nDeveloper: Subhajit Roy\nEmail: subhajit.roy@example.com\nSupport: subhajit.roy@myntra.com"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = themePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I Understand & Accept", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PrivacySectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1D1B20),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun PrivacySectionBody(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = Color(0xFF49454F),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
