package com.example

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.*
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Calculate
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
            val viewModel: TodoViewModel = viewModel()
            val isDark by viewModel.isDarkTheme.collectAsState()
            MyApplicationTheme(darkTheme = isDark) {
                CompositionLocalProvider(LocalIsDark provides isDark) {
                    TodoAppScreen(viewModel = viewModel)
                }
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
    var todoToEdit by remember { mutableStateOf<TodoItem?>(null) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var openKhataSettingsFromMain by remember { mutableStateOf(false) }
    var showPrivacyPolicyScreen by remember { mutableStateOf(false) }

    // Categories list
    val categories = listOf("All", "Personal", "Work", "Shopping", "Finance")

    // Stats calculations
    val totalCount = todos.size
    val completedCount = todos.count { it.isCompleted }
    val pendingCount = totalCount - completedCount
    val completionPercent = if (totalCount > 0) (completedCount * 100) / totalCount else 0

    val isDark = LocalIsDark.current
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isDark) Color(0xFF141218) else Color(0xFFFEF7FF), // Material 3 clean dynamic light surface
            bottomBar = {
                NavigationBar(
                    containerColor = if (isDark) Color(0xFF1D1B22) else Color(0xFFF7F2FA),
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
                                tint = if (currentTab == 0) selectedTheme.primary() else (if (isDark) Color(0xFFCAC4D0) else Color.Gray)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = selectedTheme.primary(),
                            selectedTextColor = selectedTheme.primary(),
                            indicatorColor = selectedTheme.primary().copy(alpha = 0.15f)
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
                                tint = if (currentTab == 1) selectedTheme.primary() else (if (isDark) Color(0xFFCAC4D0) else Color.Gray)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = selectedTheme.primary(),
                            selectedTextColor = selectedTheme.primary(),
                            indicatorColor = selectedTheme.primary().copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        label = { Text("Cost Calc", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Cost Calculator",
                                tint = if (currentTab == 2) selectedTheme.primary() else (if (isDark) Color(0xFFCAC4D0) else Color.Gray)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = selectedTheme.primary(),
                            selectedTextColor = selectedTheme.primary(),
                            indicatorColor = selectedTheme.primary().copy(alpha = 0.15f)
                        )
                    )
                }
            },
            floatingActionButton = {
                if (currentTab == 0) {
                    FloatingActionButton(
                        onClick = { showAddTodoDialog = true },
                        containerColor = selectedTheme.primary(), // Styled by custom dynamic theme
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, selectedTheme.primary().copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Secure Planner",
                                color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20), // Primary black text
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "100% Offline Task Vault",
                                color = selectedTheme.primary(), // Themed subtitle
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSettingsScreen = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(selectedTheme.primary().copy(alpha = 0.15f))
                            .testTag("app_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "App Settings",
                            tint = selectedTheme.primary(),
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
                    placeholder = { Text("Search tasks...", color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedTheme.primary(),
                        unfocusedBorderColor = if (isDark) Color(0xFF49454F) else Color(0xFF79747E),
                        focusedContainerColor = if (isDark) Color(0xFF2E2A36) else Color(0xFFF3EDF7),
                        unfocusedContainerColor = if (isDark) Color(0xFF2E2A36) else Color(0xFFF3EDF7),
                        focusedTextColor = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
                        unfocusedTextColor = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
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
                            colors = CardDefaults.cardColors(containerColor = selectedTheme.primary().copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, selectedTheme.border().copy(alpha = 0.3f))
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
                                        color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
                                    )
                                    Text(
                                        text = "$completedCount/$totalCount Completed",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = selectedTheme.primary()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                // Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF3C3843) else Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(if (totalCount > 0) completedCount.toFloat() / totalCount else 0f)
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(selectedTheme.primary())
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Pending", fontSize = 10.sp, color = if (isDark) Color(0xFFCAC4D0) else Color.Gray)
                                        Text("$pendingCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Success Rate", fontSize = 10.sp, color = if (isDark) Color(0xFFCAC4D0) else Color.Gray)
                                        Text("$completionPercent%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = selectedTheme.primary())
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Total Tasks", fontSize = 10.sp, color = if (isDark) Color(0xFFCAC4D0) else Color.Gray)
                                        Text("$totalCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20))
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
                                    color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedCategoryFilter != "All" || selectedPriorityFilter != null) {
                                    Text(
                                        text = "Reset Filters",
                                        color = selectedTheme.primary(),
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
                                    val chipBg = if (isSelected) selectedTheme.primary().copy(alpha = 0.15f) else Color.Transparent
                                    val chipText = if (isSelected) selectedTheme.primary() else (if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                                    val chipBorderColor = if (isSelected) selectedTheme.primary() else (if (isDark) Color(0xFF49454F) else Color(0xFF79747E))

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
                                    color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
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
                                    val chipBg = if (isSelected) selectedTheme.primary().copy(alpha = 0.15f) else Color.Transparent
                                    val chipText = if (isSelected) selectedTheme.primary() else (if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F))
                                    val chipBorderColor = if (isSelected) selectedTheme.primary() else (if (isDark) Color(0xFF49454F) else Color(0xFF79747E))

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
                                        tint = selectedTheme.primary(),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sort: ",
                                        color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    var isSortMenuExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(selectedTheme.primary().copy(alpha = 0.15f))
                                                .clickable { isSortMenuExpanded = true }
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${selectedSortOption.displayName}  ▼",
                                                color = selectedTheme.primary(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = isSortMenuExpanded,
                                            onDismissRequest = { isSortMenuExpanded = false },
                                            modifier = Modifier.background(if (isDark) Color(0xFF25232A) else Color(0xFFFEF7FF))
                                        ) {
                                            TaskSortOption.values().forEach { option ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = option.displayName,
                                                            color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
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
                                        .background(if (isDark) Color(0xFF2D2930) else Color(0xFFF3EDF7))
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
                                        tint = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Export List",
                                        color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
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
                                        .background(selectedTheme.primary().copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = selectedTheme.primary(),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No tasks found!",
                                    color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Create a task or reset active search filters.",
                                    color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
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
                                onEdit = { todoToEdit = item },
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
                } else if (currentTab == 1) {
                    KhataBookDashboard(
                        viewModel = viewModel,
                        theme = selectedTheme,
                        initialShowSettings = openKhataSettingsFromMain,
                        onResetInitialSettings = { openKhataSettingsFromMain = false }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GramPriceCalculator(theme = selectedTheme)
                        Spacer(modifier = Modifier.height(100.dp))
                    }
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

        // Modal Edit Todo & Time Reminder Dialog
        if (todoToEdit != null) {
            EditTodoDialog(
                todo = todoToEdit!!,
                theme = selectedTheme,
                onDismiss = { todoToEdit = null },
                onUpdateTodo = { updated ->
                    viewModel.updateTodo(updated)
                    todoToEdit = null
                    Toast.makeText(context, "Task & reminder updated!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Dedicated Full-screen Settings Section
        AnimatedVisibility(
            visible = showSettingsScreen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { showSettingsScreen = false },
                onRequestPrivacy = {
                    showSettingsScreen = false
                    showPrivacyPolicyScreen = true
                },
                permissionGranted = permissionGranted,
                onRequestNotificationPermission = {
                    requestPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
                },
                onOpenKhataSettings = {
                    showSettingsScreen = false
                    currentTab = 1
                    openKhataSettingsFromMain = true
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
    onEdit: () -> Unit = {},
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

    val themePrimary = theme.primary()
    val themeContainer = theme.container()
    val themeText = theme.text()
    val themeBorder = theme.border()

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
            .clickable { onEdit() }
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

                    // Scheduled Due Date & Time Reminder
                    if (todo.dueDate != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val isOverdue = todo.dueDate < System.currentTimeMillis() && !todo.isCompleted
                        val statusColor = if (isOverdue) Color(0xFFEF4444) else themeText.copy(alpha = 0.7f)
                        val format = SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.US)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.12f) else themePrimary.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.Alarm,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = format.format(Date(todo.dueDate)),
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Edit Action button
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("edit_todo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit task",
                    tint = themeText.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete Action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("delete_todo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task",
                    tint = themeText.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeReminderSection(
    dueDate: Long?,
    onDueDateChange: (Long?) -> Unit,
    themePrimary: Color
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = themePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Time Reminder & Due Date",
                    color = Color(0xFF1D1B20),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (dueDate != null) {
                TextButton(
                    onClick = { onDueDateChange(null) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Clear", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Selected Date & Time summary box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (dueDate != null) themePrimary.copy(alpha = 0.08f) else Color(0xFFF4F3F6))
                .border(
                    width = 1.dp,
                    color = if (dueDate != null) themePrimary.copy(alpha = 0.4f) else Color(0xFFCAC4D0),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(12.dp)
        ) {
            if (dueDate == null) {
                Text(
                    text = "No time reminder set. Tap 'Pick Date & Time' to schedule an alert.",
                    color = Color(0xFF49454F),
                    fontSize = 12.sp
                )
            } else {
                Column {
                    val dateFormatted = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(Date(dueDate))
                    val timeFormatted = SimpleDateFormat("hh:mm a", Locale.US).format(Date(dueDate))
                    Text(
                        text = dateFormatted,
                        color = Color(0xFF1D1B20),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "⏰ System Alert at $timeFormatted",
                        color = themePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: Pick Date & Time / Change Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    if (dueDate != null) cal.timeInMillis = dueDate

                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selCal = Calendar.getInstance()
                            if (dueDate != null) selCal.timeInMillis = dueDate
                            selCal.set(Calendar.YEAR, year)
                            selCal.set(Calendar.MONTH, month)
                            selCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    selCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    selCal.set(Calendar.MINUTE, minute)
                                    selCal.set(Calendar.SECOND, 0)
                                    selCal.set(Calendar.MILLISECOND, 0)
                                    onDueDateChange(selCal.timeInMillis)
                                },
                                selCal.get(Calendar.HOUR_OF_DAY),
                                selCal.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = themePrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pick Date & Time", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (dueDate != null) {
                Button(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = dueDate
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                cal.set(Calendar.MINUTE, minute)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                onDueDateChange(cal.timeInMillis)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themePrimary.copy(alpha = 0.15f),
                        contentColor = themePrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Time", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Preset Chips
        Text(text = "Quick Time Presets:", color = Color(0xFF49454F), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                "In 1 hr" to { System.currentTimeMillis() + 3600_000L },
                "Tonight 8 PM" to {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 20)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    if (cal.timeInMillis <= System.currentTimeMillis()) {
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                    }
                    cal.timeInMillis
                },
                "Tomorrow 9 AM" to {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 9)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.timeInMillis
                }
            )

            presets.forEach { (label, getMillis) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(themePrimary.copy(alpha = 0.1f))
                        .border(1.dp, themePrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .clickable { onDueDateChange(getMillis()) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(label, color = themePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
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

    val categories = listOf("Personal", "Work", "Shopping", "Finance")
    val context = LocalContext.current
    val themePrimary = Color(theme.primaryColor)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFEF7FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .wrapContentHeight()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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

                Spacer(modifier = Modifier.height(16.dp))

                // Time Reminder Section
                TimeReminderSection(
                    dueDate = dueDate,
                    onDueDateChange = { dueDate = it },
                    themePrimary = themePrimary
                )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTodoDialog(
    todo: TodoItem,
    theme: TaskCardTheme,
    onDismiss: () -> Unit,
    onUpdateTodo: (TodoItem) -> Unit
) {
    var title by remember { mutableStateOf(todo.title) }
    var description by remember { mutableStateOf(todo.description) }
    var priority by remember { mutableStateOf(todo.priority) }
    var category by remember { mutableStateOf(todo.category) }
    var dueDate by remember { mutableStateOf<Long?>(todo.dueDate) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Personal", "Work", "Shopping", "Finance")
    val context = LocalContext.current
    val themePrimary = Color(theme.primaryColor)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFEF7FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .wrapContentHeight()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Edit Task & Reminder",
                    color = Color(0xFF1D1B20),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title", color = Color(0xFF49454F)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themePrimary,
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = Color(0xFF49454F)) },
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

                Spacer(modifier = Modifier.height(16.dp))

                TimeReminderSection(
                    dueDate = dueDate,
                    onDueDateChange = { dueDate = it },
                    themePrimary = themePrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                                onUpdateTodo(
                                    todo.copy(
                                        title = title,
                                        description = description,
                                        priority = priority,
                                        category = category,
                                        dueDate = dueDate
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Update Task", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: TodoViewModel,
    onBack: () -> Unit,
    onRequestPrivacy: () -> Unit,
    permissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenKhataSettings: () -> Unit = {}
) {
    val areRemindersEnabled by viewModel.areRemindersEnabled.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val todos by viewModel.filteredTodos.collectAsState()
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE) }
    var glassMode by remember { mutableStateOf(prefs.getBoolean("glass_mode", true)) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val surfaceColor = if (isDark) Color(0xFF141218) else Color(0xFFFEF7FF)
    val cardBg = if (isDark) Color(0xFF1D1B22) else Color(0xFFFFFFFF)
    val textMain = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSecondary = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
    val themeColor = selectedTheme.primary()
    val borderStrokeColor = (if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)).copy(alpha = 0.4f)

    val completedCount = todos.count { it.isCompleted }
    val totalCount = todos.size

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = surfaceColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
            // Top Bar Header with Back Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(themeColor.copy(alpha = 0.12f))
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Main",
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "App Settings",
                        color = textMain,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Customize layout, notifications & security",
                        color = textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Settings Scrollable Section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Section 1: Appearance & Theme Customization
                Text(
                    text = "APPEARANCE & THEMES",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Accent Color Palette", color = textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Current: ${selectedTheme.displayName}", color = textSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(TaskCardTheme.values()) { theme ->
                                val isSelected = selectedTheme == theme
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(theme.getColors(isDark).container))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color(theme.getColors(isDark).primary) else borderStrokeColor,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.selectTheme(theme) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(theme.getColors(isDark).primary))
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

                        Spacer(modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(borderStrokeColor.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Dark Theme Switch Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dark Mode", color = textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Use dark high-contrast mode", color = textSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isDark,
                                onCheckedChange = { viewModel.toggleDarkTheme(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = themeColor,
                                    checkedTrackColor = themeColor.copy(alpha = 0.25f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(borderStrokeColor.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Glassmorphism UI Theme Switch Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Visual Glassmorphism & UI Themes", color = textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Frosted translucent cards with dynamic refraction gradients", color = textSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = glassMode,
                                onCheckedChange = {
                                    glassMode = it
                                    prefs.edit().putBoolean("glass_mode", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = themeColor,
                                    checkedTrackColor = themeColor.copy(alpha = 0.25f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Khata Ledger Settings Option
                Text(
                    text = "KHATA LEDGER & STORE SETTINGS",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                val shopName = prefs.getString("default_shop_name", "KhataIndex") ?: "KhataIndex"
                                val curr = prefs.getString("currency_symbol", "₹") ?: "₹"
                                Text("Store & Ledger Profile", color = textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Store: $shopName • Active Currency: $curr", color = textSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onOpenKhataSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Khata Ledger Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Notifications & Reminders
                Text(
                    text = "NOTIFICATIONS & REMINDERS",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(themeColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Task Scheduled Alerts", color = textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Overdue & upcoming reminders", color = textSecondary, fontSize = 11.sp)
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
                                    checkedThumbColor = themeColor,
                                    checkedTrackColor = themeColor.copy(alpha = 0.25f)
                                )
                            )
                        }

                        if (areRemindersEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.triggerTestReminder() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeColor.copy(alpha = 0.12f),
                                    contentColor = themeColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Trigger Test Reminder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 3: Data & Storage
                Text(
                    text = "DATA & STORAGE VAULT",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Local SQLite Storage", color = textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("$totalCount Total Tasks stored on device", color = textSecondary, fontSize = 11.sp)
                            }
                        }

                        if (completedCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showClearConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                                    contentColor = Color(0xFFEF4444)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear $completedCount Completed Tasks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 4: Security & Privacy
                Text(
                    text = "SECURITY & PRIVACY",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("100% Offline Vault", color = textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Zero network tracking or ad analytics", color = textSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onRequestPrivacy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor.copy(alpha = 0.12f),
                                contentColor = themeColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Read Full Privacy Policy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App Info & Version Footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Secure Planner Pro v1.2.0",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = textMain
                    )
                    Text(
                        text = "Designed by Subhajit Roy • Offline Vault",
                        fontSize = 10.sp,
                        color = textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

    // Confirmation dialog for clearing completed tasks
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Completed Tasks?", fontWeight = FontWeight.Bold, color = textMain) },
            text = { Text("Are you sure you want to remove all completed tasks from local storage?", fontSize = 13.sp, color = textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCompletedTodos()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun PrivacyPolicyScreen(
    theme: TaskCardTheme,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDark.current
    val themePrimary = Color(theme.primaryColor)
    val scrollState = rememberScrollState()

    val surfaceBg = if (isDark) Color(0xFF141218) else Color(0xFFFEF7FF)
    val textMain = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSub = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = surfaceBg
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
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
                        color = textMain
                    )
                    Text(
                        text = "Last updated: July 2026",
                        fontSize = 11.sp,
                        color = textSub
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
                    color = textSub,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                PrivacySectionHeader(title = "1. Personal Information Collection", color = textMain)
                PrivacySectionBody(
                    text = "We collect absolutely zero personally identifiable information (PII). You are not required to create an account, register an email address, verify your phone number, or connect any social profile to use the application.",
                    color = textSub
                )

                PrivacySectionHeader(title = "2. Task Data and Local Storage", color = textMain)
                PrivacySectionBody(
                    text = "All data created inside the app (including task titles, detailed descriptions, due dates, categories, priorities, and completed states) is saved strictly on your local physical device. \n\nWe utilize Android's built-in SQLite database engine managed by the Room Persistence Library. No task data is transmitted over the internet, and no cloud-synchronization components are active.",
                    color = textSub
                )

                PrivacySectionHeader(title = "3. Zero Third-Party Tracker SDKs", color = textMain)
                PrivacySectionBody(
                    text = "Unlike traditional apps, Secure Planner Pro has completely REMOVED Google AdSense, AdMob, Firebase Analytics, and any telemetry scripts. There are no tracking scripts or audience measurement networks running in the background. Your behavior, task habits, and schedule remain 100% private to you.",
                    color = textSub
                )

                PrivacySectionHeader(title = "4. Device Permissions Explained", color = textMain)
                PrivacySectionBody(
                    text = "• Local System Notifications: Used strictly to schedule alerts for overdue tasks or upcoming deadlines. These notifications are processed completely locally by the Android operating system and do not use any cloud messaging services. \n\n• Boot Completed: Used to re-register scheduled task alerts upon system restart.",
                    color = textSub
                )

                PrivacySectionHeader(title = "5. Data Erasure and Lifecycle", color = textMain)
                PrivacySectionBody(
                    text = "Since all data is saved locally on your device, you have complete control over its lifecycle. You can wipe your data at any time by: \n\n1. Clearing individual tasks inside the app list.\n2. Selecting 'Clear Completed Tasks' in the interface.\n3. Going to Android Settings -> Apps -> Secure Planner -> Storage -> Clear Data. \n\nUninstalling the application will automatically purge the entire database permanently.",
                    color = textSub
                )

                PrivacySectionHeader(title = "6. Security Architecture", color = textMain)
                PrivacySectionBody(
                    text = "Secure Planner Pro operates within the standard Android application secure container sandbox. This isolating mechanism ensures that no other third-party applications installed on your device can inspect, read, or tamper with your tasks or settings database.",
                    color = textSub
                )

                PrivacySectionHeader(title = "7. Contact and Support", color = textMain)
                PrivacySectionBody(
                    text = "If you have any questions or require support regarding your offline planner application, you may contact our lead developer:\n\nDeveloper: Subhajit Roy\nEmail: romendraroy4@gmail.com\nSupport: None",
                    color = textSub
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
}

@Composable
fun PrivacySectionHeader(title: String, color: Color = Color(0xFF1D1B20)) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun PrivacySectionBody(text: String, color: Color = Color(0xFF49454F)) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}
