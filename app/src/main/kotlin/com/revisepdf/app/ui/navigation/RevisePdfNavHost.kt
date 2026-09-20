package com.revisepdf.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.revisepdf.app.AppContainer
import com.revisepdf.app.ui.importing.ImportScreen
import com.revisepdf.app.ui.library.LibraryScreen
import com.revisepdf.app.ui.revision.RevisionScreen
import com.revisepdf.app.ui.settings.SettingsScreen

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_IMPORT = "import"
private const val ROUTE_REVISION = "revision/{documentId}"

@Composable
fun RevisePdfNavHost(
    container: AppContainer,
    pendingRevisionDocumentId: String?,
    onPendingRevisionConsumed: () -> Unit,
) {
    val navController = rememberNavController()

    // A content:// URI cannot travel as a route path argument: Uri.encode turns its slashes into
    // %2F, Navigation decodes them again while matching, and the extra segments stop the route
    // from ever matching — the app just sits on the library screen. It is held as state instead.
    var pickedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedName by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingRevisionDocumentId) {
        if (pendingRevisionDocumentId != null) {
            navController.navigate("revision/$pendingRevisionDocumentId")
            onPendingRevisionConsumed()
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_LIBRARY) {
        composable(ROUTE_LIBRARY) {
            LibraryScreen(
                container = container,
                onOpenDocument = { documentId -> navController.navigate("revision/$documentId") },
                onNewPdfPicked = { uri, name ->
                    pickedUri = uri.toString()
                    pickedName = name
                    navController.navigate(ROUTE_IMPORT)
                },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_IMPORT) {
            val uri = pickedUri
            ImportScreen(
                container = container,
                uri = remember(uri) { uri?.let(Uri::parse) },
                displayName = pickedName ?: "Selected PDF",
                onDone = { documentId ->
                    navController.navigate("revision/$documentId") {
                        popUpTo(ROUTE_LIBRARY)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = ROUTE_REVISION,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId").orEmpty()
            RevisionScreen(
                container = container,
                documentId = documentId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(container = container, onBack = { navController.popBackStack() })
        }
    }
}
