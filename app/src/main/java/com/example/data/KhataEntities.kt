package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "khata_contacts")
data class KhataContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val type: String, // "SELLER" (wholesaler) or "CUSTOMER" (consumer)
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "khata_transactions")
data class KhataTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactId: Int,
    val description: String = "", // purchased items details, count, or custom note
    val amount: Double,
    val type: String, // For CUSTOMER: "THEY_OWE" or "THEY_PAID"; For SELLER: "WE_OWE" or "WE_PAID"
    val timestamp: Long = System.currentTimeMillis()
)
