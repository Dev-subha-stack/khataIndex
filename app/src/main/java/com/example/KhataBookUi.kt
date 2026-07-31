package com.example

import android.content.Context
import android.content.Intent
import android.content.ContentValues
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.KhataContact
import com.example.data.KhataTransaction
import java.text.SimpleDateFormat
import java.util.*

// Helper functions for Currency and Glassmorphism preferences
fun getCurrencySymbol(context: Context): String {
    val prefs = context.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE)
    return prefs.getString("currency_symbol", "₹") ?: "₹"
}

fun formatKhataCurrency(amount: Double, context: Context): String {
    val prefs = context.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE)
    val symbol = prefs.getString("currency_symbol", "₹") ?: "₹"
    val showDecimals = prefs.getBoolean("show_decimals", true)
    return if (showDecimals) {
        String.format("%s%.2f", symbol, amount)
    } else {
        String.format("%s%.0f", symbol, amount)
    }
}

@Composable
fun GlassCardContainer(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    themeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDark.current
    val prefs = LocalContext.current.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE)
    val glassMode = prefs.getBoolean("glass_mode", true)

    val glassBg = if (glassMode) {
        if (isDark) Color(0xFF1F1C2B).copy(alpha = 0.68f) else Color.White.copy(alpha = 0.78f)
    } else {
        if (isDark) Color(0xFF211D2A) else Color.White
    }

    val glassBorder = Brush.linearGradient(
        colors = if (isDark) listOf(
            Color.White.copy(alpha = 0.32f),
            themeColor.copy(alpha = 0.45f),
            Color.White.copy(alpha = 0.1f)
        ) else listOf(
            Color.White.copy(alpha = 0.95f),
            themeColor.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.5f)
        )
    )

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glass_scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .border(1.dp, glassBorder, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        isPressed = true
                        onClick()
                    }
                } else Modifier
            ),
        shape = shape,
        color = glassBg,
        tonalElevation = if (glassMode) 6.dp else 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun KhataBookDashboard(
    viewModel: TodoViewModel,
    theme: TaskCardTheme,
    initialShowSettings: Boolean = false,
    onResetInitialSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Wholesalers (Sellers), 1 = Customers (Consumers)
    var selectedContact by remember { mutableStateOf<KhataContact?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showKhataSettingsScreen by remember(initialShowSettings) { mutableStateOf(initialShowSettings) }
    var khataSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(initialShowSettings) {
        if (initialShowSettings) {
            showKhataSettingsScreen = true
        }
    }

    val sellers by viewModel.khataSellers.collectAsState()
    val customers by viewModel.khataCustomers.collectAsState()
    val allTransactions by viewModel.allKhataTransactions.collectAsState()

    val themeColor = theme.primary()
    val containerBg = theme.container()

    AnimatedContent(
        targetState = Pair(selectedContact, showKhataSettingsScreen),
        transitionSpec = {
            if (targetState.second || targetState.first != null) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "KhataNavigation"
    ) { (contact, showSettings) ->
        if (showSettings) {
            KhataSettingsScreen(
                viewModel = viewModel,
                theme = theme,
                onBack = {
                    showKhataSettingsScreen = false
                    onResetInitialSettings()
                }
            )
        } else if (contact != null) {
            // Contact Details and Ledgers View
            ContactDetailsScreen(
                contact = contact,
                viewModel = viewModel,
                theme = theme,
                onBack = { selectedContact = null }
            )
        } else {
            // Main Dashboard List
            val prefs = remember { context.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE) }
            val showLedgerSummary = remember { prefs.getBoolean("show_ledger_summary", true) }
            val shopName = remember { prefs.getString("default_shop_name", "KhataIndex") ?: "KhataIndex" }

            val mainFilePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    importCustomersFromFile(context, it, viewModel, if (selectedTab == 0) "SELLER" else "CUSTOMER")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header Segment with Glassmorphism
                GlassCardContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    themeColor = themeColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon),
                            contentDescription = "KhataIndex Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$shopName Ledger",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
                            )
                            Text(
                                text = "Track credits, payments & WhatsApp statements securely offline.",
                                fontSize = 11.sp,
                                color = if (LocalIsDark.current) Color(0xFFCAC4D0) else Color(0xFF49454F)
                            )
                        }
                        IconButton(
                            onClick = { showKhataSettingsScreen = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(themeColor.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Khata Settings",
                                tint = themeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Optional Net Ledger Summary Header Card
                if (showLedgerSummary) {
                    val totalWeOwe = remember(allTransactions, sellers) {
                        val sellerIds = sellers.map { it.id }.toSet()
                        val sellerTx = allTransactions.filter { it.contactId in sellerIds }
                        val owe = sellerTx.filter { it.type == "WE_OWE" }.sumOf { it.amount }
                        val paid = sellerTx.filter { it.type == "WE_PAID" }.sumOf { it.amount }
                        (owe - paid).coerceAtLeast(0.0)
                    }
                    val totalTheyOweUs = remember(allTransactions, customers) {
                        val customerIds = customers.map { it.id }.toSet()
                        val custTx = allTransactions.filter { it.contactId in customerIds }
                        val owe = custTx.filter { it.type == "THEY_OWE" }.sumOf { it.amount }
                        val paid = custTx.filter { it.type == "THEY_PAID" }.sumOf { it.amount }
                        (owe - paid).coerceAtLeast(0.0)
                    }

                    GlassCardContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        themeColor = themeColor
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "WE OWE WHOLESALERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(
                                    text = formatKhataCurrency(totalWeOwe, context),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF388E3C)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(1.dp)
                                    .background(Color.Gray.copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(text = "CUSTOMERS OWE US", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(
                                    text = formatKhataCurrency(totalTheyOweUs, context),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }

                // Frosted Tab Switcher for Sellers vs Customers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (LocalIsDark.current) Color(0xFF1E1B28).copy(alpha = 0.6f) else Color.LightGray.copy(alpha = 0.25f)
                        )
                        .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    listOf("🏢 Wholesalers (${sellers.size})", "🛍️ Customers (${customers.size})").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val tabScale by animateFloatAsState(targetValue = if (isSelected) 1f else 0.96f, label = "tab_scale")

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .scale(tabScale)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(
                                        listOf(themeColor, themeColor.copy(alpha = 0.85f))
                                    ) else SolidColor(Color.Transparent)
                                )
                                .clickable {
                                    selectedTab = index
                                    khataSearchQuery = ""
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else (if (LocalIsDark.current) Color(0xFFCAC4D0) else Color(0xFF49454F))
                            )
                        }
                    }
                }

                // Section title and Action buttons (Upload / Download)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "Wholesalers" else "Customers",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Upload CSV / TXT button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(themeColor.copy(alpha = 0.15f))
                                .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable {
                                    mainFilePickerLauncher.launch("*/*")
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = "Upload CSV / TXT",
                                    tint = themeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Upload",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            }
                        }

                        // Download CSV button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(themeColor.copy(alpha = 0.12f))
                                .clickable {
                                    downloadAllCustomersCsv(
                                        context,
                                        if (selectedTab == 0) sellers else customers,
                                        allTransactions
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download CSV",
                                    tint = themeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Export CSV",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            }
                        }
                    }
                }

                // Search bar for Contacts
                OutlinedTextField(
                    value = khataSearchQuery,
                    onValueChange = { khataSearchQuery = it },
                    placeholder = { Text("Search by name or phone...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (khataSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { khataSearchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
                        unfocusedTextColor = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = if (LocalIsDark.current) Color(0xFF49454F) else Color(0xFFCAC4D0).copy(alpha = 0.8f),
                        focusedContainerColor = if (LocalIsDark.current) Color(0xFF1D1B22) else Color.White,
                        unfocusedContainerColor = if (LocalIsDark.current) Color(0xFF1D1B22) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                // Add Contact Action Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { showAddContactDialog = true },
                    colors = CardDefaults.cardColors(containerColor = if (LocalIsDark.current) Color(0xFF211D2A) else Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (LocalIsDark.current) Color(0xFF49454F) else Color.LightGray.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTab == 0) "Add New Wholesaler / Seller" else "Add New Customer",
                            color = themeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                val displayedContacts = if (selectedTab == 0) sellers else customers
                val filteredContacts = remember(displayedContacts, khataSearchQuery) {
                    if (khataSearchQuery.isBlank()) {
                        displayedContacts
                    } else {
                        displayedContacts.filter {
                            it.name.contains(khataSearchQuery, ignoreCase = true) ||
                            it.phone.contains(khataSearchQuery, ignoreCase = true)
                        }
                    }
                }

                if (displayedContacts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Store else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) "No Wholesalers Recorded" else "No Customers Recorded",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Tap the button above to add a contact and manage credits.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else if (filteredContacts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Contacts Match Search",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Try searching with a different name or phone number.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredContacts, key = { it.id }) { contact ->
                            // Calculate outstanding balance
                            val contactTx = allTransactions.filter { it.contactId == contact.id }
                            val balance = if (contact.type == "SELLER") {
                                val weOwe = contactTx.filter { it.type == "WE_OWE" }.sumOf { it.amount }
                                val wePaid = contactTx.filter { it.type == "WE_PAID" }.sumOf { it.amount }
                                weOwe - wePaid
                            } else {
                                val theyOwe = contactTx.filter { it.type == "THEY_OWE" }.sumOf { it.amount }
                                val theyPaid = contactTx.filter { it.type == "THEY_PAID" }.sumOf { it.amount }
                                theyOwe - theyPaid
                            }

                            ContactLedgerCard(
                                contact = contact,
                                balance = balance,
                                theme = theme,
                                onClick = { selectedContact = contact }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            type = if (selectedTab == 0) "SELLER" else "CUSTOMER",
            theme = theme,
            onDismiss = { showAddContactDialog = false },
            onAdd = { name, phone ->
                viewModel.addKhataContact(name, phone, if (selectedTab == 0) "SELLER" else "CUSTOMER")
                showAddContactDialog = false
                Toast.makeText(context, "Contact added successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ContactLedgerCard(
    contact: KhataContact,
    balance: Double,
    theme: TaskCardTheme,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val themeColor = theme.primary()
    val isDark = LocalIsDark.current

    GlassCardContainer(
        modifier = Modifier.fillMaxWidth(),
        themeColor = themeColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                themeColor.copy(alpha = 0.35f),
                                themeColor.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(1.dp, themeColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = themeColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = contact.phone,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (contact.type == "SELLER") "We owe them" else "They owe us",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = formatKhataCurrency(balance, context),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (balance > 0) Color(0xFFD32F2F) else Color(0xFF388E3C)
                )
            }
        }
    }
}

@Composable
fun AddContactDialog(
    type: String,
    theme: TaskCardTheme,
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val themeColor = theme.primary()

    val isDark = LocalIsDark.current
    val surfaceColor = if (isDark) Color(0xFF1D1B22) else Color.White
    val textMain = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSecondary = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
    val inputBg = if (isDark) Color(0xFF2E2A36) else Color.White
    val borderStrokeColor = if (isDark) Color(0xFF49454F) else Color.LightGray.copy(alpha = 0.5f)
    val borderFieldColor = if (isDark) Color(0xFF49454F) else Color(0xFF79747E)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (type == "SELLER") "Add New Wholesaler" else "Add New Customer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textMain
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = textSecondary,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = borderFieldColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = textSecondary,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = borderFieldColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (name.isNotBlank()) onAdd(name, phone) },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Contact", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactDetailsScreen(
    contact: KhataContact,
    viewModel: TodoViewModel,
    theme: TaskCardTheme,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val themeColor = theme.primary()
    val containerBg = theme.container()

    val transactions by viewModel.getTransactionsForContact(contact.id).collectAsState(initial = emptyList())

    var showAddTxDialog by remember { mutableStateOf(false) }
    var selectedTxType by remember { mutableStateOf("") } // "OWE" or "PAID"
    var filterType by remember { mutableStateOf("ALL") } // "ALL", "PURCHASES", "PAYMENTS"
    var showSendBillDialog by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, filterType) {
        val reversedTx = transactions.reversed()
        when (filterType) {
            "PURCHASES" -> {
                if (contact.type == "SELLER") {
                    reversedTx.filter { it.type == "WE_OWE" }
                } else {
                    reversedTx.filter { it.type == "THEY_OWE" }
                }
            }
            "PAYMENTS" -> {
                if (contact.type == "SELLER") {
                    reversedTx.filter { it.type == "WE_PAID" }
                } else {
                    reversedTx.filter { it.type == "THEY_PAID" }
                }
            }
            else -> reversedTx
        }
    }

    // Calculate outstanding
    val balance = if (contact.type == "SELLER") {
        val weOwe = transactions.filter { it.type == "WE_OWE" }.sumOf { it.amount }
        val wePaid = transactions.filter { it.type == "WE_PAID" }.sumOf { it.amount }
        weOwe - wePaid
    } else {
        val theyOwe = transactions.filter { it.type == "THEY_OWE" }.sumOf { it.amount }
        val theyPaid = transactions.filter { it.type == "THEY_PAID" }.sumOf { it.amount }
        theyOwe - theyPaid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
                )
                Text(
                    text = if (contact.type == "SELLER") "Wholesaler Account" else "Customer Account",
                    fontSize = 11.sp,
                    color = themeColor
                )
            }
            // Delete contact action
            IconButton(
                onClick = {
                    viewModel.deleteKhataContact(contact.id)
                    onBack()
                    Toast.makeText(context, "Contact deleted securely!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Contact", tint = Color.Gray)
            }
        }

        // Outstanding Balance Summary Card with Glassmorphism
        GlassCardContainer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            themeColor = themeColor
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (contact.type == "SELLER") "Net Outstanding Balance (We Owe)" else "Net Outstanding Dues (They Owe Us)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatKhataCurrency(balance, context),
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = if (balance > 0) Color(0xFFD32F2F) else Color(0xFF388E3C)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Send Bill & Copy Statement row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val billText = generateBillText(contact, transactions, balance, context)
                            clipboardManager.setText(AnnotatedString(billText))
                            Toast.makeText(context, "Statement copied to Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Statement", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showSendBillDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Bill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Paid / Payment Progress Graph Card
        val totalCredit = remember(transactions) {
            if (contact.type == "SELLER") {
                transactions.filter { it.type == "WE_OWE" }.sumOf { it.amount }
            } else {
                transactions.filter { it.type == "THEY_OWE" }.sumOf { it.amount }
            }
        }
        val totalPaid = remember(transactions) {
            if (contact.type == "SELLER") {
                transactions.filter { it.type == "WE_PAID" }.sumOf { it.amount }
            } else {
                transactions.filter { it.type == "THEY_PAID" }.sumOf { it.amount }
            }
        }
        val paidRatio = if (totalCredit > 0) (totalPaid / totalCredit).coerceIn(0.0, 1.0).toFloat()
        else if (totalPaid > 0) 1.0f
        else 0.0f
        val paidPct = (paidRatio * 100).toInt()

        GlassCardContainer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            themeColor = themeColor
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Payment Settlement Graph",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
                        )
                    }
                    Text(
                        text = if (paidRatio >= 1.0f && totalCredit > 0) "100% Fully Cleared ✅" else "$paidPct% Paid",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (paidRatio >= 1.0f) Color(0xFF388E3C) else themeColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress graph bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (LocalIsDark.current) Color(0xFF2E2A36) else Color.LightGray.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = paidRatio)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF388E3C),
                                        Color(0xFF81C784)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL CREDIT / SALES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(formatKhataCurrency(totalCredit, context), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL PAID RECEIVED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(formatKhataCurrency(totalPaid, context), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("REMAINING DUES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(
                            formatKhataCurrency(balance, context),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (balance <= 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Ledger Entry Transactions Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📜 Timeline of Purchases & Payments",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
            )
            Text(
                text = "${filteredTransactions.size} shown",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "All Feed",
                "PURCHASES" to if (contact.type == "SELLER") "Credit Purchases" else "Credit Sales (Gave)",
                "PAYMENTS" to if (contact.type == "SELLER") "We Paid" else "Cash Received (Paid)"
            ).forEach { (type, label) ->
                val isSelected = filterType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) themeColor.copy(alpha = 0.15f)
                            else Color(0xFFF3EDF7)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) themeColor else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { filterType = type }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) themeColor else Color(0xFF49454F)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Ledger List with Timeline styling
        if (filteredTransactions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = Color.LightGray.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No timeline entries found.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                itemsIndexed(filteredTransactions, key = { _, tx -> tx.id }) { index, tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Vertical Timeline path connector
                        val isAddition = tx.type == "THEY_OWE" || tx.type == "WE_OWE"
                        val nodeColor = if (isAddition) Color(0xFFC62828) else Color(0xFF2E7D32)

                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Stem connector line
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(2.dp)
                                    .background(Color.LightGray.copy(alpha = 0.5f))
                            )
                            // Circle node dot
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(nodeColor.copy(alpha = 0.15f))
                                    .border(1.5.dp, nodeColor, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Transaction Card
                        Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                            TransactionItemRow(
                                transaction = tx,
                                contactType = contact.type,
                                theme = theme,
                                onDelete = { viewModel.deleteKhataTransaction(tx) },
                                showTimelineLine = false // Hide inner timeline line since we draw the unified stem here!
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action buttons to record ledger items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (contact.type == "SELLER") {
                Button(
                    onClick = {
                        selectedTxType = "WE_OWE"
                        showAddTxDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Took Udhaar (I Owe)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        selectedTxType = "WE_PAID"
                        showAddTxDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Paid Cash", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        selectedTxType = "THEY_OWE"
                        showAddTxDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Gave Udhaar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        selectedTxType = "THEY_PAID"
                        showAddTxDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Got Cash", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            txType = selectedTxType,
            contactType = contact.type,
            theme = theme,
            onDismiss = { showAddTxDialog = false },
            onAdd = { desc, amount ->
                viewModel.addKhataTransaction(contact.id, desc, amount, selectedTxType)
                showAddTxDialog = false
                Toast.makeText(context, "Ledger entry recorded!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSendBillDialog) {
        SendBillDialog(
            contact = contact,
            defaultAmount = kotlin.math.abs(balance),
            theme = theme,
            onDismiss = { showSendBillDialog = false },
            onShare = { message ->
                showSendBillDialog = false
                shareStatement(context, contact.name, message)
            },
            onCopy = { message ->
                clipboardManager.setText(AnnotatedString(message))
                Toast.makeText(context, "Bill copied to Clipboard!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun TransactionItemRow(
    transaction: KhataTransaction,
    contactType: String,
    theme: TaskCardTheme,
    onDelete: () -> Unit,
    showTimelineLine: Boolean = true
) {
    val themeColor = theme.primary()
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val formattedDate = sdf.format(Date(transaction.timestamp))

    val isAddition = transaction.type == "THEY_OWE" || transaction.type == "WE_OWE"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showTimelineLine) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isAddition) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                        .border(1.5.dp, if (isAddition) Color(0xFFC62828) else Color(0xFF2E7D32), CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = if (LocalIsDark.current) Color(0xFF211D2A) else Color.White),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (LocalIsDark.current) Color(0xFF49454F) else Color.LightGray.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAddition) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAddition) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isAddition) Color(0xFFC62828) else Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val actionLabel = when (transaction.type) {
                        "THEY_OWE" -> "Gave Udhaar"
                        "THEY_PAID" -> "Got Cash"
                        "WE_OWE" -> "Took Udhaar"
                        "WE_PAID" -> "Paid Cash"
                        else -> "Ledger Entry"
                    }
                    Text(
                        text = actionLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAddition) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                    Text(
                        text = if (transaction.description.isNotBlank()) transaction.description else {
                            when (transaction.type) {
                                "THEY_OWE" -> "Gave items on credit (Udhaar)"
                                "THEY_PAID" -> "Cash payment received (Got)"
                                "WE_OWE" -> "Bought items on credit (Udhaar)"
                                "WE_PAID" -> "Payment settled (Paid)"
                                else -> "Ledger entry"
                            }
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAddition) "+ ₹${transaction.amount}" else "- ₹${transaction.amount}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (isAddition) Color(0xFFC62828) else Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete transaction",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    txType: String,
    contactType: String,
    theme: TaskCardTheme,
    onDismiss: () -> Unit,
    onAdd: (description: String, amount: Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val themeColor = theme.primary()

    val dialogTitle = when (txType) {
        "THEY_OWE" -> "Gave Goods on Credit (Udhaar)"
        "THEY_PAID" -> "Received Payment (Cash Got)"
        "WE_OWE" -> "Took Goods on Credit (Udhaar)"
        "WE_PAID" -> "Paid Money (Cash Settled)"
        else -> "New Entry"
    }

    val isDark = LocalIsDark.current
    val surfaceColor = if (isDark) Color(0xFF1D1B22) else Color.White
    val textMain = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSecondary = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
    val inputBg = if (isDark) Color(0xFF2E2A36) else Color.White
    val borderStrokeColor = if (isDark) Color(0xFF49454F) else Color.LightGray.copy(alpha = 0.5f)
    val borderFieldColor = if (isDark) Color(0xFF49454F) else Color(0xFF79747E)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textMain
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = textSecondary,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = borderFieldColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Items / Purchased Notes") },
                    placeholder = { Text("e.g. 5 bags wheat, partial settlement") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = textSecondary,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = borderFieldColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amtDouble = amount.toDoubleOrNull()
                            if (amtDouble != null && amtDouble > 0) {
                                onAdd(description, amtDouble)
                            } else {
                                amount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Entry", color = Color.White)
                    }
                }
            }
        }
    }
}

// Statement and Bill Text compiler helper
fun generateBillText(contact: KhataContact, transactions: List<KhataTransaction>, balance: Double, context: Context): String {
    val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    val prefs = context.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE)
    val shopName = prefs.getString("default_shop_name", "KhataIndex") ?: "KhataIndex"
    val shopUpi = prefs.getString("shop_upi_id", "") ?: ""
    val tagline = prefs.getString("bill_tagline", "Thank you for doing business with us!") ?: "Thank you for doing business with us!"

    val isPaid = balance <= 0.0
    val statusText = if (isPaid) "BILL STATUS: PAID / FULLY SETTLED ✅" else "BILL STATUS: PAYMENT PENDING ⏳"

    val sb = StringBuilder()
    sb.append("=============================\n")
    sb.append("   $shopName LEDGER STATEMENT\n")
    sb.append("=============================\n")
    sb.append("Party Name: ${contact.name}\n")
    if (contact.phone.isNotBlank()) {
        sb.append("Phone: ${contact.phone}\n")
    }
    sb.append("Account Type: ${if (contact.type == "SELLER") "Wholesaler (Seller)" else "Customer (Buyer)"}\n")
    sb.append("Statement Date: ${sdf.format(Date())}\n")
    sb.append("-----------------------------\n")
    sb.append("$statusText\n")
    sb.append("-----------------------------\n")
    sb.append("Transaction Log:\n")

    if (transactions.isEmpty()) {
        sb.append("No recorded entries.\n")
    } else {
        transactions.forEach { tx ->
            val dateStr = sdf.format(Date(tx.timestamp))
            val desc = if (tx.description.isNotBlank()) tx.description else {
                when (tx.type) {
                    "THEY_OWE" -> "Items bought on Credit"
                    "THEY_PAID" -> "Cash payment"
                    "WE_OWE" -> "Wholesale bought"
                    "WE_PAID" -> "Cash paid"
                    else -> "Ledger item"
                }
            }
            val sign = if (tx.type == "THEY_OWE" || tx.type == "WE_OWE") "(+)" else "(-)"
            sb.append("$dateStr: $desc $sign ${formatKhataCurrency(tx.amount, context)}\n")
        }
    }

    sb.append("-----------------------------\n")
    if (contact.type == "SELLER") {
        sb.append("NET AMOUNT WE OWE: ${formatKhataCurrency(balance, context)}\n")
    } else {
        sb.append("NET AMOUNT THEY OWE US: ${formatKhataCurrency(balance, context)}\n")
    }
    if (shopUpi.isNotBlank()) {
        sb.append("UPI Payment Handle: $shopUpi\n")
    }
    sb.append("-----------------------------\n")
    sb.append("$tagline\n")
    return sb.toString()
}

fun buildCustomerReportText(
    context: Context,
    contact: KhataContact,
    transactions: List<KhataTransaction>,
    balance: Double
): String {
    val sdf = SimpleDateFormat("dd-MMM-yyyy, hh:mm a", Locale.getDefault())
    val dateOnlySdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    val prefs = context.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE)
    val shopName = prefs.getString("default_shop_name", "KhataIndex") ?: "KhataIndex"
    val curr = getCurrencySymbol(context)

    val totalPurchases = if (contact.type == "SELLER") {
        transactions.filter { it.type == "WE_OWE" }.sumOf { it.amount }
    } else {
        transactions.filter { it.type == "THEY_OWE" }.sumOf { it.amount }
    }

    val totalPayments = if (contact.type == "SELLER") {
        transactions.filter { it.type == "WE_PAID" }.sumOf { it.amount }
    } else {
        transactions.filter { it.type == "THEY_PAID" }.sumOf { it.amount }
    }

    val isSettled = balance <= 0.0
    val statusStr = if (isSettled) "PAID / FULLY SETTLED ✅" else "PENDING PAYMENT (DUE: $curr${String.format("%.2f", balance)}) ⏳"

    val sb = StringBuilder()
    sb.append("====================================================\n")
    sb.append("         $shopName - CUSTOMER LEDGER REPORT\n")
    sb.append("====================================================\n")
    sb.append("Customer Name   : ${contact.name}\n")
    sb.append("Phone Number    : ${if (contact.phone.isNotBlank()) contact.phone else "Not Provided"}\n")
    sb.append("Account Type    : ${if (contact.type == "SELLER") "Wholesaler (Seller)" else "Customer (Buyer)"}\n")
    sb.append("Report Date     : ${sdf.format(Date())}\n")
    sb.append("----------------------------------------------------\n")
    sb.append("                FINANCIAL SUMMARY                   \n")
    sb.append("----------------------------------------------------\n")
    sb.append("Total Entries   : ${transactions.size}\n")
    sb.append("Total Credit    : ${formatKhataCurrency(totalPurchases, context)}\n")
    sb.append("Total Paid      : ${formatKhataCurrency(totalPayments, context)}\n")
    sb.append("Net Balance     : ${formatKhataCurrency(balance, context)}\n")
    sb.append("Bill Status     : $statusStr\n")
    sb.append("----------------------------------------------------\n")
    sb.append("             DETAILED TRANSACTION HISTORY           \n")
    sb.append("----------------------------------------------------\n")

    if (transactions.isEmpty()) {
        sb.append("No transactions recorded yet.\n")
    } else {
        transactions.forEachIndexed { index, tx ->
            val dStr = dateOnlySdf.format(Date(tx.timestamp))
            val typeStr = when (tx.type) {
                "THEY_OWE" -> "Credit Purchase (+)"
                "THEY_PAID" -> "Payment Received (-)"
                "WE_OWE" -> "Wholesale Credit (+)"
                "WE_PAID" -> "Wholesale Payment (-)"
                else -> tx.type
            }
            val desc = if (tx.description.isNotBlank()) " | ${tx.description}" else ""
            sb.append("${index + 1}. [$dStr] $typeStr: ${formatKhataCurrency(tx.amount, context)}$desc\n")
        }
    }

    sb.append("====================================================\n")
    sb.append("Generated by $shopName Khata Ledger System\n")
    sb.append("====================================================\n")

    return sb.toString()
}

fun importCustomersFromFile(
    context: Context,
    uri: Uri,
    viewModel: TodoViewModel,
    defaultType: String = "CUSTOMER"
) {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    coroutineScope.launch {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
            val reader = BufferedReader(InputStreamReader(inputStream))
            var lineCount = 0
            var addedCount = 0

            val lines = reader.readLines()
            for (rawLine in lines) {
                val line = rawLine.trim()
                lineCount++
                if (line.isBlank()) continue

                val columns = line.split(",", "\t", ";", "|").map { it.trim().removeSurrounding("\"") }
                
                // Skip header line
                if (lineCount == 1 && (columns.firstOrNull()?.equals("ID", ignoreCase = true) == true ||
                            columns.firstOrNull()?.contains("Name", ignoreCase = true) == true ||
                            columns.firstOrNull()?.contains("Customer", ignoreCase = true) == true)) {
                    continue
                }

                if (columns.isNotEmpty()) {
                    var name = ""
                    var phone = ""
                    var type = defaultType
                    var creditAmount = 0.0
                    var paidAmount = 0.0

                    if (columns.size == 1) {
                        name = columns[0]
                    } else if (columns.size >= 2) {
                        if (columns[0].toIntOrNull() != null && columns.size >= 3) {
                            name = columns[1]
                            phone = columns[2]
                            if (columns.size >= 4 && (columns[3].equals("SELLER", true) || columns[3].equals("CUSTOMER", true))) {
                                type = columns[3].uppercase()
                            }
                            if (columns.size >= 5) creditAmount = columns[4].toDoubleOrNull() ?: 0.0
                            if (columns.size >= 6) paidAmount = columns[5].toDoubleOrNull() ?: 0.0
                        } else {
                            name = columns[0]
                            phone = columns[1]
                            if (columns.size >= 3 && (columns[2].equals("SELLER", true) || columns[2].equals("CUSTOMER", true))) {
                                type = columns[2].uppercase()
                            } else if (columns.size >= 3) {
                                creditAmount = columns[2].toDoubleOrNull() ?: 0.0
                            }
                            if (columns.size >= 4 && creditAmount == 0.0) {
                                creditAmount = columns[3].toDoubleOrNull() ?: 0.0
                            }
                            if (columns.size >= 5) {
                                paidAmount = columns[4].toDoubleOrNull() ?: 0.0
                            }
                        }
                    }

                    if (name.isNotBlank()) {
                        val contactId = viewModel.addKhataContactAndGetId(name, phone, type).toInt()
                        if (creditAmount > 0) {
                            val txType = if (type == "SELLER") "WE_OWE" else "THEY_OWE"
                            viewModel.addKhataTransaction(contactId, "Imported Balance", creditAmount, txType)
                        }
                        if (paidAmount > 0) {
                            val txType = if (type == "SELLER") "WE_PAID" else "THEY_PAID"
                            viewModel.addKhataTransaction(contactId, "Imported Paid Amount", paidAmount, txType)
                        }
                        addedCount++
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (addedCount > 0) {
                    Toast.makeText(context, "🎉 Successfully imported $addedCount customer accounts!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "⚠️ No valid customer entries found in file.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to import file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun downloadCustomerInfoFile(
    context: Context,
    contact: KhataContact,
    transactions: List<KhataTransaction>,
    balance: Double
) {
    val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val safeName = contact.name.trim().replace("[^a-zA-Z0-9]".toRegex(), "_")
    val fileName = "Customer_${safeName}_$dateSuffix.txt"
    val content = buildCustomerReportText(context, contact, transactions, balance)

    var savedSuccessfully = false

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray())
                }
                savedSuccessfully = true
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { stream ->
                stream.write(content.toByteArray())
            }
            savedSuccessfully = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (savedSuccessfully) {
        Toast.makeText(context, "📥 Customer Info saved to Downloads folder:\n$fileName", Toast.LENGTH_LONG).show()
    } else {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Customer Info - ${contact.name}", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "📥 Customer Info copied to Clipboard!", Toast.LENGTH_LONG).show()
    }
}

fun downloadAllCustomersCsv(
    context: Context,
    contacts: List<KhataContact>,
    allTransactions: List<KhataTransaction>
) {
    val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "All_Customers_Report_$dateSuffix.csv"
    val prefs = context.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE)
    val shopName = prefs.getString("default_shop_name", "KhataIndex") ?: "KhataIndex"
    val curr = getCurrencySymbol(context)

    val sb = StringBuilder()
    sb.append("ID,Customer Name,Phone Number,Account Type,Total Credit ($curr),Total Paid ($curr),Net Balance ($curr),Bill Status\n")

    contacts.forEach { contact ->
        val ctx = allTransactions.filter { it.contactId == contact.id }
        val credit = if (contact.type == "SELLER") {
            ctx.filter { it.type == "WE_OWE" }.sumOf { it.amount }
        } else {
            ctx.filter { it.type == "THEY_OWE" }.sumOf { it.amount }
        }
        val paid = if (contact.type == "SELLER") {
            ctx.filter { it.type == "WE_PAID" }.sumOf { it.amount }
        } else {
            ctx.filter { it.type == "THEY_PAID" }.sumOf { it.amount }
        }
        val balance = credit - paid
        val status = if (balance <= 0.0) "PAID" else "PENDING_DUES"

        sb.append("${contact.id},\"${contact.name.replace("\"", "\"\"")}\",\"${contact.phone}\",${contact.type},$credit,$paid,$balance,$status\n")
    }

    val content = sb.toString()
    var savedSuccessfully = false

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray())
                }
                savedSuccessfully = true
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { stream ->
                stream.write(content.toByteArray())
            }
            savedSuccessfully = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (savedSuccessfully) {
        Toast.makeText(context, "📥 Customers CSV saved to Downloads folder:\n$fileName", Toast.LENGTH_LONG).show()
    } else {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("All Customers CSV", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "📥 Customers CSV copied to Clipboard!", Toast.LENGTH_LONG).show()
    }
}

fun downloadAllCustomersTextReport(
    context: Context,
    contacts: List<KhataContact>,
    allTransactions: List<KhataTransaction>
) {
    val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "All_Customers_Summary_$dateSuffix.txt"
    val prefs = context.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE)
    val shopName = prefs.getString("default_shop_name", "KhataIndex") ?: "KhataIndex"
    val curr = getCurrencySymbol(context)

    val sb = StringBuilder()
    sb.append("====================================================\n")
    sb.append("      $shopName - MASTER CUSTOMERS LEDGER SUMMARY\n")
    sb.append("====================================================\n")
    sb.append("Generated On: ${SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date())}\n")
    sb.append("Total Accounts: ${contacts.size}\n")
    sb.append("----------------------------------------------------\n\n")

    contacts.forEachIndexed { index, contact ->
        val ctx = allTransactions.filter { it.contactId == contact.id }
        val credit = if (contact.type == "SELLER") {
            ctx.filter { it.type == "WE_OWE" }.sumOf { it.amount }
        } else {
            ctx.filter { it.type == "THEY_OWE" }.sumOf { it.amount }
        }
        val paid = if (contact.type == "SELLER") {
            ctx.filter { it.type == "WE_PAID" }.sumOf { it.amount }
        } else {
            ctx.filter { it.type == "THEY_PAID" }.sumOf { it.amount }
        }
        val balance = credit - paid
        val status = if (balance <= 0.0) "PAID / CLEARED ✅" else "PENDING DUES (Due: $curr${String.format("%.2f", balance)}) ⏳"

        sb.append("${index + 1}. ${contact.name} (${if (contact.type == "SELLER") "Wholesaler" else "Customer"})\n")
        sb.append("   Phone: ${if (contact.phone.isNotBlank()) contact.phone else "N/A"}\n")
        sb.append("   Total Credit: $curr${String.format("%.2f", credit)} | Total Paid: $curr${String.format("%.2f", paid)}\n")
        sb.append("   Net Balance : $curr${String.format("%.2f", balance)}\n")
        sb.append("   Bill Status : $status\n")
        sb.append("----------------------------------------------------\n")
    }

    val content = sb.toString()
    var savedSuccessfully = false

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray())
                }
                savedSuccessfully = true
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { stream ->
                stream.write(content.toByteArray())
            }
            savedSuccessfully = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (savedSuccessfully) {
        Toast.makeText(context, "📥 Customers Text Summary saved to Downloads:\n$fileName", Toast.LENGTH_LONG).show()
    } else {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("All Customers Report", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "📥 Customers Report copied to Clipboard!", Toast.LENGTH_LONG).show()
    }
}

// Android Intent Share Trigger helper
fun shareStatement(context: Context, contactName: String, statement: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Ledger Statement for $contactName")
            putExtra(Intent.EXTRA_TEXT, statement)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Send Statement via")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to send bill: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

enum class BillLanguage(val displayName: String, val flag: String) {
    ENGLISH("English", "🇬🇧"),
    HINDI("Hindi (हिंदी)", "🇮🇳"),
    BENGALI("Bengali (বাংলা)", "🇮🇳")
}

fun getLocalizedBillTemplate(
    language: BillLanguage,
    customerName: String,
    amount: Double,
    shopName: String,
    isWholesaler: Boolean,
    isCleared: Boolean = false
): String {
    val formattedAmount = String.format("%.2f", amount)
    return when (language) {
        BillLanguage.ENGLISH -> {
            if (isWholesaler) {
                if (isCleared) {
                    """
                    Hi $customerName,

                    This is to confirm that the wholesale balance of ₹$formattedAmount at $shopName is FULLY PAID & SETTLED. ✅

                    Thank you for your support!
                    """.trimIndent()
                } else {
                    """
                    Hi $customerName,

                    This is to confirm that my pending wholesale balance to you is ₹$formattedAmount for the account at $shopName.

                    I will clear this amount soon.

                    Thank you for your support!
                    """.trimIndent()
                }
            } else {
                if (isCleared) {
                    """
                    Hi $customerName,

                    Your bill of ₹$formattedAmount at $shopName is FULLY PAID & SETTLED. ✅

                    Thank you for your prompt payment!
                    """.trimIndent()
                } else {
                    """
                    Hi $customerName,

                    You have a pending bill of ₹$formattedAmount at $shopName.

                    Please clear this due amount as soon as possible via UPI or cash.

                    Thank you!
                    """.trimIndent()
                }
            }
        }
        
        BillLanguage.HINDI -> {
            if (isWholesaler) {
                if (isCleared) {
                    """
                    नमस्ते $customerName,

                    यह पुष्टि की जाती है कि $shopName के खाते का ₹$formattedAmount का थोक बकाया पूरी तरह से चुका (PAID) दिया गया है। ✅

                    आपके सहयोग के लिए धन्यवाद!
                    """.trimIndent()
                } else {
                    """
                    नमस्ते $customerName,

                    यह पुष्टि करने के लिए है कि $shopName के खाते का मेरा आपके प्रति बकाया ₹$formattedAmount है।

                    मैं जल्द ही इस राशि का भुगतान कर दूँगा।

                    आपके सहयोग के लिए धन्यवाद!
                    """.trimIndent()
                }
            } else {
                if (isCleared) {
                    """
                    नमस्ते $customerName,

                    $shopName पर आपका ₹$formattedAmount का बिल पूरी तरह चुका दिया गया है (PAID)। ✅

                    समय पर भुगतान के लिए धन्यवाद!
                    """.trimIndent()
                } else {
                    """
                    नमस्ते $customerName,

                    आपका $shopName पर ₹$formattedAmount का बिल बकाया (PENDING) है।

                    कृपया जल्द से जल्द इस बकाया राशि का भुगतान करें।

                    धन्यवाद!
                    """.trimIndent()
                }
            }
        }
        
        BillLanguage.BENGALI -> {
            if (isWholesaler) {
                if (isCleared) {
                    """
                    নমস্কার $customerName,

                    এটি নিশ্চিত করার জন্য যে $shopName-এর অ্যাকাউন্টের ₹$formattedAmount টাকার পাইকারি বকেয়া সম্পূর্ণ মিটিয়ে দেওয়া হয়েছে (PAID)। ✅

                    আপনার সহযোগিতার জন্য ধন্যবাদ!
                    """.trimIndent()
                } else {
                    """
                    নমস্কার $customerName,

                    এটি নিশ্চিত করার জন্য যে $shopName-এর অ্যাকাউন্টের জন্য আপনার কাছে আমার ₹$formattedAmount টাকা বকেয়া রয়েছে।

                    আমি শীঘ্রই এই টাকাটি মিটিয়ে দেব।

                    আপনার সহযোগিতার জন্য ধন্যবাদ!
                    """.trimIndent()
                }
            } else {
                if (isCleared) {
                    """
                    নমস্কার $customerName,

                    $shopName-এ আপনার ₹$formattedAmount টাকার বিল সম্পূর্ণ মিটিয়ে দেওয়া হয়েছে (PAID)। ✅

                    ধন্যবাদ!
                    """.trimIndent()
                } else {
                    """
                    নমস্কার $customerName,

                    $shopName-এ আপনার ₹$formattedAmount টাকার বিল বকেয়া (PENDING) আছে।

                    দয়া করে এই বকেয়া টাকাটি যত তাড়াতাড়ি সম্ভব মিটিয়ে দিন।

                    ধন্যবাদ!
                    """.trimIndent()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SendBillDialog(
    contact: KhataContact,
    defaultAmount: Double,
    theme: TaskCardTheme,
    onDismiss: () -> Unit,
    onShare: (message: String) -> Unit,
    onCopy: (message: String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("khatabook_prefs", android.content.Context.MODE_PRIVATE) }
    val savedShopName = remember { prefs.getString("default_shop_name", "KhataIndex") ?: "KhataIndex" }
    val savedLangStr = remember { prefs.getString("default_lang", "ENGLISH") ?: "ENGLISH" }
    val savedLang = remember {
        try { BillLanguage.valueOf(savedLangStr) } catch(e: Exception) { BillLanguage.ENGLISH }
    }

    var shopName by remember { mutableStateOf(savedShopName) }
    var customAmount by remember { mutableStateOf(String.format("%.2f", defaultAmount)) }
    var selectedLanguage by remember { mutableStateOf(savedLang) }
    var isCleared by remember { mutableStateOf(defaultAmount <= 0.0) }

    val themeColor = theme.primary()

    val currentAmountDouble = customAmount.toDoubleOrNull() ?: 0.0
    val finalMessage = remember(selectedLanguage, contact.name, currentAmountDouble, shopName, isCleared) {
        getLocalizedBillTemplate(
            language = selectedLanguage,
            customerName = contact.name,
            amount = currentAmountDouble,
            shopName = shopName,
            isWholesaler = contact.type == "SELLER",
            isCleared = isCleared
        )
    }

    val isDark = LocalIsDark.current
    val surfaceColor = if (isDark) Color(0xFF1D1B22) else Color.White
    val textMain = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSecondary = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
    val inputBg = if (isDark) Color(0xFF2E2A36) else Color.White
    val borderStrokeColor = if (isDark) Color(0xFF49454F) else Color.LightGray.copy(alpha = 0.5f)
    val borderFieldColor = if (isDark) Color(0xFF49454F) else Color(0xFF79747E)
    val previewBg = if (isDark) Color(0xFF2E2A36) else Color(0xFFF5F5F5)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✉️ Send Bill Statement",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = textMain
                        )
                        Text(
                            text = "Recipient: ${contact.name}",
                            fontSize = 12.sp,
                            color = themeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Language selection chips
                Text(
                    text = "Select Language",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BillLanguage.values().forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) themeColor.copy(alpha = 0.18f)
                                    else if (isDark) Color(0xFF2E2A36) else Color(0xFFF3EDF7)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) themeColor else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedLanguage = lang }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${lang.flag} ${lang.displayName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) themeColor else textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Type Selection
                Text(
                    text = "Statement Mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(false, true).forEach { cleared ->
                        val label = if (cleared) "Paid / Cleared ✅" else "Pending Due ⏳"
                        val isSelected = isCleared == cleared
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) themeColor.copy(alpha = 0.18f)
                                    else if (isDark) Color(0xFF2E2A36) else Color(0xFFF3EDF7)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) themeColor else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { isCleared = cleared }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) themeColor else textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input field: Shop Name
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop / Business Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = textSecondary,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = borderFieldColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        cursorColor = themeColor
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Input field: Bill Amount
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = { customAmount = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = textSecondary,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = borderFieldColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        cursorColor = themeColor
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Message Preview
                Text(
                    text = "Message Preview",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(previewBg, RoundedCornerShape(12.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = finalMessage,
                        fontSize = 13.sp,
                        color = textMain,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onCopy(finalMessage) },
                        colors = ButtonDefaults.buttonColors(containerColor = previewBg, contentColor = themeColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onShare(finalMessage) },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send / Share", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KhataSettingsScreen(
    viewModel: TodoViewModel,
    theme: TaskCardTheme,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE) }

    var shopName by remember { mutableStateOf(prefs.getString("default_shop_name", "KhataIndex") ?: "KhataIndex") }
    var shopPhone by remember { mutableStateOf(prefs.getString("shop_phone", "") ?: "") }
    var shopUpi by remember { mutableStateOf(prefs.getString("shop_upi_id", "") ?: "") }

    var currencySymbol by remember { mutableStateOf(prefs.getString("currency_symbol", "₹") ?: "₹") }
    var showDecimals by remember { mutableStateOf(prefs.getBoolean("show_decimals", true)) }

    var defaultLangStr by remember { mutableStateOf(prefs.getString("default_lang", "ENGLISH") ?: "ENGLISH") }
    var billTagline by remember { mutableStateOf(prefs.getString("bill_tagline", "Thank you for doing business with us!") ?: "Thank you for doing business with us!") }

    var showLedgerSummary by remember { mutableStateOf(prefs.getBoolean("show_ledger_summary", true)) }

    var showConfirmClear by remember { mutableStateOf(false) }

    val settingsFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            importCustomersFromFile(context, it, viewModel, "CUSTOMER")
        }
    }

    val themeColor = theme.primary()
    val isDark = LocalIsDark.current

    val surfaceColor = if (isDark) Color(0xFF141218) else Color(0xFFFEF7FF)
    val cardBg = if (isDark) Color(0xFF1D1B22) else Color(0xFFFFFFFF)
    val textMain = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSecondary = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
    val inputBg = if (isDark) Color(0xFF2E2A3A) else Color.White
    val borderStrokeColor = (if (isDark) Color(0xFF49454F) else Color(0xFFCAC4D0)).copy(alpha = 0.4f)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Navigation Header
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
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Ledger",
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Khata Ledger Settings",
                        color = textMain,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Configure store profile, currency, WhatsApp messaging & reports",
                        color = textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {

                // Section 1: Shop & Business Profile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Shop & Contact Identity",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = shopName,
                            onValueChange = {
                                shopName = it
                                prefs.edit().putString("default_shop_name", it).apply()
                            },
                            label = { Text("Shop / Business Name", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedLabelColor = themeColor,
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = borderStrokeColor,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                cursorColor = themeColor
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = shopPhone,
                            onValueChange = {
                                shopPhone = it
                                prefs.edit().putString("shop_phone", it).apply()
                            },
                            label = { Text("Store Owner Phone (Optional)", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedLabelColor = themeColor,
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = borderStrokeColor,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                cursorColor = themeColor
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = shopUpi,
                            onValueChange = {
                                shopUpi = it
                                prefs.edit().putString("shop_upi_id", it).apply()
                            },
                            label = { Text("Shop UPI Payment Handle (e.g. store@upi)", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedLabelColor = themeColor,
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = borderStrokeColor,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                cursorColor = themeColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Currency & Formatting
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Currency & Display Precision",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Select Currency Symbol",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val currencies = listOf("₹", "$", "€", "£", "৳", "¥", "AED", "SAR")
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currencies.forEach { symbol ->
                                val isSelected = currencySymbol == symbol
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) themeColor.copy(alpha = 0.25f)
                                            else if (isDark) Color(0xFF2E2A3A) else Color.White
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) themeColor else borderStrokeColor.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            currencySymbol = symbol
                                            prefs.edit().putString("currency_symbol", symbol).apply()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = symbol,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) themeColor else textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show Decimal Amounts (.00)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textMain
                                )
                                Text(
                                    text = "Display paise or cents accuracy",
                                    fontSize = 10.sp,
                                    color = textSecondary
                                )
                            }
                            Switch(
                                checked = showDecimals,
                                onCheckedChange = {
                                    showDecimals = it
                                    prefs.edit().putBoolean("show_decimals", it).apply()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: WhatsApp Invoicing & Multilingual Messaging
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WhatsApp Invoicing & Tagline",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Default Messaging Language",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BillLanguage.values().forEach { lang ->
                                val isSelected = defaultLangStr == lang.name
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) themeColor.copy(alpha = 0.25f)
                                            else if (isDark) Color(0xFF2E2A3A) else Color.White
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) themeColor else borderStrokeColor.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            defaultLangStr = lang.name
                                            prefs.edit().putString("default_lang", lang.name).apply()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${lang.flag} ${lang.displayName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) themeColor else textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = billTagline,
                            onValueChange = {
                                billTagline = it
                                prefs.edit().putString("bill_tagline", it).apply()
                            },
                            label = { Text("Bill Footer Tagline", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedLabelColor = themeColor,
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = borderStrokeColor,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                cursorColor = themeColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 4: DASHBOARD DISPLAY PREFERENCES
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dashboard Net Position Header",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textMain
                                )
                                Text(
                                    text = "Show overall ledger summary card at top of dashboard",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                            Switch(
                                checked = showLedgerSummary,
                                onCheckedChange = {
                                    showLedgerSummary = it
                                    prefs.edit().putBoolean("show_ledger_summary", it).apply()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = themeColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 5: Data Export & Reset Data
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export & Data Management",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Button 0: Upload Customers Info (CSV / TXT)
                        Button(
                            onClick = {
                                settingsFilePickerLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor.copy(alpha = 0.15f),
                                contentColor = themeColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp), tint = themeColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Customers Info (CSV / TXT File)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button 1: Download All Customers CSV
                        Button(
                            onClick = {
                                val allContacts = viewModel.khataCustomers.value + viewModel.khataSellers.value
                                val allTx = viewModel.allKhataTransactions.value
                                downloadAllCustomersCsv(context, allContacts, allTx)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download All Customers Info (CSV File)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button 2: Download All Customers Text Info
                        OutlinedButton(
                            onClick = {
                                val allContacts = viewModel.khataCustomers.value + viewModel.khataSellers.value
                                val allTx = viewModel.allKhataTransactions.value
                                downloadAllCustomersTextReport(context, allContacts, allTx)
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = themeColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download All Customers Info (Text File)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button 3: Share Full Text Statement
                        OutlinedButton(
                            onClick = {
                                val sellersList = viewModel.khataSellers.value
                                val customersList = viewModel.khataCustomers.value
                                val txList = viewModel.allKhataTransactions.value

                                val summaryBuilder = StringBuilder()
                                summaryBuilder.append("==============================\n")
                                summaryBuilder.append("    $shopName FULL LEDGER REPORT\n")
                                summaryBuilder.append("==============================\n")
                                summaryBuilder.append("Date: ${SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date())}\n\n")

                                summaryBuilder.append("🏢 WHOLESALERS (${sellersList.size}):\n")
                                sellersList.forEach { s ->
                                    val ctx = txList.filter { it.contactId == s.id }
                                    val owe = ctx.filter { it.type == "WE_OWE" }.sumOf { it.amount }
                                    val paid = ctx.filter { it.type == "WE_PAID" }.sumOf { it.amount }
                                    summaryBuilder.append("- ${s.name}: We Owe ${formatKhataCurrency(owe - paid, context)}\n")
                                }

                                summaryBuilder.append("\n🛍️ CUSTOMERS (${customersList.size}):\n")
                                customersList.forEach { c ->
                                    val ctx = txList.filter { it.contactId == c.id }
                                    val owe = ctx.filter { it.type == "THEY_OWE" }.sumOf { it.amount }
                                    val paid = ctx.filter { it.type == "THEY_PAID" }.sumOf { it.amount }
                                    summaryBuilder.append("- ${c.name}: They Owe ${formatKhataCurrency(owe - paid, context)}\n")
                                }

                                summaryBuilder.append("\n==============================\n")
                                summaryBuilder.append("$billTagline\n")

                                val textReport = summaryBuilder.toString()
                                clipboardManager.setText(AnnotatedString(textReport))
                                shareStatement(context, shopName, textReport)
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textMain),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = textMain)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share & Copy Full Ledger Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMain)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 6: Danger Zone
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF2D1717) else Color(0xFFFFF5F5)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF5E2B2B) else Color(0xFFFFCCCC)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Danger Zone",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Resetting clears all customer entries, wholesalers, and ledger logs permanently.",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF665555),
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (!showConfirmClear) {
                            Button(
                                onClick = { showConfirmClear = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset Ledger Book Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isDark) Color(0xFF4C1D1D) else Color(0xFFFFEBEE),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0xFF752424) else Color(0xFFEF9A9A),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Are you absolutely sure?",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFF8B8B) else Color(0xFFC62828),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showConfirmClear = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = surfaceColor,
                                            contentColor = textMain
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel", fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.clearAllKhataData()
                                            showConfirmClear = false
                                            Toast.makeText(context, "Ledger Book cleared successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFC62828),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text("Yes, Clear All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save & Back to Dashboard", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun GramPriceCalculator(theme: TaskCardTheme) {
    var baseWeight by remember { mutableStateOf("100") }
    var baseWeightUnit by remember { mutableStateOf("g") } // "g" or "kg"
    var basePrice by remember { mutableStateOf("50") }
    var targetWeight by remember { mutableStateOf("20") }
    var targetWeightUnit by remember { mutableStateOf("g") } // "g" or "kg"

    val isDark = LocalIsDark.current
    val cardBg = if (isDark) Color(0xFF211D2A) else Color(0xFFF9F6FC)
    val textMain = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSecondary = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
    val inputBg = if (isDark) Color(0xFF2E2A36) else Color.White
    val borderColor = if (isDark) Color(0xFF49454F) else Color(0xFFE1DDF3)

    // Parse values and calculate cost
    val calculatedCost: Double? = remember(baseWeight, baseWeightUnit, basePrice, targetWeight, targetWeightUnit) {
        try {
            val bw = baseWeight.toDoubleOrNull() ?: 0.0
            val bp = basePrice.toDoubleOrNull() ?: 0.0
            val tw = targetWeight.toDoubleOrNull() ?: 0.0

            if (bw <= 0.0 || bp < 0.0 || tw < 0.0) {
                null
            } else {
                val bwInBase = if (baseWeightUnit == "kg") bw * 1000.0 else bw
                val twInBase = if (targetWeightUnit == "kg") tw * 1000.0 else tw
                (bp / bwInBase) * twInBase
            }
        } catch (e: Exception) {
            null
        }
    }

    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Calculator",
                        tint = theme.primary(),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "⚖️ Simple Gram & Cost Calculator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = theme.primary()
                        )
                        Text(
                            text = "Tap to calculate how much custom grams cost (e.g., Sugar/Rice)",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    // Help Presets Row
                    Text(
                        text = "Step 1: Tap a quick example to learn how it works:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.primary(),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Presets horizontal scroll row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            Triple("Sugar example (100g = ₹50)", "100", "50"),
                            Triple("Rice example (1kg = ₹90)", "1", "90"),
                            Triple("Spices example (50g = ₹35)", "50", "35"),
                            Triple("Tea example (250g = ₹120)", "250", "120")
                        )
                        presets.forEach { (label, presetWeight, presetPrice) ->
                            val unitVal = if (label.contains("1kg")) "kg" else "g"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(theme.primary().copy(alpha = 0.08f))
                                    .border(1.dp, theme.primary().copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        baseWeight = presetWeight
                                        baseWeightUnit = unitVal
                                        basePrice = presetPrice
                                        targetWeight = if (unitVal == "kg") "0.5" else "20"
                                        targetWeightUnit = if (unitVal == "kg") "kg" else "g"
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primary()
                                )
                            }
                        }
                    }

                    // Base Pricing Inputs
                    Text(
                        text = "Step 2: Enter known pricing (e.g. 100g costs ₹50):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Base weight field
                        OutlinedTextField(
                            value = baseWeight,
                            onValueChange = { baseWeight = it },
                            label = { Text("Weight", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedLabelColor = theme.primary(),
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = theme.primary(),
                                unfocusedBorderColor = borderColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )

                        // Base unit select (g / kg toggle button)
                        Button(
                            onClick = { baseWeightUnit = if (baseWeightUnit == "g") "kg" else "g" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primary().copy(alpha = 0.12f),
                                contentColor = theme.primary()
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(56.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(baseWeightUnit, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        }

                        // Label "costs"
                        Text(
                            text = "costs",
                            fontSize = 11.sp,
                            color = textSecondary,
                            modifier = Modifier.weight(0.6f),
                            textAlign = TextAlign.Center
                        )

                        // Base price field
                        OutlinedTextField(
                            value = basePrice,
                            onValueChange = { basePrice = it },
                            label = { Text("Price (₹)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedLabelColor = theme.primary(),
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = theme.primary(),
                                unfocusedBorderColor = borderColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Target Weight input
                    Text(
                        text = "Step 3: Enter the custom weight you want to buy:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick minus button
                        IconButton(
                            onClick = {
                                val current = targetWeight.toDoubleOrNull() ?: 0.0
                                val step = if (targetWeightUnit == "kg") 0.1 else 10.0
                                if (current > step) {
                                    targetWeight = if (targetWeightUnit == "kg") {
                                        "%.1f".format(current - step)
                                    } else {
                                        "%.0f".format(current - step)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.primary().copy(alpha = 0.08f))
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, color = theme.primary(), fontSize = 18.sp)
                        }

                        // Target weight input field
                        OutlinedTextField(
                            value = targetWeight,
                            onValueChange = { targetWeight = it },
                            label = { Text("Buy Weight", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedLabelColor = theme.primary(),
                                unfocusedLabelColor = textSecondary,
                                focusedBorderColor = theme.primary(),
                                unfocusedBorderColor = borderColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )

                        // Quick plus button
                        IconButton(
                            onClick = {
                                val current = targetWeight.toDoubleOrNull() ?: 0.0
                                val step = if (targetWeightUnit == "kg") 0.1 else 10.0
                                targetWeight = if (targetWeightUnit == "kg") {
                                    "%.1f".format(current + step)
                                } else {
                                    "%.0f".format(current + step)
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.primary().copy(alpha = 0.08f))
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, color = theme.primary(), fontSize = 18.sp)
                        }

                        // Target unit select
                        Button(
                            onClick = { targetWeightUnit = if (targetWeightUnit == "g") "kg" else "g" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.primary().copy(alpha = 0.12f),
                                contentColor = theme.primary()
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(targetWeightUnit, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target Quick Preset Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val targets = if (targetWeightUnit == "kg") {
                            listOf("0.1", "0.25", "0.5", "1", "2")
                        } else {
                            listOf("10", "20", "50", "100", "250", "500")
                        }
                        targets.forEach { value ->
                            val isSel = targetWeight == value
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) theme.primary() else theme.primary().copy(alpha = 0.05f))
                                    .clickable { targetWeight = value }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$value$targetWeightUnit",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else theme.primary()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Final Conversational Calculation Result panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.primary().copy(alpha = 0.12f))
                            .border(1.5.dp, theme.primary().copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Result Info",
                                        tint = theme.primary(),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Answer",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primary()
                                    )
                                }

                                Text(
                                    text = "₹${"%.2f".format(calculatedCost ?: 0.0)}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = theme.primary()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (calculatedCost != null) {
                                    "✨ If $baseWeight$baseWeightUnit costs ₹$basePrice, then $targetWeight$targetWeightUnit will cost exactly ₹${"%.2f".format(calculatedCost)}."
                                } else {
                                    "⚠️ Please enter a valid weight and cost to calculate the price."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMain,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
