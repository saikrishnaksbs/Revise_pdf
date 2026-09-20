package com.revisepdf.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
private const val ROUTE_IMPORT = "import/{uri}/{name}"
private const val ROUTE_REVISION = "revision/{documentId}"

@Composable
fun RevisePdfNavHost(
    container: AppContainer,
    pendingRevisionDocumentId: String?,
    onPendingRevisionConsumed: () -> Unit,
) {
    val navController = rememberNavController()

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
                    navController.navigate("import/${Uri.encode(uri.toString())}/${Uri.encode(name)}")
                },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(
            route = ROUTE_IMPORT,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val uriArg = backStackEntry.arguments?.getString("uri").orEmpty()
            val nameArg = backStackEntry.arguments?.getString("name").orEmpty()
            ImportScreen(
                container = container,
                uri = Uri.parse(Uri.decode(uriArg)),
                displayName = Uri.decode(nameArg),
                onDone = { documentId ->
                    navController.navigate("revision/$documentId") {
                        popUpTo(ROUTE_LIBRARY)
                    }
                },
                onFailed = { navController.popBackStack() },
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
