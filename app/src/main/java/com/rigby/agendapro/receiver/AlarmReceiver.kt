package com.rigby.agendapro.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rigby.agendapro.MainActivity
import com.rigby.agendapro.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("TITLE") ?: "Recordatorio"
        val message = intent.getStringExtra("MESSAGE") ?: "Tienes una tarea pendiente"
        val noteId = intent.getStringExtra("NOTE_ID")
        val isAlarm = intent.getBooleanExtra("IS_ALARM", false)

        showNotification(context, title, message, noteId, isAlarm)
    }

    private fun showNotification(context: Context, title: String, message: String, noteId: String?, isAlarm: Boolean) {
        val channelId = if (isAlarm) "alarm_channel" else "reminder_channel"
        val channelName = if (isAlarm) "Alarmas" else "Recordatorios"
        val importance = if (isAlarm) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal de notificación (Necesario para Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Canal para ${channelName.lowercase()}"
                if (isAlarm) {
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
                    enableVibration(true)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("note_id", noteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (noteId ?: "0").hashCode(),
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri = if (isAlarm) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Construcción de la notificación
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(soundUri)

        if (isAlarm) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        }

        val notification = builder.build()

        // --- CORRECCIÓN CLAVE ---
        // Aplicamos la bandera INSISTENT directamente al objeto notificación final
        // Esto hace que el sonido se repita hasta que el usuario atienda la notificación.
        if (isAlarm) {
            notification.flags = notification.flags or Notification.FLAG_INSISTENT
        }

        notificationManager.notify((noteId ?: "0").hashCode(), notification)
    }
}