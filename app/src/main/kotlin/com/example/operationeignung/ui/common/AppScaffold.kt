package com.example.operationeignung.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import com.example.operationeignung.ui.navigation.Screen

// Globaler Zugriff auf eine App-weite Snackbar
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState not provided")
}

/** App-Gerüst mit TopAppBar/Overflow-Menü und optionaler Filter-Action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    current: Screen,
    showFilterAction: Boolean,
    onFilterClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    CompositionLocalProvider(LocalSnackbarHostState provides snackbar) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AppTopBar(
                    title = current.title,
                    showFilterAction = showFilterAction,
                    onFilterClick = onFilterClick,
                    onNavigate = { screen -> navController.navigate(screen.route) },
                    scrollBehavior = scrollBehavior
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { inner -> content(inner) }
    }
}


/** Oberleiste mit Titel, optionaler Filter-Action und Overflow-Menü. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    showFilterAction: Boolean,
    onFilterClick: () -> Unit,
    onNavigate: (Screen) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    var menuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        actions = {
            if (showFilterAction) {
                IconButton(onClick = onFilterClick) {
                    Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                }
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menü")
            }
            OverflowMenu(
                expanded = menuOpen,
                onDismiss = { menuOpen = false },
                onNavigate = { screen ->
                    menuOpen = false
                    onNavigate(screen)
                }
            )
        },
        scrollBehavior = scrollBehavior
    )
}

/** Overflow-Menü mit Navigation zu den bekannten Screens. */
@Composable
private fun OverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text(Screen.Dashboard.title) }, onClick = { onNavigate(Screen.Dashboard) })
        DropdownMenuItem(text = { Text(Screen.Quiz.title) }, onClick = { onNavigate(Screen.Quiz) })
        DropdownMenuItem(text = { Text(Screen.Import.title) }, onClick = { onNavigate(Screen.Import) })
        DropdownMenuItem(text = { Text(Screen.ManageQuestions.title) }, onClick = { onNavigate(Screen.ManageQuestions) })
        DropdownMenuItem(text = { Text(Screen.ManageCategories.title) }, onClick = { onNavigate(Screen.ManageCategories) })
        DropdownMenuItem(text = { Text(Screen.CleanUpData.title) }, onClick = { onNavigate(Screen.CleanUpData) })
        DropdownMenuItem(text = { Text(Screen.PromptTemplate.title) }, onClick = { onNavigate(Screen.PromptTemplate) })
        DropdownMenuItem(text = { Text(Screen.Info.title) }, onClick = { onNavigate(Screen.Info) })
    }
}
