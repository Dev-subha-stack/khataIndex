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

    val themeColor = Color(theme.primaryColor)
    val containerBg = Color(theme.containerColor)

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
                                color = Color(0xFF1D1B20)
                            )
                            Text(
                                text = "Track business transactions, credits, and payments securely offline.",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F)
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
                            color = if (selectedTab == 0) Color.White else Color(0xFF49454F)
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
                            color = if (selectedTab == 1) Color.White else Color(0xFF49454F)
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
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color(0xFFCAC4D0).copy(alpha = 0.8f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
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
    val themeColor = Color(theme.primaryColor)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.35f))
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
                    color = Color(0xFF1D1B20),
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

    val themeColor = Color(theme.primaryColor)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (type == "SELLER") "Add New Wholesaler" else "Add New Customer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1D1B20)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = Color(0xFF49454F),
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
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
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = Color(0xFF49454F),
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
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
    val themeColor = Color(theme.primaryColor)
    val containerBg = Color(theme.containerColor)

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
                    color = Color(0xFF1D1B20)
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
                color = Color(0xFF1D1B20)
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
    val themeColor = Color(theme.primaryColor)
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
                        color = Color(0xFF1D1B20)
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

    val themeColor = Color(theme.primaryColor)

    val dialogTitle = when (txType) {
        "THEY_OWE" -> "Gave Goods on Credit (Udhaar)"
        "THEY_PAID" -> "Received Payment (Cash Got)"
        "WE_OWE" -> "Took Goods on Credit (Udhaar)"
        "WE_PAID" -> "Paid Money (Cash Settled)"
        else -> "New Entry"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1D1B20)
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
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = Color(0xFF49454F),
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
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
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedLabelColor = themeColor,
                        unfocusedLabelColor = Color(0xFF49454F),
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
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
    BENGALI("Bengali (বাংলা)", "🇮🇳"),
    SPANISH("Spanish (Español)", "🇪🇸"),
    FRENCH("French (Français)", "🇫🇷"),
    ARABIC("Arabic (العربية)", "🇸🇦")
}

fun getLocalizedBillTemplate(
    language: BillLanguage,
    customerName: String,
    amount: Double,
    shopName: String
): String {
    val formattedAmount = String.format("%.2f", amount)
    return when (language) {
        BillLanguage.ENGLISH -> """
Hi $customerName,
Your bill of ₹$formattedAmount has been generated at $shopName.

Thank you!
        """.trimIndent()
        
        BillLanguage.HINDI -> """
नमस्ते $customerName,
आपका ₹$formattedAmount का बिल $shopName पर जनरेट हो गया है।

धन्यवाद!
        """.trimIndent()
        
        BillLanguage.BENGALI -> """
নমস্কার $customerName,
আপনার ₹$formattedAmount টাকার বিল $shopName-এ তৈরি হয়েছে।

ধন্যবাদ!
        """.trimIndent()
        
        BillLanguage.SPANISH -> """
Hola $customerName,
Tu factura de ₹$formattedAmount ha sido generada en $shopName.

¡Gracias!
        """.trimIndent()
        
        BillLanguage.FRENCH -> """
Bonjour $customerName,
Votre facture de ₹$formattedAmount a été générée chez $shopName.

Merci !
        """.trimIndent()
        
        BillLanguage.ARABIC -> """
مرحباً $customerName،
تم إصدار فاتورتك بقيمة ₹$formattedAmount في $shopName.

شكراً لك!
        """.trimIndent()
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

    val themeColor = Color(theme.primaryColor)

    val currentAmountDouble = customAmount.toDoubleOrNull() ?: 0.0
    val finalMessage = remember(selectedLanguage, contact.name, currentAmountDouble, shopName) {
        getLocalizedBillTemplate(
            language = selectedLanguage,
            customerName = contact.name,
            amount = currentAmountDouble,
            shopName = shopName
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
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
                    color = Color(0xFF1D1B20)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Language selection chips
                Text(
                    text = "Select Message Language",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
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
                                    else Color(0xFFF3EDF7)
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
                                color = if (isSelected) themeColor else Color(0xFF49454F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input field: Shop Name
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop / App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedLabelColor = themeColor,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
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
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedLabelColor = themeColor,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Message Preview
                Text(
                    text = "Message Preview",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = finalMessage,
                        fontSize = 12.sp,
                        color = Color(0xFF1D1B20),
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
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = { onCopy(finalMessage) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = themeColor),
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

    val themeColor = Color(theme.primaryColor)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ Ledger Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1D1B20)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Default Shop Name setting
                Text(
                    text = "Default Shop / App Name",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = defaultShopName,
                    onValueChange = { 
                        defaultShopName = it
                        prefs.edit().putString("default_shop_name", it).apply()
                    },
                    placeholder = { Text("e.g. Roy's Store") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedLabelColor = themeColor,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = Color(0xFF79747E),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Default Billing Language setting
                Text(
                    text = "Default Message Language",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BillLanguage.values().forEach { lang ->
                        val isSelected = defaultLangStr == lang.name
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
                                color = if (isSelected) themeColor else Color(0xFF49454F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.LightGray.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.height(16.dp))

                // Reset and Clear data
                Text(
                    text = "⚠️ Danger Zone",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deleting all Khata records is permanent and cannot be undone.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!showConfirmClear) {
                    Button(
                        onClick = { showConfirmClear = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFC62828)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A))
                    ) {
                        Text("Reset Ledger Book Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFEF9A9A), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Are you absolutely sure?",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showConfirmClear = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                                shape = RoundedCornerShape(6.dp),
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Yes, Clear All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save & Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
