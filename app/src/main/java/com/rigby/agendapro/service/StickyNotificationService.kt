package com.rigby.agendapro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.rigby.agendapro.MainActivity
import com.rigby.agendapro.R
import com.rigby.agendapro.data.local.NoteEntity
import com.rigby.agendapro.data.repository.NoteRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StickyNotificationService : Service() {

    @Inject
    lateinit var repository: NoteRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // CAMBIAMOS A V3 PARA FORZAR LA NUEVA PRIORIDAD ALTA
    private val CHANNEL_ID = "sticky_notes_channel_v3"
    private val NOTIFICATION_ID = 1001

    companion object {
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"
        const val EXTRA_NOTE_ID = "note_id"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()

        val initialNotification = buildNotification(emptyList())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        serviceScope.launch {
            repository.stickyNotes.collectLatest { notes ->
                val notification = buildNotification(notes)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }

        // START_STICKY le dice al sistema: "Si me matan (por swipe o memoria), revíveme"
        return START_STICKY
    }

    private fun buildNotification(notes: List<NoteEntity>): Notification {
        val collapsedView = RemoteViews(packageName, R.layout.notification_collapsed)
        collapsedView.setTextViewText(R.id.text_collapsed_content, "Tienes ${notes.size} tareas fijadas.")

        val expandedView = RemoteViews(packageName, R.layout.notification_expanded)

        // Botón "X" que mata el servicio INTENCIONALMENTE
        val stopIntent = Intent(this, StickyNotificationService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        expandedView.setOnClickPendingIntent(R.id.btn_close_service, stopPendingIntent)

        // --- TRUCO DE RESURRECCIÓN ---
        // Si el usuario borra la noti con Swipe, se dispara este Intent que reinicia el servicio
        val restartIntent = Intent(this, StickyNotificationService::class.java)
        val restartPendingIntent = PendingIntent.getService(
            this,
            666,
            restartIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Reset views
        expandedView.setViewVisibility(R.id.container_note_1, View.GONE)
        expandedView.setViewVisibility(R.id.container_note_2, View.GONE)
        expandedView.setViewVisibility(R.id.container_note_3, View.GONE)

        // Rellenar notas
        notes.forEachIndexed { index, note ->
            val titleId = when(index) { 0 -> R.id.text_title_1; 1 -> R.id.text_title_2; else -> R.id.text_title_3 }
            val contentId = when(index) { 0 -> R.id.text_content_1; 1 -> R.id.text_content_2; else -> R.id.text_content_3 }
            val containerId = when(index) { 0 -> R.id.container_note_1; 1 -> R.id.container_note_2; else -> R.id.container_note_3 }

            expandedView.setTextViewText(titleId, note.title)
            expandedView.setTextViewText(contentId, note.content)
            expandedView.setViewVisibility(containerId, View.VISIBLE)

            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NOTE_ID, note.id)
            }
            val appPendingIntent = PendingIntent.getActivity(this, index + 10, appIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            expandedView.setOnClickPendingIntent(containerId, appPendingIntent)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setDeleteIntent(restartPendingIntent) // <--- AQUÍ ESTÁ LA CLAVE
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Panel Permanente (Máxima)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Muestra tus notas fijadas siempre visibles"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // Método extra de seguridad: si el sistema mata la tarea
    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, StickyNotificationService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intentamos programar una alarma para reiniciar en 1 segundo
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmService.set(
            android.app.AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}