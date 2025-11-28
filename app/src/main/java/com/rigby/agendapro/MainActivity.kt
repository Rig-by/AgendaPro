package com.rigby.agendapro

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rigby.agendapro.service.StickyNotificationService
import com.rigby.agendapro.ui.screens.AddEditNoteScreen
import com.rigby.agendapro.ui.screens.DashboardScreen
import com.rigby.agendapro.ui.screens.HomeScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startServiceSilently()

        setContent {
            // Tema personalizado (Dark Blue Pro)
            val darkColors = darkColorScheme(
                primary = Color(0xFF4B7BE5),
                onPrimary = Color.White,
                background = Color(0xFF0F111A), // Fondo casi negro
                surface = Color(0xFF1E212B), // Tarjetas gris oscuro
                onSurface = Color.White
            )
            val lightColors = lightColorScheme(
                primary = Color(0xFF2563EB),
                background = Color(0xFFF3F4F6),
                surface = Color.White,
                onSurface = Color.Black
            )

            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColors else lightColors
            ) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                // Permisos
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { if(it) startStickyService(context) }
                )
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            startStickyService(context)
                        }
                    } else {
                        startStickyService(context)
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.width(300.dp)
                        ) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "Agenda Pro",
                                modifier = Modifier.padding(horizontal = 24.dp),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(24.dp))

                            DrawerItem(Icons.Default.Dashboard, "Resumen", true) {
                                scope.launch { drawerState.close() }
                                navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } }
                            }
                            DrawerItem(Icons.Default.Note, "Todas las notas", false) {
                                scope.launch { drawerState.close() }
                                navController.navigate("notes")
                            }
                            DrawerItem(Icons.Default.Settings, "Configuración", false) {
                                scope.launch { drawerState.close() }
                            }
                        }
                    }
                ) {
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                onNavigateToNotes = { navController.navigate("notes") },
                                onNavigateToEditor = { navController.navigate("editor") },
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                        composable("notes") {
                            HomeScreen(
                                onNavigateToEditor = { id ->
                                    if(id != null) navController.navigate("editor?id=$id") else navController.navigate("editor")
                                },
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                        composable(
                            route = "editor?id={id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })
                        ) { backStackEntry ->
                            AddEditNoteScreen(
                                noteId = backStackEntry.arguments?.getString("id"),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startServiceSilently() {
        try {
            val intent = Intent(this, StickyNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        } catch (e: Exception) {}
    }
}

@Composable
fun DrawerItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, null) },
        label = { Text(label, fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(50),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
        )
    )
}

fun startStickyService(context: Context) {
    val intent = Intent(context, StickyNotificationService::class.java)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
    } catch (e: Exception) { e.printStackTrace() }
}