package com.example

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
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

@Composable
fun KhataBookDashboard(
    viewModel: TodoViewModel,
    theme: TaskCardTheme
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Wholesalers (Sellers), 1 = Customers (Consumers)
    var selectedContact by remember { mutableStateOf<KhataContact?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showKhataSettingsDialog by remember { mutableStateOf(false) }
    var khataSearchQuery by remember { mutableStateOf("") }

    val sellers by viewModel.khataSellers.collectAsState()
    val customers by viewModel.khataCustomers.collectAsState()
    val allTransactions by viewModel.allKhataTransactions.collectAsState()

    val themeColor = theme.primary()
    val containerBg = theme.container()

    AnimatedContent(
        targetState = selectedContact,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "KhataNavigation"
    ) { contact ->
        if (contact != null) {
            // Contact Details and Ledgers View
            ContactDetailsScreen(
                contact = contact,
                viewModel = viewModel,
                theme = theme,
                onBack = { selectedContact = null }
            )
        } else {
            // Main Dashboard List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Header Segment
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = containerBg.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon),
                            contentDescription = "KhataIndex Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Khata Ledger Book",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
                            )
                            Text(
                                text = "Track business transactions, credits, and payments securely offline.",
                                fontSize = 11.sp,
                                color = if (LocalIsDark.current) Color(0xFFCAC4D0) else Color(0xFF49454F)
                            )
                        }
                        IconButton(
                            onClick = { showKhataSettingsDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Khata Settings",
                                tint = themeColor
                            )
                        }
                    }
                }

                if (showKhataSettingsDialog) {
                    KhataSettingsDialog(
                        viewModel = viewModel,
                        theme = theme,
                        onDismiss = { showKhataSettingsDialog = false }
                    )
                }

                // Tab Switcher for Sellers vs Customers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) themeColor else Color.Transparent)
                            .clickable { 
                                selectedTab = 0
                                khataSearchQuery = ""
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🏢 Wholesalers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 0) Color.White else (if (LocalIsDark.current) Color(0xFFCAC4D0) else Color(0xFF49454F))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) themeColor else Color.Transparent)
                            .clickable { 
                                selectedTab = 1
                                khataSearchQuery = ""
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🛍️ Customers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 1) Color.White else (if (LocalIsDark.current) Color(0xFFCAC4D0) else Color(0xFF49454F))
                        )
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
    val themeColor = theme.primary()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (LocalIsDark.current) Color(0xFF211D2A) else Color.White),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (LocalIsDark.current) Color(0xFF49454F) else Color.LightGray.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = themeColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(10.dp)
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
                    text = String.format("₹%.2f", balance),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
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

        // Outstanding Balance Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = containerBg.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                    text = String.format("₹%.2f", balance),
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = if (balance > 0) Color(0xFFD32F2F) else Color(0xFF388E3C)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Send Bill & Copy ledger text row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val billText = generateBillText(contact, transactions, balance)
                            clipboardManager.setText(AnnotatedString(billText))
                            Toast.makeText(context, "Statement copied to Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = themeColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Statement", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showSendBillDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Bill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.25f))
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
fun generateBillText(contact: KhataContact, transactions: List<KhataTransaction>, balance: Double): String {
    val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    val sb = StringBuilder()
    sb.append("-----------------------------\n")
    sb.append("   KHATA LEDGER STATEMENT    \n")
    sb.append("-----------------------------\n")
    sb.append("Party Name: ${contact.name}\n")
    if (contact.phone.isNotBlank()) {
        sb.append("Phone: ${contact.phone}\n")
    }
    sb.append("Account Type: ${if (contact.type == "SELLER") "Wholesaler (Seller)" else "Customer (Buyer)"}\n")
    sb.append("Statement Date: ${sdf.format(Date())}\n")
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
            sb.append("$dateStr: $desc $sign ₹${tx.amount}\n")
        }
    }
    
    sb.append("-----------------------------\n")
    if (contact.type == "SELLER") {
        sb.append(String.format("NET AMOUNT WE OWE: ₹%.2f\n", balance))
    } else {
        sb.append(String.format("NET AMOUNT THEY OWE US: ₹%.2f\n", balance))
    }
    sb.append("-----------------------------\n")
    sb.append("Thank you for choosing Secure Planner Pro Ledger.\n")
    return sb.toString()
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
                    This is to confirm that pending balance of ₹$formattedAmount in the account at $shopName have been cleared.

                    Thank you for your support!
                    """.trimIndent()
                } else {
                    """
                    Hi $customerName,
                    This is to confirm that my pending balance to you is ₹$formattedAmount for the account at $shopName.

                    I will clear this amount soon.

                    Thank you for your support!
                    """.trimIndent()
                }
            } else {
                if (isCleared) {
                    """
                    Hi $customerName,
                    the pending balance of ₹$formattedAmount at $shopName.
                    have been cleared.
                    Thank you!
                    """.trimIndent()
                } else {
                    """
                    Hi $customerName,
                    You have a pending balance of ₹$formattedAmount at $shopName.

                    Please clear this due amount as soon as possible.

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
                    यह पुष्टि करने के लिए है कि $shopName के खाते का ₹$formattedAmount का बकाया चुका दिया गया है।

                    आपके सहयोग के लिए धन्यवाद!
                    """.trimIndent()
                } else {
                    """
                    नमस्ते $customerName,
                    यह पुष्टि करने के लिए है कि $shopName के खाते के लिए मेरा आपके प्रति बकाया ₹$formattedAmount है।

                    मैं जल्द ही इस राशि का भुगतान कर दूँगा।

                    आपके सहयोग के लिए धन्यवाद!
                    """.trimIndent()
                }
            } else {
                if (isCleared) {
                    """
                    नमस्ते $customerName,
                    $shopName पर ₹$formattedAmount का आपका बकाया पूरा चुका दिया गया है।

                    धन्यवाद!
                    """.trimIndent()
                } else {
                    """
                    नमस्ते $customerName,
                    आपका $shopName पर ₹$formattedAmount का बकाया है।

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
                    এটি নিশ্চিত করার জন্য যে $shopName-এর অ্যাকাউন্টের ₹$formattedAmount টাকার বকেয়া মিটিয়ে দেওয়া হয়েছে।

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
                    $shopName-এ আপনার ₹$formattedAmount টাকার বকেয়া সম্পূর্ণ মিটিয়ে দেওয়া হয়েছে।

                    ধন্যবাদ!
                    """.trimIndent()
                } else {
                    """
                    নমস্কার $customerName,
                    $shopName-এ আপনার ₹$formattedAmount টাকা বাকি আছে।

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
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "✉️ Send Multilingual Bill",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textMain
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Language selection chips
                Text(
                    text = "Select Message Language",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BillLanguage.values().forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) themeColor.copy(alpha = 0.15f)
                                    else if (isDark) Color(0xFF2E2A36) else Color(0xFFF3EDF7)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) themeColor else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedLanguage = lang }
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

                Spacer(modifier = Modifier.height(14.dp))

                // Status Type Selection
                Text(
                    text = "Select Statement Type",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(false, true).forEach { cleared ->
                        val label = if (cleared) "Paid / Cleared ✅" else "Pending Balance ⏳"
                        val isSelected = isCleared == cleared
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) themeColor.copy(alpha = 0.15f)
                                    else if (isDark) Color(0xFF2E2A36) else Color(0xFFF3EDF7)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) themeColor else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { isCleared = cleared }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
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
                    label = { Text("Shop / App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedLabelColor = themeColor,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = borderFieldColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Input field: Bill Amount
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = { customAmount = it },
                    label = { Text("Bill Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedLabelColor = themeColor,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = borderFieldColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Message Preview
                Text(
                    text = "Message Preview",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(previewBg, RoundedCornerShape(8.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = finalMessage,
                        fontSize = 12.sp,
                        color = textMain,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = textSecondary)
                    }
                    Button(
                        onClick = { onCopy(finalMessage) },
                        colors = ButtonDefaults.buttonColors(containerColor = surfaceColor, contentColor = themeColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onShare(finalMessage) },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Send / Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KhataSettingsDialog(
    viewModel: TodoViewModel,
    theme: TaskCardTheme,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("khatabook_prefs", android.content.Context.MODE_PRIVATE) }
    var defaultShopName by remember { mutableStateOf(prefs.getString("default_shop_name", "KhataIndex") ?: "KhataIndex") }
    var defaultLangStr by remember { mutableStateOf(prefs.getString("default_lang", "ENGLISH") ?: "ENGLISH") }
    var showConfirmClear by remember { mutableStateOf(false) }

    val themeColor = theme.primary()

    val isDark = LocalIsDark.current
    val surfaceColor = if (isDark) Color(0xFF1D1B22) else Color.White
    val textMain = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSecondary = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
    val inputBg = if (isDark) Color(0xFF2E2A36) else Color(0xFFF9F9FA)
    val cardBg = if (isDark) Color(0xFF25232A) else Color(0xFFF3F2F5)
    val borderStrokeColor = if (isDark) Color(0xFF49454F) else Color.LightGray.copy(alpha = 0.5f)
    val borderFieldColor = if (isDark) Color(0xFF49454F) else Color(0xFF79747E)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(themeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ledger Settings",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = textMain
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                // Section 1: Shop Profile Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Shop Identity",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = defaultShopName,
                            onValueChange = { 
                                defaultShopName = it
                                prefs.edit().putString("default_shop_name", it).apply()
                            },
                            placeholder = { Text("e.g. Roy's Store", fontSize = 13.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedLabelColor = themeColor,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = borderFieldColor.copy(alpha = 0.5f),
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Localization Preferences
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Default Messaging Language",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
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
                                            if (isSelected) themeColor.copy(alpha = 0.2f)
                                            else if (isDark) Color(0xFF2E2A36) else Color.White
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
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Danger Zone
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF2D1717) else Color(0xFFFFF5F5)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF5E2B2B) else Color(0xFFFFCCCC)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                            text = "Clearing the ledger book clears all customers, wholesalers, and transactions forever.",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF665555),
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save & Close Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
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
