package com.example.operationeignung.ui.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.operationeignung.ui.navigation.Screen

/** Gibt des InfoScreen aus **/
@Composable
fun InfoScreen(
    onNavigate: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
){
    Column(
        Modifier
            .padding(contentPadding)
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Kopf
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Operation Eignung",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            "Diese App unterstützt dich bei der Vorbereitung auf Eignungstests.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Kurze Anleitung
        SectionCard(
            title = "So funktioniert's",
            icon = Icons.Default.QuestionAnswer
        ) {
            Bullet("Lasse eine KI deiner Wahl Fragen erstellen und importiere diese in die App.")
            Bullet("Es gibt drei Fragentypen: Multiple-Choice, Code-Aufgabe und Freitext-Aufgabe.")
            Bullet("Wähle einen Quiz-Modus: Zufällige Frage (random), nur neue Fragen (new), nur falsch beantwortete Fragen (wrong)")
            Bullet("Wähle zusätzlich Kategorien aus oder übe alles.")
            Bullet("Starte das Quiz: Jede Frage hat eine Erklärung oder eine Musterlösung.")
            Bullet("Dein Fortschritt wird ausschließlich lokal gespeichert.")
            Bullet("Läuft etwas schief, kannst du Fragen und Kategorien bearbeiten sowie automatische Tools zur Bereinigung nutzen")
        }

        // Links
        AssistChipRow(onNavigate)

        // Tips
        SectionCard(title = "Tipps für den Test", icon = Icons.Default.CheckCircle) {
            Bullet("Lies die Frage vollständig, achte auf Schlüsselwörter.")
            Bullet("Bei MC-Fragen: erst ausschließen, dann entscheiden.")
            Bullet("Nutze 'Überspringen', wenn du unsicher bist, und komme später zurück.")
        }

        // Datenschutz
        SectionCard(title = "Datenschutz", icon = Icons.Default.Security) {
                Bullet("Die App kann vollständig offline genutzt werden. Persönliche Daten werden ausschließlich auf deinem Gerät verarbeitet.")
                Bullet("Vermeide der KI mitzuteilen auf welchen konkreten Test bei welchem Arbeitgeber du dich vorbereitest")

        }

        // Footer / version
        Text(
            "Version 1.0.0 \u00B7 \u00A9 2025 Operation Eignung",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
    }
}

//Helferfunktion für die Links als Chips
@Composable
private fun AssistChipRow( onNavigate: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = { onNavigate(Screen.Import.route)},
            label = { Text("Fragen importieren") },
            leadingIcon = { Icon(Icons.Default.ImportExport, contentDescription = null) }
        )
        AssistChip(
            onClick = { onNavigate(Screen.Quiz.route)},
            label = { Text("Quiz starten") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
        )
    }
}

//Helferfunktion zur Erstellung eines Abschnittes mit Card
@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

//Helferfunktion zur Erstellung eines AUfzählungspunktes
@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text("\u2022", modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
