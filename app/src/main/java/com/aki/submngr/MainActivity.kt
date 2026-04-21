package com.aki.submngr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aki.submngr.ui.screens.HomeScreen
import com.aki.submngr.ui.screens.SectionDetailScreen
import com.aki.submngr.ui.theme.SubmngrTheme
import com.aki.submngr.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubmngrTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

private val enter = fadeIn(tween(90))
private val exit = fadeOut(tween(70))

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val vm: MainViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { enter },
        popExitTransition = { exit }
    ) {
        composable("home") {
            HomeScreen(navController = navController, vm = vm)
        }
        composable(
            route = "section/{sectionId}",
            arguments = listOf(navArgument("sectionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getLong("sectionId") ?: return@composable
            SectionDetailScreen(navController = navController, sectionId = sectionId, vm = vm)
        }
    }
}
