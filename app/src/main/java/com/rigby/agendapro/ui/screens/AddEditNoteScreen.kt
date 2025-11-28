package com.rigby.agendapro.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rigby.agendapro.ui.NoteBlock
import com.rigby.agendapro.ui.NoteViewModel
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    noteId: String?,
    onNavigateBack: () -> Unit,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val existingNote = remember(noteId, notes) { notes.find { it.id == noteId } }

    // --- ESTADOS DE DATOS ---
    var title by remember { mutableStateOf("") }
    // Lista mutable de bloques (Texto, Imagen, Audio, Check)
    var blocks by remember { mutableStateOf<List<NoteBlock>>(listOf(NoteBlock.Text(text = ""))) }

    var urgency by remember { mutableStateOf(0) }
    var isPinned by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var isAlarmType by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf<String?>(null) }

    // --- ESTADOS DE UI (Grabadora) ---
    var showRecorder by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recorderDuration by remember { mutableStateOf(0L) }

    // Referencias de Media
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val currentAudioFile = remember { mutableStateOf<File?>(null) }

    // --- CARGA INICIAL ---
    LaunchedEffect(existingNote) {
        existingNote?.let {
            title = it.title
            urgency = it.urgency
            isPinned = it.isPinned
            reminderTime = it.reminderTime
            isAlarmType = it.isAlarm
            repeatMode = it.repeatMode
            // Cargar bloques
            blocks = viewModel.parseBlocks(it.blocksData)
        }
    }

    // --- PICKERS ---
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newBlocks = uris.map { NoteBlock.Image(uri = it.toString()) }
            // Añadir imágenes y luego un bloque de texto vacío para seguir escribiendo
            blocks = blocks + newBlocks + NoteBlock.Text(text = "")
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if(it) showRecorder = true
    }

    // --- TIMER GRABADORA ---
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            val startTime = System.currentTimeMillis() - recorderDuration
            while(isRecording && !isPaused) {
                recorderDuration = System.currentTimeMillis() - startTime
                kotlinx.coroutines.delay(100)
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F111A), // Fondo Oscuro
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    if (reminderTime != null) {
                        Icon(Icons.Default.Alarm, null, tint = Color(0xFF4B7BE5), modifier = Modifier.padding(end = 8.dp))
                    }
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(Icons.Default.PushPin, null, tint = if(isPinned) Color(0xFF4B7BE5) else Color.Gray)
                    }
                    IconButton(onClick = {
                        // Guardar
                        viewModel.saveNote(existingNote, title, blocks, urgency, isPinned, reminderTime, isAlarmType, repeatMode)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF4B7BE5))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F111A))
            )
        },
        bottomBar = {
            // Barra de Herramientas Inferior
            if (!showRecorder) {
                BottomAppBar(
                    containerColor = Color(0xFF1E212B),
                    actions = {
                        IconButton(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Default.Image, "Foto", tint = Color.White)
                        }
                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                showRecorder = true
                            } else {
                                micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }) {
                            Icon(Icons.Default.Mic, "Voz", tint = Color.White)
                        }
                        IconButton(onClick = {
                            blocks = blocks + NoteBlock.Checkbox(text = "", checked = false)
                        }) {
                            Icon(Icons.Default.CheckBox, "Checklist", tint = Color.White)
                        }
                        IconButton(onClick = {
                            // Añadir alarma lógica aquí (Show Dialog)
                        }) {
                            Icon(Icons.Default.AlarmAdd, "Alarma", tint = Color.White)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // --- LISTA PRINCIPAL DE BLOQUES ---
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 100.dp) // Espacio para grabadora
            ) {
                // 1. Título
                item {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(Color(0xFF4B7BE5)),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) Text("Título", color = Color.Gray, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }

                // 2. Bloques Dinámicos
                items(blocks) { block ->
                    when (block) {
                        is NoteBlock.Text -> {
                            BasicTextField(
                                value = block.text,
                                onValueChange = { newText ->
                                    // Actualizar el texto en la lista
                                    blocks = blocks.map { if (it == block) block.copy(text = newText) else it }
                                },
                                textStyle = TextStyle(color = Color(0xFFE2E8F0), fontSize = 16.sp, lineHeight = 24.sp),
                                cursorBrush = SolidColor(Color(0xFF4B7BE5)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                decorationBox = { innerTextField ->
                                    if (block.text.isEmpty() && blocks.last() == block) {
                                        Text("Escribe aquí...", color = Color.Gray)
                                    }
                                    innerTextField()
                                }
                            )
                        }
                        is NoteBlock.Image -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp))) {
                                AsyncImage(
                                    model = block.uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                                IconButton(
                                    onClick = { blocks = blocks - block },
                                    modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(0.5f), CircleShape)
                                ) { Icon(Icons.Default.Close, null, tint = Color.White) }
                            }
                        }
                        is NoteBlock.Audio -> {
                            AudioPlayerBlock(
                                path = block.path,
                                onDelete = { blocks = blocks - block }
                            )
                        }
                        is NoteBlock.Checkbox -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = block.checked,
                                    onCheckedChange = { isChecked ->
                                        blocks = blocks.map { if (it == block) block.copy(checked = isChecked) else it }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4B7BE5))
                                )
                                BasicTextField(
                                    value = block.text,
                                    onValueChange = { newText ->
                                        blocks = blocks.map { if (it == block) block.copy(text = newText) else it }
                                    },
                                    textStyle = TextStyle(
                                        color = if(block.checked) Color.Gray else Color.White,
                                        fontSize = 16.sp
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF4B7BE5)),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { blocks = blocks - block }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // --- PANEL DE GRABACIÓN (STICKY BOTTOM) ---
            if (showRecorder) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212B)),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            formatTime(recorderDuration),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Cancelar
                            IconButton(onClick = {
                                try { recorder.value?.release() } catch(e:Exception){}
                                showRecorder = false
                                isRecording = false
                                recorderDuration = 0
                            }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                            }

                            // Botón Acción Principal (Grabar / Stop)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) Color.Red else Color(0xFF4B7BE5))
                                    .clickable {
                                        if (!isRecording) {
                                            // INICIAR
                                            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.mp3")
                                            currentAudioFile.value = file
                                            recorder.value = MediaRecorder().apply {
                                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                setOutputFile(file.absolutePath)
                                                prepare()
                                                start()
                                            }
                                            isRecording = true
                                            isPaused = false
                                        } else {
                                            // PARAR Y GUARDAR
                                            try {
                                                recorder.value?.stop()
                                                recorder.value?.release()
                                            } catch(e: Exception){}
                                            isRecording = false
                                            showRecorder = false
                                            recorderDuration = 0

                                            // AÑADIR BLOQUE DE AUDIO AL FINAL
                                            currentAudioFile.value?.let {
                                                blocks = blocks + NoteBlock.Audio(path = it.absolutePath) + NoteBlock.Text(text = "")
                                            }
                                        }
                                    }
                            ) {
                                Icon(
                                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Pausa (Si está grabando)
                            IconButton(onClick = {
                                if (isRecording) {
                                    if (isPaused) {
                                        recorder.value?.resume()
                                        isPaused = false
                                    } else {
                                        recorder.value?.pause()
                                        isPaused = true
                                    }
                                }
                            }) {
                                Icon(
                                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    null,
                                    tint = if(isRecording) Color.White else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTE DE REPRODUCTOR DE AUDIO ---
@Composable
fun AudioPlayerBlock(path: String, onDelete: () -> Unit) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0) }
    val player = remember { MediaPlayer() }

    // Configuración inicial del player
    DisposableEffect(path) {
        try {
            player.setDataSource(path)
            player.prepare()
            duration = player.duration
        } catch (e: Exception) { e.printStackTrace() }

        onDispose { player.release() }
    }

    // Actualizador de progreso
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                if (player.isPlaying) {
                    progress = player.currentPosition.toFloat() / duration.toFloat()
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212B)), // Azul oscuro
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            // Botón Play/Pause Circular
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4B7BE5))
                    .clickable {
                        if (isPlaying) {
                            player.pause()
                            isPlaying = false
                        } else {
                            player.start()
                            isPlaying = true
                            player.setOnCompletionListener {
                                isPlaying = false
                                progress = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Barra de Progreso
            Column(modifier = Modifier.weight(1f)) {
                Slider(
                    value = progress,
                    onValueChange = {
                        progress = it
                        player.seekTo((it * duration).toInt())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF4B7BE5),
                        activeTrackColor = Color(0xFF4B7BE5),
                        inactiveTrackColor = Color.Gray.copy(0.3f)
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime((progress * duration).toLong()), color = Color.Gray, fontSize = 10.sp)
                    Text(formatTime(duration.toLong()), color = Color.Gray, fontSize = 10.sp)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, null, tint = Color.Gray)
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 1000) / 60
    return String.format("%02d:%02d", minutes, seconds)
}