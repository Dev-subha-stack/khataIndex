package com.example.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val todoDao: TodoDao,
    private val khataDao: KhataDao
) {
    // Todo items delegation
    val allTodos: Flow<List<TodoItem>> = todoDao.getAllTodoItems()

    suspend fun insert(todo: TodoItem): Long {
        return todoDao.insertTodo(todo)
    }

    suspend fun update(todo: TodoItem) {
        todoDao.updateTodo(todo)
    }

    suspend fun delete(todo: TodoItem) {
        todoDao.deleteTodo(todo)
    }

    suspend fun deleteById(id: Int) {
        todoDao.deleteTodoById(id)
    }

    suspend fun clearCompleted() {
        todoDao.deleteCompletedTodos()
    }

    // KhataBook ledger delegation
    val allKhataContacts: Flow<List<KhataContact>> = khataDao.getAllContacts()
    
    fun getContactsByType(type: String): Flow<List<KhataContact>> {
        return khataDao.getContactsByType(type)
    }
    
    fun getTransactionsForContact(contactId: Int): Flow<List<KhataTransaction>> {
        return khataDao.getTransactionsForContact(contactId)
    }
    
    val allKhataTransactions: Flow<List<KhataTransaction>> = khataDao.getAllTransactions()
    
    suspend fun insertContact(contact: KhataContact): Long {
        return khataDao.insertContact(contact)
    }
    
    suspend fun updateContact(contact: KhataContact) {
        khataDao.updateContact(contact)
    }
    
    suspend fun deleteContactById(id: Int) {
        khataDao.deleteContactById(id)
        khataDao.deleteTransactionsByContactId(id)
    }
    
    suspend fun insertTransaction(transaction: KhataTransaction): Long {
        return khataDao.insertTransaction(transaction)
    }
    
    suspend fun deleteTransaction(transaction: KhataTransaction) {
        khataDao.deleteTransaction(transaction)
    }

    suspend fun clearAllKhataData() {
        khataDao.deleteAllContacts()
        khataDao.deleteAllTransactions()
    }
}
