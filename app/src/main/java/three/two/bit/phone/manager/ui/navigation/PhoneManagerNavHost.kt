package three.two.bit.phone.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import three.two.bit.phone.manager.ui.home.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    // Future: Add Settings screen
}

@Composable
fun PhoneManagerNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }
        // Future: Add Settings screen route
    }
}
