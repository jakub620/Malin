package com.example.szpital.ui.patients

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.szpital.data.api.PatientResponse
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

private val ColorHasTag   = Color(0xFF2E7D32)   // zielony – ma tag
private val ColorNoTag    = Color(0xFFF57C00)   // pomarańczowy – brak tagu
private val ColorLocation = Color(0xFF1565C0)   // niebieski – lokalizacja

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    onPatientClick: (PatientResponse) -> Unit,
    onCameraMode:   () -> Unit,
    vm: PatientsViewModel = viewModel()
) {
    val uiState    by vm.uiState.collectAsState()
    val actionError by vm.actionError.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    // Patient waiting for a tag scan
    var pendingTagPatient by remember { mutableStateOf<PatientResponse?>(null) }
    var tagScanError by remember { mutableStateOf<String?>(null) }

    // QR scanner launcher dla przypisania tagu
    val tagScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scanned = result.contents
        val patient = pendingTagPatient
        if (scanned != null && patient != null) {
            val tagId = if (scanned.contains("/")) scanned.substringAfterLast("/") else scanned
            vm.assignTag(patient.id, tagId)
        } else if (scanned == null) {
            tagScanError = "Anulowano skanowanie tagu"
        }
        pendingTagPatient = null
    }

    // Odśwież pacjentów przy otwarciu ekranu
    LaunchedEffect(Unit) {
        vm.loadPatients()
    }

    // Error notification
    val errorMsg = actionError ?: tagScanError
    val context = androidx.compose.ui.platform.LocalContext.current
    errorMsg?.let { msg ->
        LaunchedEffect(msg) {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            vm.clearError()
            tagScanError = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🏥 MALIN – Pacjenci", fontWeight = FontWeight.Bold)
                        Text(
                            text = when (uiState) {
                                is PatientsUiState.Success ->
                                    "${(uiState as PatientsUiState.Success).patients.size} pacjentów"
                                else -> "Ładowanie…"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    // Przycisk trybu kamery
                    IconButton(onClick = onCameraMode) {
                        Icon(Icons.Default.Camera, "Tryb kamery",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    // Odśwież
                    IconButton(onClick = { vm.loadPatients() }) {
                        Icon(Icons.Default.Refresh, "Odśwież",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon    = { Icon(Icons.Default.PersonAdd, null) },
                text    = { Text("Dodaj pacjenta") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is PatientsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PatientsUiState.Error -> {
                    Column(
                        modifier              = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, null,
                            modifier = Modifier.size(48.dp),
                            tint     = MaterialTheme.colorScheme.error)
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { vm.loadPatients() }) { Text("Spróbuj ponownie") }
                        Text(
                            "Upewnij się że backend działa:\nuvicorn main:app --host 0.0.0.0 --port 8001",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                is PatientsUiState.Success -> {
                    if (state.patients.isEmpty()) {
                        Column(
                            modifier            = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PersonOff, null,
                                modifier = Modifier.size(64.dp),
                                tint     = MaterialTheme.colorScheme.outline)
                            Text("Brak pacjentów",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.outline)
                            Text("Dodaj pierwszego pacjenta przyciskiem poniżej",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            contentPadding    = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.patients, key = { it.id }) { patient ->
                                PatientCard(
                                    patient       = patient,
                                    onClick       = { onPatientClick(patient) },
                                    onAssignTag   = {
                                        pendingTagPatient = patient
                                        tagScanLauncher.launch(
                                            ScanOptions().apply {
                                                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                                setPrompt("Zeskanuj opaskę QR pacjenta ${patient.name}")
                                                setBeepEnabled(true)
                                                setOrientationLocked(false)
                                            }
                                        )
                                    },
                                    onUnassignTag = { vm.unassignTag(patient.id) },
                                    onDelete      = { vm.deletePatient(patient.id) }
                                )
                            }
                            item { Spacer(Modifier.height(72.dp)) } // FAB clearance
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPatientDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, age, ward, notes ->
                vm.createPatient(name, age, ward, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PatientCard(
    patient:       PatientResponse,
    onClick:       () -> Unit,
    onAssignTag:   () -> Unit,
    onUnassignTag: () -> Unit,
    onDelete:      () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar z inicjałami
            Box(
                modifier        = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = patient.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(patient.name, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    patient.ward?.let {
                        Text("🏥 $it", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    patient.age?.let {
                        Text("$it lat", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Tag QR
                if (patient.tagId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp).clip(CircleShape)
                                .background(ColorHasTag)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Tag: ${patient.tagId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorHasTag)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp).clip(CircleShape)
                                .background(ColorNoTag)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Brak tagu QR",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorNoTag)
                    }
                }

                // Lokalizacja
                val loc = patient.location
                val localEntry = patient.tagId?.let { com.example.szpital.data.local.LocalQrDatabase.findByQrText(it) }

                if (loc != null || localEntry != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null,
                            modifier = Modifier.size(12.dp),
                            tint     = ColorLocation)
                        Spacer(Modifier.width(2.dp))
                        val text = buildString {
                            if (loc != null) {
                                loc.floor?.let { append("P$it ") }
                                loc.locationLabel?.let { append(it) }
                            } else if (localEntry != null) {
                                append("P${localEntry.floor} ")
                                append(localEntry.qrText)
                                append(" (Gmach Główny)")
                            }
                        }
                        Text(text, style = MaterialTheme.typography.labelSmall,
                            color = ColorLocation)
                    }
                }
            }

            // Menu kontekstowe
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Opcje")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (patient.tagId == null) {
                        DropdownMenuItem(
                            text        = { Text("Przypisz tag QR") },
                            leadingIcon = { Icon(Icons.Default.QrCode, null) },
                            onClick     = { showMenu = false; onAssignTag() }
                        )
                    } else {
                        DropdownMenuItem(
                            text        = { Text("Odepnij tag") },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) },
                            onClick     = { showMenu = false; onUnassignTag() }
                        )
                    }
                    DropdownMenuItem(
                        text        = { Text("Usuń pacjenta", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error) },
                        onClick     = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun AddPatientDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, age: Int?, ward: String?, notes: String?) -> Unit
) {
    var name  by remember { mutableStateOf("") }
    var age   by remember { mutableStateOf("") }
    var ward  by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title  = { Text("Nowy pacjent", fontWeight = FontWeight.Bold) },
        text   = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Imię i nazwisko *") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = age,
                    onValueChange = { age = it },
                    label         = { Text("Wiek") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = ward,
                    onValueChange = { ward = it },
                    label         = { Text("Oddział") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Notatki") },
                    minLines      = 2,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    if (name.isNotBlank())
                        onConfirm(name.trim(), age.toIntOrNull(),
                            ward.ifBlank { null }, notes.ifBlank { null })
                },
                enabled  = name.isNotBlank()
            ) { Text("Dodaj") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}


