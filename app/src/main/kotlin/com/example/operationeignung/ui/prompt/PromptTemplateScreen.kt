package com.example.operationeignung.ui.prompt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

/** Ausgabe des Vorschlages für das Prompt-Template **/
@Composable
fun PromptTemplateScreen(
    contentPadding: PaddingValues = PaddingValues()
) {
    var template by rememberSaveable { mutableStateOf(PromptTemplates.Default) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        PromptIntroText()

        PromptActions(
            onCopy = { clipboard.setText(AnnotatedString(template)) },
            onReset = { template = PromptTemplates.Default }
        )

        PromptEditor(
            value = template,
            onValueChange = { template = it }
        )
    }
}

/**Einleitungstext**/
@Composable
private fun PromptIntroText() {
    Text(
        // Das kurze Einleitungs-Label aus deinem bisherigen Screen.
        // (Den langen Prompt-Text NICHT hier, der steckt in PromptTemplates.Default)
        "Dies ist ein Vorschlag für einen Prompt, um durch KI Fragen zu generieren. " +
                "Passe ihn an deine Bedürfnisse an. Das JSON-Format muss gleich bleiben. " +
                "Vermeide der KI mitzuteilen, auf welchen Test bei welchem Arbeitgeber du dich konkret vorbereitest."
    )
}

/** Buttons erzeugen **/
@Composable
private fun PromptActions(
    onCopy: () -> Unit,
    onReset: () -> Unit
) {
    // Zwei Buttons untereinander
    Button(
        onClick = onCopy,
        modifier = Modifier.padding(top = 12.dp)
    ) { Text("Prompt in Zwischenablage kopieren") }

    Button(
        onClick = onReset,
        modifier = Modifier.padding(top = 8.dp)
    ) { Text("Auf Standard zurücksetzen") }
}

/** Eingabefeld anzeigen **/
@Composable
private fun PromptEditor(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        minLines = 6  // etwas höher als vorher für bessere Editierbarkeit
    )
}
