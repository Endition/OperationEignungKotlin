package com.example.operationeignung.ui.jsonimport
//Das Package darf nicht nur "import" heißen, da "import" ein Schlüsselwort ist und fatal error verursacht
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.operationeignung.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI für den Import **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onNavigate: (String) -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    var input by rememberSaveable { mutableStateOf("") } // input bei Recomposition/Navi nicht verloren
    val state by viewModel.state.collectAsState()
    var showJsonInfo by remember { mutableStateOf(false) }

    //GUI Beginn
    Column(
        Modifier
            .padding(contentPadding)
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Lasse dir von einer KI deiner Wahl Fragen als JSON-Liste generieren und speichere die Ausgabe der KI hier")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            //Link zum KI-Prompt-Vorschlag
            Text(
                text = "Zum KI-Prompt-Vorschlag",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    onNavigate(Screen.PromptTemplate.route)
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            //Link zur Erklräung für JSON
            Text(
                text = "Was ist JSON?",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {showJsonInfo = true}
            )
        }

        // Konfliktmodus
        ConflictModeSelector(
            value = state.conflictMode,
            onChange = { viewModel.setConflictMode(it) }
        )

        // Dateiauswahl (füllt das Textfeld & lässt dich sofort importieren)
        JsonFilePickerButton(
            enabled = !state.isBusy,
            onJsonPicked = { picked ->
                input = picked                  // Textfeld zeigt, was importiert wird
                // Optional Auto-Import:
                // viewModel.runImport(picked)
            }
        )

        // Eingabefeld für JSON (manuell einfügen/bearbeiten)
        JsonInputField(
            value = input,
            onValueChange = { input = it },
            isBusy = state.isBusy
        )

        ImportActions(
            canStart = input.isNotBlank(),
            isBusy   = state.isBusy,
            onStart  = { viewModel.runImport(input) },
            onReset  = { viewModel.reset(); input = "" },
        )


        // Status und Ergebnis des Imports ausgeben
        ImportResultSection(
            report   = state.report,
            isRunning = state.isBusy,
            error    = state.error,
            onRetry  = { viewModel.runImport(input) },       // "letzte Eingabe" erneut importieren
            onClose  = {
                viewModel.reset()
                input = ""                                   // UI zurücksetzen
            },
            modifier = Modifier.padding(top = 8.dp)
        )

        //Ausgabe der kurzen Erklärung zu JSON
        if (showJsonInfo) {
            ShowJsonInfo(onDismiss = { showJsonInfo = false })
        }
    }
}

//Helferfunkion um die kurze Erklärung zu JSON auszugeben
@Composable
private fun ShowJsonInfo(
    onDismiss: () -> Unit
){
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        title = { Text("Was ist JSON?") },
        text = {
            Text(
                "JSON ist eine einfache Text-Datei mit Daten in einer festgelegten Struktur. " +
                        "Hier enthält sie deine Fragen und Antworten. Du kannst so mehrere Fragen " +
                        "auf einmal importieren, statt sie manuell einzugeben."
            )
        }
    )
}

//Helferfunktion um die Datei anstatt das Textfeld auszulesen
private fun readTextFromUri(context: Context, uri: Uri): String {
    context.contentResolver.openInputStream(uri).use { input ->
        if (input == null) throw IllegalStateException("Kein Zugriff auf die Datei")
        val bytes = input.readBytes()
        //Große Dateien verhinderm
        if (bytes.size > 2 * 1024 * 1024) {
            throw IllegalArgumentException("Datei ist zu groß (>2 MB)")
        }
        return bytes.toString(Charsets.UTF_8)
    }
}

//Helferfunktion stellt Dateiauswahl bereit
@Composable
fun JsonFilePickerButton(
    enabled: Boolean,
    onJsonPicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // SAF-Launcher: bleibt intern; der Screen muss ihn nicht „sehen“.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val json = readTextFromUri(ctx, uri)
            withContext(Dispatchers.Main) {
                if (json.isNotBlank()) onJsonPicked(json)
            }
        }
    }

    Button(
        onClick = {
            launcher.launch(arrayOf("application/json", "application/*+json", "text/json", "text/plain"))
        },
        enabled = enabled,
        modifier = modifier
    ) {
        Text("JSON-Datei wählen")
    }
}

//Helferfunktion stellt Inputfeld bereit
@Composable
fun JsonInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
    label: String = "JSON eingeben oder aus Datei laden"
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            enabled = !isBusy,
            singleLine = false,
            minLines = 6,
            maxLines = 8, // wächst NICHT mehr über 8 Zeilen hinaus
            modifier = Modifier.fillMaxWidth()
        )
        // Kleiner Counter/Hint
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${value.length} Zeichen", style = MaterialTheme.typography.bodySmall)
            // einfache Gültigkeitsprüfung anzeigen
            Text(if (value.isBlank()) "Eingabe ist leer" else "OK", style = MaterialTheme.typography.bodySmall)
        }
    }
}

//Helferfunktion für die Auswahl des Konfliktmodus
@Composable
fun ConflictModeSelector(
    value: ConflictMode,
    onChange: (ConflictMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(ConflictMode.SKIP, ConflictMode.UPDATE)

    Column(modifier) {
        Text("Verhalten bei doppelten Fragen", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { mode ->
                FilterChip(
                    selected = value == mode,
                    onClick = { onChange(mode) },
                    label = { Text(text = mode.toUiLabel(), textAlign = TextAlign.Center) }
                )
            }
        }
    }
}


/** "Import starten" und "Zurücksetzen" */
@Composable
fun ImportActions(
    canStart: Boolean,   // z.B. input.isNotBlank()
    isBusy: Boolean,     // z.B. state.isBusy
    onStart: () -> Unit, // z.B. { viewModel.runImport(input) }
    onReset: () -> Unit, // z.B. { viewModel.reset(); input = "" }
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Primär-Action: Import starten
        Button(
            onClick = onStart,
            enabled = canStart && !isBusy,
            modifier = Modifier.weight(1f) // verteilt Platz gleichmäßig
        ) {
            Text("Import starten")
        }

        // Sekundär-Action: Zurücksetzen
        OutlinedButton(
            onClick = onReset,
            enabled = !isBusy,
            modifier = Modifier.weight(1f)
        ) {
            Text("Zurücksetzen")
        }
    }
}

// Kleine UI-Hilfe für Labels
private fun ConflictMode.toUiLabel(): String = when (this) {
    ConflictMode.SKIP -> "Überspringen"
    ConflictMode.UPDATE -> "Aktualisieren"
}

