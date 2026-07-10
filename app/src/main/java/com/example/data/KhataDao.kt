package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataDao {
    @Query("SELECT * FROM khata_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<KhataContact>>

    @Query("SELECT * FROM khata_contacts WHERE type = :type ORDER BY name ASC")
    fun getContactsByType(type: String): Flow<List<KhataContact>>

    @Query("SELECT * FROM khata_contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Int): KhataContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: KhataContact): Long

    @Update
    suspend fun updateContact(contact: KhataContact)

    @Delete
    suspend fun deleteContact(contact: KhataContact)

    @Query("DELETE FROM khata_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Int)

    @Query("SELECT * FROM khata_transactions WHERE contactId = :contactId ORDER BY timestamp ASC")
    fun getTransactionsForContact(contactId: Int): Flow<List<KhataTransaction>>

    @Query("SELECT * FROM khata_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<KhataTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: KhataTransaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: KhataTransaction)

    @Query("DELETE FROM khata_transactions WHERE contactId = :contactId")
    suspend fun deleteTransactionsByContactId(contactId: Int)

    @Query("DELETE FROM khata_contacts")
    suspend fun deleteAllContacts()

    @Query("DELETE FROM khata_transactions")
    suspend fun deleteAllTransactions()
}
