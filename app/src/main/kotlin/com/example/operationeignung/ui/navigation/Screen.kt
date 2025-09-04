package com.example.operationeignung.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

    // Typsichere Routen
    sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Quiz : Screen("quiz", "Quiz", Icons.Default.Quiz)
    data object Import : Screen("import", "Import", Icons.Default.Settings)
    data object ManageQuestions : Screen("manage_questions", "Fragen verwalten", Icons.Default.Edit)
    data object ManageCategories : Screen("manage_categories", "Kategorien verwalten", Icons.Default.Settings)
    data object CleanUpData : Screen("clean_up_data", "Daten bereinigen", Icons.Default.CleaningServices)
    data object PromptTemplate : Screen("prompt_template", title = "KI-Prompt-Vorschlag", Icons.Default.AddRoad)
    data object Info : Screen("info", title = "Info", Icons.Default.Info)

    object AddEditQuestion : Screen("add_edit_question", title = "Frage hinzufügen/bearbeiten", Icons.Default.Edit) {
        const val ARG_ID = "questionId"
        const val ROUTE_WITH_ARG = "add_edit_question?$ARG_ID={$ARG_ID}"

        fun buildRoute(id: Int?): String =
            if (id != null) "add_edit_question?$ARG_ID=$id" else "add_edit_question"
    }
}
