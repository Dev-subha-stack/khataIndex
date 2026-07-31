package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllReminders(context)
            return
        }

        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        val category = intent.getStringExtra(EXTRA_TASK_CATEGORY) ?: "General"
        val description = intent.getStringExtra(EXTRA_TASK_DESC) ?: ""

        showTaskNotification(context, taskId, title, category, description)
    }

    private fun showTaskNotification(
        context: Context,
        taskId: Int,
        title: String,
        category: String,
        description: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "todo_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Scheduled Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "Notifications for scheduled task time reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (taskId != -1) taskId else 100,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (description.isNotBlank()) {
            "[$category] $description"
        } else {
            "Time to complete your scheduled task! [$category]"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ $title")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(if (taskId != -1) taskId else System.currentTimeMillis().toInt(), builder.build())
    }

    private fun rescheduleAllReminders(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val db = TodoDatabase.getDatabase(context)
                val todos = db.todoDao().getAllTodoItems().first()
                val now = System.currentTimeMillis()
                todos.forEach { todo ->
                    if (!todo.isCompleted && todo.dueDate != null && todo.dueDate > now) {
                        scheduleTaskReminder(
                            context = context,
                            taskId = todo.id,
                            title = todo.title,
                            category = todo.category,
                            description = todo.description,
                            triggerAtMillis = todo.dueDate
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_CATEGORY = "extra_task_category"
        const val EXTRA_TASK_DESC = "extra_task_desc"

        fun scheduleTaskReminder(
            context: Context,
            taskId: Int,
            title: String,
            category: String,
            description: String,
            triggerAtMillis: Long
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, title)
                putExtra(EXTRA_TASK_CATEGORY, category)
                putExtra(EXTRA_TASK_DESC, description)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                try {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }

        fun cancelTaskReminder(context: Context, taskId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
