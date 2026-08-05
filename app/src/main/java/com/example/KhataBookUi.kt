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
import androidx.compose.ui.unit.*
import coil.compose.rememberAsyncImagePainter
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
import androidx.compose.ui.window.DialogProperties
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
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDark.current
    val prefs = LocalContext.current.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE)
    val glassMode = prefs.getBoolean("glass_mode", true)

    val glassBgBrush = if (glassMode) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2C263E).copy(alpha = 0.65f),
                    Color(0xFF191525).copy(alpha = 0.80f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color(0xFFF3EEFF).copy(alpha = 0.68f)
                )
            )
        }
    } else {
        if (isDark) SolidColor(Color(0xFF211D2A)) else SolidColor(Color.White)
    }

    val glassBorderBrush = if (glassMode) {
        Brush.linearGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.50f),
                themeColor.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.15f)
            ) else listOf(
                Color.White.copy(alpha = 0.95f),
                themeColor.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.60f)
            )
        )
    } else {
        SolidColor(if (isDark) Color(0xFF383344) else Color(0xFFE1DDF3))
    }

    val shimmerHighlight = Brush.linearGradient(
        colors = if (isDark) listOf(
            Color.White.copy(alpha = 0.10f),
            Color.Transparent,
            Color.White.copy(alpha = 0.03f)
        ) else listOf(
            Color.White.copy(alpha = 0.40f),
            Color.Transparent,
            Color.White.copy(alpha = 0.15f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassBgBrush, shape)
            .border(1.2.dp, glassBorderBrush, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
    ) {
        if (glassMode) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(shimmerHighlight, shape)
            )
        }

        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

data class PresetAvatar(
    val id: String,
    val name: String,
    val emoji: String,
    val category: String, // "WHOLESALER", "CUSTOMER", or "ALL"
    val gradientColors: List<Color>
)

val PRESET_AVATARS = listOf(
    PresetAvatar("preset_wholesaler_suit", "Wholesale Executive", "👔", "WHOLESALER", listOf(Color(0xFF3F51B5), Color(0xFF1A237E))),
    PresetAvatar("preset_shopkeeper", "Main Storekeeper", "🏬", "WHOLESALER", listOf(Color(0xFFFF9800), Color(0xFFE65100))),
    PresetAvatar("preset_bulk_supplier", "Bulk Supplier", "📦", "WHOLESALER", listOf(Color(0xFF009688), Color(0xFF004D40))),
    PresetAvatar("preset_factory_dealer", "Factory Distributor", "🏭", "WHOLESALER", listOf(Color(0xFF607D8B), Color(0xFF263238))),
    PresetAvatar("preset_transport", "Logistics Agent", "🚚", "WHOLESALER", listOf(Color(0xFF795548), Color(0xFF3E2723))),

    PresetAvatar("preset_customer_user", "Retail Customer", "👤", "CUSTOMER", listOf(Color(0xFF2196F3), Color(0xFF0D47A1))),
    PresetAvatar("preset_regular_buyer", "Regular Buyer", "🛒", "CUSTOMER", listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))),
    PresetAvatar("preset_vip_client", "VIP Client", "👑", "CUSTOMER", listOf(Color(0xFFFFD700), Color(0xFFFF8C00))),
    PresetAvatar("preset_loyal_shopper", "Loyal Shopper", "🛍️", "CUSTOMER", listOf(Color(0xFFE91E63), Color(0xFF880E4F))),
    PresetAvatar("preset_credit_account", "Credit Account", "💳", "CUSTOMER", listOf(Color(0xFF9C27B0), Color(0xFF4A148C)))
)

@Composable
fun GlassContactAvatar(
    contactName: String,
    avatarUri: String,
    size: Dp = 48.dp,
    themeColor: Color = MaterialTheme.colorScheme.primary,
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDark.current
    val initial = contactName.trim().take(1).ifEmpty { "?" }.uppercase()
    val preset = PRESET_AVATARS.find { it.id == avatarUri }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing glass border ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = maxOf(1.5f, size.value * 0.035f).dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.9f),
                            themeColor.copy(alpha = 0.8f),
                            Color.White.copy(alpha = 0.4f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Avatar Image / Preset / Default Initial
        if (avatarUri.startsWith("content://") || avatarUri.startsWith("file://") || avatarUri.startsWith("http")) {
            Image(
                painter = rememberAsyncImagePainter(avatarUri),
                contentDescription = "$contactName Avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (preset != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(preset.gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preset.emoji,
                    fontSize = (size.value * 0.45f).sp
                )
            }
        } else {
            // Default initial letter avatar with frosted glass gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                themeColor.copy(alpha = 0.45f),
                                themeColor.copy(alpha = 0.20f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.42f).sp,
                    color = if (isDark) Color.White else themeColor
                )
            }
        }

        // Optional Camera Edit Badge
        if (showEditBadge) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.36f).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(themeColor)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Edit DP",
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.22f).dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarPickerModal(
    currentAvatarUri: String,
    contactName: String,
    contactType: String,
    themeColor: Color,
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit
) {
    val isDark = LocalIsDark.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onAvatarSelected(it.toString())
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCardContainer(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 500.dp)
                .wrapContentHeight()
                .padding(16.dp),
            themeColor = themeColor,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Custom Profile Picture & DP",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF1D1B20)
                )
                Text(
                    text = "Set a custom photo for $contactName (${if (contactType == "SELLER") "Wholesaler" else "Customer"})",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Current Avatar Preview
                GlassContactAvatar(
                    contactName = contactName,
                    avatarUri = currentAvatarUri,
                    size = 72.dp,
                    themeColor = themeColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action 1: Upload from Device Gallery
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Photo from Gallery", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Divider(color = themeColor.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(12.dp))

                // Action 2: Preset Avatar Options
                Text(
                    text = "Or Choose Preset Avatar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filteredPresets = PRESET_AVATARS.filter { it.category == contactType }
                val presetsToShow = if (filteredPresets.isEmpty()) PRESET_AVATARS else filteredPresets

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    presetsToShow.forEach { preset ->
                        val isSelected = currentAvatarUri == preset.id
                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(preset.gradientColors))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onAvatarSelected(preset.id)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset.emoji, fontSize = 24.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset to Default Initial Letter
                    TextButton(
                        onClick = {
                            onAvatarSelected("")
                            onDismiss()
                        }
                    ) {
                        Text("Reset DP", color = Color.Red.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
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
    var contactForAvatarChange by remember { mutableStateOf<KhataContact?>(null) }
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
                                imageVector = Icons.Default.Build,
                                contentDescription = "Khata Settings (Wrench)",
                                tint = themeColor,
                                modifier = Modifier.size(20.dp)
                            )
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
                        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
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
                                onAvatarClick = { contactForAvatarChange = contact },
                                onClick = { selectedContact = contact }
                            )
                        }
                    }
                }
            }
        }
    }

    if (contactForAvatarChange != null) {
        AvatarPickerModal(
            currentAvatarUri = contactForAvatarChange!!.avatarUri,
            contactName = contactForAvatarChange!!.name,
            contactType = contactForAvatarChange!!.type,
            themeColor = themeColor,
            onDismiss = { contactForAvatarChange = null },
            onAvatarSelected = { newUri ->
                viewModel.updateKhataContactAvatar(contactForAvatarChange!!, newUri)
                contactForAvatarChange = null
                Toast.makeText(context, "Profile Picture updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddContactDialog) {
        AddContactDialog(
            type = if (selectedTab == 0) "SELLER" else "CUSTOMER",
            theme = theme,
            onDismiss = { showAddContactDialog = false },
            onAdd = { name, phone, avatarUri ->
                viewModel.addKhataContact(name, phone, if (selectedTab == 0) "SELLER" else "CUSTOMER", avatarUri)
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
    onAvatarClick: (() -> Unit)? = null,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glass Contact Avatar with DP support and camera badge preview
            GlassContactAvatar(
                contactName = contact.name,
                avatarUri = contact.avatarUri,
                size = 48.dp,
                themeColor = themeColor,
                showEditBadge = true,
                onClick = onAvatarClick
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Contact Name & Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = contact.name.trim().ifBlank { "Unnamed Contact" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    color = if (isDark) Color.White else Color(0xFF1C1B1F),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (contact.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone Number",
                            tint = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = contact.phone,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Balance Summary
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (contact.type == "SELLER") "We owe them" else "They owe us",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF938F96) else Color(0xFF79747E),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatKhataCurrency(balance, context),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (balance > 0) Color(0xFFE53935) else Color(0xFF43A047),
                    maxLines = 1
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
    onAdd: (name: String, phone: String, avatarUri: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf("") }
    var showAvatarPicker by remember { mutableStateOf(false) }

    val themeColor = theme.primary()
    val isDark = LocalIsDark.current
    val textMain = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSecondary = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
    val inputBg = if (isDark) Color(0xFF2E2A36) else Color.White
    val borderFieldColor = if (isDark) Color(0xFF49454F) else Color(0xFF79747E)

    if (showAvatarPicker) {
        AvatarPickerModal(
            currentAvatarUri = avatarUri,
            contactName = name.ifBlank { if (type == "SELLER") "New Wholesaler" else "New Customer" },
            contactType = type,
            themeColor = themeColor,
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { selectedUri -> avatarUri = selectedUri }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCardContainer(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .wrapContentHeight()
                .imePadding(),
            themeColor = themeColor,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (type == "SELLER") "Add New Wholesaler" else "Add New Customer",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = textMain
                )
                Spacer(modifier = Modifier.height(14.dp))

                // DP Avatar Selector Preview
                GlassContactAvatar(
                    contactName = name.ifBlank { if (type == "SELLER") "Wholesaler" else "Customer" },
                    avatarUri = avatarUri,
                    size = 64.dp,
                    themeColor = themeColor,
                    showEditBadge = true,
                    onClick = { showAvatarPicker = true }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap photo to set custom DP or choose preset",
                    fontSize = 11.sp,
                    color = themeColor,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onAdd(name, phone, avatarUri)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Contact", color = Color.White, fontWeight = FontWeight.Bold)
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
    var showAvatarPickerForDetail by remember { mutableStateOf(false) }

    if (showAvatarPickerForDetail) {
        AvatarPickerModal(
            currentAvatarUri = contact.avatarUri,
            contactName = contact.name,
            contactType = contact.type,
            themeColor = themeColor,
            onDismiss = { showAvatarPickerForDetail = false },
            onAvatarSelected = { newUri ->
                viewModel.updateKhataContactAvatar(contact, newUri)
                Toast.makeText(context, "Profile Picture updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (LocalIsDark.current) Color(0xFF141218) else Color(0xFFFBF8FD))
    ) {
        // Main scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
        ) {
            // Back Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = themeColor)
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Glass Contact DP Avatar in Header
                    GlassContactAvatar(
                        contactName = contact.name,
                        avatarUri = contact.avatarUri,
                        size = 52.dp,
                        themeColor = themeColor,
                        showEditBadge = true,
                        onClick = { showAvatarPickerForDetail = true }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = if (LocalIsDark.current) Color.White else Color(0xFF1D1B20),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (contact.type == "SELLER") "Wholesaler Account" else "Customer Account",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = themeColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Tap photo to edit DP",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
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
            }

            // Outstanding Balance Summary Card
            item {
                GlassCardContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                            color = Color.Gray,
                            textAlign = TextAlign.Center
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
            }

            // Payment Settlement Graph Card
            item {
                val totalCredit = if (contact.type == "SELLER") {
                    transactions.filter { it.type == "WE_OWE" }.sumOf { it.amount }
                } else {
                    transactions.filter { it.type == "THEY_OWE" }.sumOf { it.amount }
                }
                val totalPaid = if (contact.type == "SELLER") {
                    transactions.filter { it.type == "WE_PAID" }.sumOf { it.amount }
                } else {
                    transactions.filter { it.type == "THEY_PAID" }.sumOf { it.amount }
                }
                val paidRatio = if (totalCredit > 0) (totalPaid / totalCredit).coerceIn(0.0, 1.0).toFloat()
                else if (totalPaid > 0) 1.0f
                else 0.0f
                val paidPct = (paidRatio * 100).toInt()

                Spacer(modifier = Modifier.height(4.dp))
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
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
                                    color = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (paidRatio >= 1.0f && totalCredit > 0) "100% Cleared ✅" else "$paidPct% Paid",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (paidRatio >= 1.0f) Color(0xFF388E3C) else themeColor,
                                maxLines = 1
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

                        // 3 Equal-width Columns to prevent text collision across devices
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = if (contact.type == "SELLER") "TOTAL WE OWE" else "TOTAL CREDIT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatKhataCurrency(totalCredit, context),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F),
                                    maxLines = 1
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TOTAL PAID",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatKhataCurrency(totalPaid, context),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF388E3C),
                                    maxLines = 1
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "REMAINING DUES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatKhataCurrency(balance, context),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (balance <= 0) Color(0xFF388E3C) else Color(0xFFD32F2F),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Timeline Header & Filter Chips
            item {
                Spacer(modifier = Modifier.height(8.dp))
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
                        fontSize = 13.sp,
                        color = if (LocalIsDark.current) Color(0xFFE6E1E5) else Color(0xFF1D1B20),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${filteredTransactions.size} shown",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                // Filter chips row with horizontal scroll
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
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
                                color = if (isSelected) themeColor else Color(0xFF49454F),
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Timeline Entries List
            if (filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = Color.LightGray.copy(alpha = 0.7f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No timeline entries found.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                itemsIndexed(filteredTransactions, key = { _, tx -> tx.id }) { _, tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.Top
                    ) {
                        val isAddition = tx.type == "THEY_OWE" || tx.type == "WE_OWE"
                        val nodeColor = if (isAddition) Color(0xFFC62828) else Color(0xFF2E7D32)

                        Box(
                            modifier = Modifier
                                .width(36.dp)
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
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(nodeColor.copy(alpha = 0.15f))
                                    .border(1.5.dp, nodeColor, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                            TransactionItemRow(
                                transaction = tx,
                                contactType = contact.type,
                                theme = theme,
                                onDelete = { viewModel.deleteKhataTransaction(tx) },
                                showTimelineLine = false
                            )
                        }
                    }
                }
            }
        }

        // Sticky Pinned Bottom Action Bar for Recording Udhaar & Cash Payments
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 12.dp,
            color = if (LocalIsDark.current) Color(0xFF1D1B20) else Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Took Udhaar (I Owe)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            selectedTxType = "WE_PAID"
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Paid Cash",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            selectedTxType = "THEY_OWE"
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gave Udhaar (+)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            selectedTxType = "THEY_PAID"
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Got Paid (-)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .wrapContentHeight()
                .imePadding(),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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
    val previewBg = if (isDark) Color(0xFF2A2733) else Color(0xFFF7F5F9)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 540.dp)
                .wrapContentHeight()
                .imePadding(),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Sheet Drag Handle Accent Bar
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(textSecondary.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🧾 Share Bill Statement",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = textMain
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Party: ${contact.name}",
                                fontSize = 13.sp,
                                color = themeColor,
                                fontWeight = FontWeight.Bold
                            )
                            if (contact.phone.isNotBlank()) {
                                Text(
                                    text = " (${contact.phone})",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(textSecondary.copy(alpha = 0.12f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Status Segmented Selector
                Text(
                    text = "Payment Status",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pending Button
                    val isPendingSelected = !isCleared
                    val pendingBg = if (isPendingSelected) Color(0xFFFFF3E0) else (if (isDark) Color(0xFF2E2A36) else Color(0xFFF3EDF7))
                    val pendingBorder = if (isPendingSelected) Color(0xFFE65100) else Color.Transparent
                    val pendingText = if (isPendingSelected) Color(0xFFE65100) else textSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(pendingBg)
                            .border(1.dp, pendingBorder, RoundedCornerShape(14.dp))
                            .clickable { isCleared = false }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Payment Pending ⏳",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = pendingText
                            )
                        }
                    }

                    // Paid Button
                    val isPaidSelected = isCleared
                    val paidBg = if (isPaidSelected) Color(0xFFE8F5E9) else (if (isDark) Color(0xFF2E2A36) else Color(0xFFF3EDF7))
                    val paidBorder = if (isPaidSelected) Color(0xFF2E7D32) else Color.Transparent
                    val paidText = if (isPaidSelected) Color(0xFF2E7D32) else textSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(paidBg)
                            .border(1.dp, paidBorder, RoundedCornerShape(14.dp))
                            .clickable { isCleared = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Paid / Cleared ✅",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = paidText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Language selection chips
                Text(
                    text = "Statement Language",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.fillMaxWidth()
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
                    label = { Text("Statement Amount (₹)") },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Formatted Live Message Preview Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Message Statement Preview",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )

                    // Active Status Badge Tag
                    Surface(
                        color = if (isCleared) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCleared) Color(0xFF2E7D32) else Color(0xFFE65100))
                    ) {
                        Text(
                            text = if (isCleared) "STATUS: PAID ✅" else "STATUS: PENDING ⏳",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCleared) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(previewBg, RoundedCornerShape(14.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = finalMessage,
                        fontSize = 13.sp,
                        color = textMain,
                        lineHeight = 19.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Buttons
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onShare(finalMessage) },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.6f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Statement", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Khata Ledger Settings",
                            color = textMain,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
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
    var calcMode by remember { mutableStateOf(0) } // 0 = Find Price by Weight, 1 = Find Weight by Budget

    var baseWeight by remember { mutableStateOf("100") }
    var baseWeightUnit by remember { mutableStateOf("g") } // "g" or "kg"
    var basePrice by remember { mutableStateOf("50") }

    // Mode 0: Target Weight input
    var targetWeight by remember { mutableStateOf("20") }
    var targetWeightUnit by remember { mutableStateOf("g") } // "g" or "kg"

    // Mode 1: Target Budget input
    var targetBudget by remember { mutableStateOf("20") }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isDark = LocalIsDark.current

    val themeColor = theme.primary()
    val cardBg = if (isDark) Color(0xFF211D2A) else Color(0xFFF9F6FC)
    val textMain = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSecondary = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)
    val inputBg = if (isDark) Color(0xFF2E2A36) else Color.White
    val borderColor = if (isDark) Color(0xFF49454F).copy(alpha = 0.5f) else Color(0xFFE1DDF3)

    // Parse Base Rate
    val bw = baseWeight.toDoubleOrNull() ?: 0.0
    val bp = basePrice.toDoubleOrNull() ?: 0.0
    val bwInGrams = if (baseWeightUnit == "kg") bw * 1000.0 else bw
    val unitPricePerGram = if (bwInGrams > 0.0 && bp >= 0.0) bp / bwInGrams else null

    // Mode 0 Calculation: Cost for Target Weight
    val tw = targetWeight.toDoubleOrNull() ?: 0.0
    val twInGrams = if (targetWeightUnit == "kg") tw * 1000.0 else tw
    val calculatedCost: Double? = if (unitPricePerGram != null && twInGrams >= 0.0) {
        unitPricePerGram * twInGrams
    } else null

    // Mode 1 Calculation: Weight for Target Budget
    val tb = targetBudget.toDoubleOrNull() ?: 0.0
    val calculatedWeightInGrams: Double? = if (unitPricePerGram != null && unitPricePerGram > 0.0 && tb >= 0.0) {
        tb / unitPricePerGram
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header with Title and Reset Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cost & Gram Calculator",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = textMain
                    )
                }
                Text(
                    text = "Quick pricing & weight calculator for custom quantities",
                    fontSize = 12.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 30.dp)
                )
            }

            IconButton(
                onClick = {
                    baseWeight = "100"
                    baseWeightUnit = "g"
                    basePrice = "50"
                    targetWeight = "20"
                    targetWeightUnit = "g"
                    targetBudget = "20"
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Mode Switcher (Pill Tabs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF2A2535) else Color(0xFFEDE8F5))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (calcMode == 0) themeColor else Color.Transparent)
                    .clickable { calcMode = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏷️ Find Cost",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (calcMode == 0) Color.White else textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (calcMode == 1) themeColor else Color.Transparent)
                    .clickable { calcMode = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💰 Find Weight",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (calcMode == 1) Color.White else textSecondary
                )
            }
        }

        // Hero Result Card
        GlassCardContainer(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            themeColor = themeColor,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (calcMode == 0) "TOTAL CALCULATED COST" else "YOU GET EXACTLY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColor,
                    letterSpacing = 0.5.sp
                )

                // Copy Action
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(themeColor.copy(alpha = 0.15f))
                        .clickable {
                            val summaryStr = if (calcMode == 0) {
                                "$targetWeight$targetWeightUnit @ ₹${"%.2f".format(calculatedCost ?: 0.0)} (Rate: $baseWeight$baseWeightUnit = ₹$basePrice)"
                            } else {
                                "₹$targetBudget buys ${if ((calculatedWeightInGrams ?: 0.0) >= 1000) "%.3f kg".format((calculatedWeightInGrams ?: 0.0)/1000) else "%.1f g".format(calculatedWeightInGrams ?: 0.0)} (Rate: $baseWeight$baseWeightUnit = ₹$basePrice)"
                            }
                            clipboardManager.setText(AnnotatedString(summaryStr))
                            Toast.makeText(context, "Copied result to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = themeColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColor)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Output Display
            if (calcMode == 0) {
                Text(
                    text = "₹${"%.2f".format(calculatedCost ?: 0.0)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = themeColor
                )
                Text(
                    text = if (calculatedCost != null && unitPricePerGram != null) {
                        "✨ $targetWeight $targetWeightUnit costs ₹${"%.2f".format(calculatedCost)} (Rate: ₹${"%.2f".format(unitPricePerGram)} / g)"
                    } else {
                        "Enter valid rate and quantity below"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMain
                )
            } else {
                val weightText = if (calculatedWeightInGrams != null) {
                    if (calculatedWeightInGrams >= 1000.0) {
                        "%.3f kg".format(calculatedWeightInGrams / 1000.0)
                    } else {
                        "%.1f g".format(calculatedWeightInGrams)
                    }
                } else "0 g"

                Text(
                    text = weightText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = themeColor
                )
                Text(
                    text = if (calculatedWeightInGrams != null && unitPricePerGram != null) {
                        "✨ For ₹$targetBudget, you get $weightText (Rate: ₹${"%.2f".format(unitPricePerGram)} / g)"
                    } else {
                        "Enter valid rate and budget below"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMain
                )
            }
        }

        // Quick Preset Items Bar
        Column {
            Text(
                text = "Quick Presets:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    Triple("🌾 Rice (1kg = ₹60)", "1", "60"),
                    Triple("🍬 Sugar (1kg = ₹45)", "1", "45"),
                    Triple("🫖 Tea (250g = ₹130)", "250", "130"),
                    Triple("🌶️ Spices (50g = ₹35)", "50", "35"),
                    Triple("🧈 Ghee (500g = ₹320)", "500", "320"),
                    Triple("🪙 Gold (1g = ₹6500)", "1", "6500")
                )
                presets.forEach { (label, pWeight, pPrice) ->
                    val pUnit = if (label.contains("1kg")) "kg" else "g"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF2A2535) else Color(0xFFEDE8F5))
                            .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable {
                                baseWeight = pWeight
                                baseWeightUnit = pUnit
                                basePrice = pPrice
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMain
                        )
                    }
                }
            }
        }

        // Card 1: Known Base Rate Input
        GlassCardContainer(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            themeColor = themeColor,
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(themeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Known Base Rate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = textMain
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Base Weight
                OutlinedTextField(
                    value = baseWeight,
                    onValueChange = { baseWeight = it },
                    label = { Text("Base Weight") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = borderColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )

                // Unit Toggle (g / kg)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(inputBg)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (baseWeightUnit == "g") themeColor else Color.Transparent)
                            .clickable { baseWeightUnit = "g" }
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Text("g", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (baseWeightUnit == "g") Color.White else textSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (baseWeightUnit == "kg") themeColor else Color.Transparent)
                            .clickable { baseWeightUnit = "kg" }
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Text("kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (baseWeightUnit == "kg") Color.White else textSecondary)
                    }
                }

                Text(
                    text = "=",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                // Base Price
                OutlinedTextField(
                    value = basePrice,
                    onValueChange = { basePrice = it },
                    label = { Text("Price (₹)") },
                    prefix = { Text("₹", fontWeight = FontWeight.Bold, color = themeColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = borderColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )
            }
        }

        // Card 2: Target Quantity / Budget Input
        GlassCardContainer(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            themeColor = themeColor,
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(themeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (calcMode == 0) "Quantity to Buy" else "Your Target Budget",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textMain
                    )
                }

                if (calcMode == 0) {
                    // Mode 0: Target Weight Input with Stepper and Unit Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val current = targetWeight.toDoubleOrNull() ?: 0.0
                                val step = if (targetWeightUnit == "kg") 0.1 else 10.0
                                if (current > step) {
                                    targetWeight = if (targetWeightUnit == "kg") "%.1f".format(current - step) else "%.0f".format(current - step)
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(themeColor.copy(alpha = 0.1f))
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 20.sp)
                        }

                        OutlinedTextField(
                            value = targetWeight,
                            onValueChange = { targetWeight = it },
                            label = { Text("Buy Weight") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = borderColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                val current = targetWeight.toDoubleOrNull() ?: 0.0
                                val step = if (targetWeightUnit == "kg") 0.1 else 10.0
                                targetWeight = if (targetWeightUnit == "kg") "%.1f".format(current + step) else "%.0f".format(current + step)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(themeColor.copy(alpha = 0.1f))
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 20.sp)
                        }

                        // Unit Toggle (g / kg)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(inputBg)
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (targetWeightUnit == "g") themeColor else Color.Transparent)
                                    .clickable { targetWeightUnit = "g" }
                                    .padding(horizontal = 10.dp, vertical = 12.dp)
                            ) {
                                Text("g", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (targetWeightUnit == "g") Color.White else textSecondary)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (targetWeightUnit == "kg") themeColor else Color.Transparent)
                                    .clickable { targetWeightUnit = "kg" }
                                    .padding(horizontal = 10.dp, vertical = 12.dp)
                            ) {
                                Text("kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (targetWeightUnit == "kg") Color.White else textSecondary)
                            }
                        }
                    }

                    // Quick Chips for Target Weight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val weightChips = if (targetWeightUnit == "kg") {
                            listOf("0.25", "0.5", "1", "2", "5")
                        } else {
                            listOf("10", "20", "50", "100", "250", "500")
                        }
                        weightChips.forEach { chipVal ->
                            val isSelected = targetWeight == chipVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) themeColor else themeColor.copy(alpha = 0.08f))
                                    .clickable { targetWeight = chipVal }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$chipVal$targetWeightUnit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else themeColor
                                )
                            }
                        }
                    }
                } else {
                    // Mode 1: Target Budget Input with Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val current = targetBudget.toDoubleOrNull() ?: 0.0
                                if (current > 10.0) {
                                    targetBudget = "%.0f".format(current - 10.0)
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(themeColor.copy(alpha = 0.1f))
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 20.sp)
                        }

                        OutlinedTextField(
                            value = targetBudget,
                            onValueChange = { targetBudget = it },
                            label = { Text("Budget (₹)") },
                            prefix = { Text("₹", fontWeight = FontWeight.Bold, color = themeColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = borderColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                val current = targetBudget.toDoubleOrNull() ?: 0.0
                                targetBudget = "%.0f".format(current + 10.0)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(themeColor.copy(alpha = 0.1f))
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, color = themeColor, fontSize = 20.sp)
                        }
                    }

                    // Quick Chips for Target Budget
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val budgetChips = listOf("10", "20", "50", "100", "200", "500")
                        budgetChips.forEach { chipVal ->
                            val isSelected = targetBudget == chipVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) themeColor else themeColor.copy(alpha = 0.08f))
                                    .clickable { targetBudget = chipVal }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "₹$chipVal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else themeColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
