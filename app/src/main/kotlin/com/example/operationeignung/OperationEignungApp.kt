package com.example.operationeignung

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.operationeignung.ui.common.AppScaffold
import com.example.operationeignung.ui.dashboard.DashboardScreen
import com.example.operationeignung.ui.info.InfoScreen
import com.example.operationeignung.ui.jsonimport.ImportScreen
import com.example.operationeignung.ui.manage.AddEditQuestionScreen
import com.example.operationeignung.ui.manage.CleanUpDataScreen
import com.example.operationeignung.ui.manage.ManageCategoriesScreen
import com.example.operationeignung.ui.manage.ManageQuestionsScreen
import com.example.operationeignung.ui.navigation.Screen
import com.example.operationeignung.ui.prompt.PromptTemplateScreen
import com.example.operationeignung.ui.quiz.QuizScreen

/** App-Einstieg: Scaffold und Navigation zusammensetzen. */
@Composable
fun OperationEignungApp() {
    val navController = rememberNavController()

    // Aktuellen Screen für Titel/Actions ermitteln
    val current = rememberCurrentScreen(navController)

    // Filter-Dialog (nur für Quiz)
    var openFilter by remember { mutableStateOf(false) }

    AppScaffold(
        navController = navController,
        current = current,
        showFilterAction = current == Screen.Quiz,
        onFilterClick = { openFilter = true }
    ) { innerPadding ->
        OperationEignungNavGraph(
            openFilter = openFilter,
            onFilterClose = { openFilter = false },
            navController = navController,
            contentPadding = innerPadding
        )
    }
}

/** Ermittelt den aktuellen Screen aus der Backstack-Route. */
@Composable
private fun rememberCurrentScreen(navController: NavHostController): Screen {
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Screen.Dashboard.route
    val base = route.substringBefore("?")
    return when (base) {
        Screen.Dashboard.route        -> Screen.Dashboard
        Screen.Quiz.route             -> Screen.Quiz
        Screen.ManageQuestions.route  -> Screen.ManageQuestions
        Screen.ManageCategories.route -> Screen.ManageCategories
        Screen.CleanUpData.route      -> Screen.CleanUpData
        Screen.PromptTemplate.route   -> Screen.PromptTemplate
        Screen.Import.route           -> Screen.Import
        Screen.Info.route             -> Screen.Info
        Screen.AddEditQuestion.route  -> Screen.AddEditQuestion
        else                          -> Screen.Dashboard
    }
}

/** Gesamte NavGraph-Definition. */
@Composable
private fun OperationEignungNavGraph(
    openFilter: Boolean,
    onFilterClose: () -> Unit,
    navController: NavHostController,
    contentPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                contentPadding = contentPadding,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(Screen.Quiz.route) {
            QuizScreen(
                contentPadding = contentPadding,
                filterOpen = openFilter,
                onFilterClose = onFilterClose,
                onEditQuestion = { id ->
                    navController.navigate(Screen.AddEditQuestion.buildRoute(id))
                }
            )
        }

        composable(Screen.ManageQuestions.route) {
            ManageQuestionsScreen(
                contentPadding = contentPadding,
            )
        }

        composable(Screen.ManageCategories.route) {
            ManageCategoriesScreen(contentPadding = contentPadding)
        }

        composable(Screen.CleanUpData.route) {
            CleanUpDataScreen(contentPadding = contentPadding)
        }

        composable(Screen.PromptTemplate.route) {
            PromptTemplateScreen(contentPadding = contentPadding)
        }

        composable(Screen.Info.route) {
            InfoScreen(
                contentPadding = contentPadding,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(Screen.Import.route) {
            ImportScreen(
                contentPadding = contentPadding,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // Add/Edit Frage: optionales Argument questionId
        composable(
            route = Screen.AddEditQuestion.ROUTE_WITH_ARG, // "add_edit_question?questionId={questionId}"
            arguments = listOf(
                navArgument(Screen.AddEditQuestion.ARG_ID) {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { entry ->
            val rawId = entry.arguments?.getInt(Screen.AddEditQuestion.ARG_ID) ?: -1
            val questionId: Int? = if (rawId == -1) null else rawId

            AddEditQuestionScreen(
                questionId = questionId,
                onSaveClick = { navController.popBackStack() }
            )
        }
    }
}
