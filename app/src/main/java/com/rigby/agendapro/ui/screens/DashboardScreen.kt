package com.rigby.agendapro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rigby.agendapro.ui.NoteViewModel

@Composable
fun DashboardScreen(
    onNavigateToNotes: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val urgentNotes = notes.filter { it.urgency == 2 }
    val reminders = notes.filter { it.reminderTime != null }.sortedBy { it.reminderTime }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToEditor,
                containerColor = Color(0xFF2563EB), // Azul vibrante
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp) // Botón grande
            ) {
                Icon(Icons.Default.Add, "Nuevo", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, "Menú", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text("Resumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Icon(
                    if(isSystemInDarkTheme()) Icons.Default.LightMode else Icons.Default.DarkMode,
                    "Tema",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SALUDO
            Text(
                "¡Hola de nuevo!",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            // SECCIÓN: CITAS PRÓXIMAS
            Text("Citas Próximas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            if (reminders.isNotEmpty()) {
                reminders.take(3).forEach { note ->
                    DashboardCard(
                        icon = Icons.Default.Event,
                        title = note.title,
                        subtitle = "Recordatorio",
                        time = note.reminderTime?.let { java.text.SimpleDateFormat("HH:mm").format(it) } ?: "--:--",
                        iconBg = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                DashboardCard(Icons.Default.EventAvailable, "Sin citas próximas", "Disfruta tu día", "", Color(0xFF1E293B))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SECCIÓN: TAREAS URGENTES
            Text("Tareas Urgentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            if (urgentNotes.isNotEmpty()) {
                urgentNotes.take(3).forEach { note ->
                    DashboardCard(
                        icon = Icons.Default.PriorityHigh,
                        title = note.title,
                        subtitle = "Alta Prioridad",
                        time = "Hoy",
                        iconBg = Color(0xFF3F1313)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                DashboardCard(Icons.Default.CheckCircle, "Todo al día", "No hay urgencias", "", Color(0xFF064E3B))
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DashboardCard(icon: ImageVector, title: String, subtitle: String, time: String, iconBg: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }

        if (time.isNotEmpty()) {
            Text(
                time,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}