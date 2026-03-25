package com.frontend.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.frontend.ui.screen.badge.BadgeScreen
import com.frontend.ui.screen.dog.DogEditScreen
import com.frontend.ui.screen.dog.DogProfileScreen
import com.frontend.ui.screen.dog.MetDogsScreen
import com.frontend.ui.screen.home.HomeScreen
import com.frontend.ui.screen.login.LoginScreen
import com.frontend.ui.screen.place.AddPlaceScreen
import com.frontend.ui.screen.record.RecordScreen
import com.frontend.ui.screen.splash.SplashScreen
import com.frontend.ui.screen.walk.WalkDetailScreen
import com.frontend.ui.screen.walk.WalkScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Routes.HOME, Routes.WALK, Routes.RECORD, Routes.MY_INFO
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            // TODO: (로그인 우회 : Routes.HOME) (원래: Routes.SPLASH)
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(navController = navController)
            }
            composable(Routes.LOGIN) {
                LoginScreen(navController = navController)
            }
            composable(Routes.HOME) {
                HomeScreen()
            }
            composable(Routes.WALK) {
                WalkScreen(
                    onNavigateToRecord = {
                        navController.navigate(Routes.RECORD) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToWalkDetail = { walkId ->
                        navController.navigate(Routes.walkDetail(walkId))
                    },
                    onNavigateToHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAddPlace = {
                        navController.navigate(Routes.ADD_PLACE)
                    }
                )
            }
            composable(Routes.ADD_PLACE) {
                AddPlaceScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.RECORD) {
                RecordScreen(
                    onWalkClick = { walkId ->
                        navController.navigate(Routes.walkDetail(walkId))
                    }
                )
            }
            composable(Routes.MY_INFO) {
                DogProfileScreen(navController = navController)
            }
            composable(Routes.BADGES) {
                BadgeScreen(navController = navController)
            }
            composable(Routes.DOG_EDIT) {
                DogEditScreen(navController = navController)
            }
            composable(
                route = Routes.MET_DOGS,
                arguments = listOf(navArgument("dogId") { type = NavType.LongType })
            ) {
                MetDogsScreen(navController = navController)
            }
            composable(
                route = Routes.WALK_DETAIL,
                arguments = listOf(navArgument("walkId") { type = NavType.LongType })
            ) {
                WalkDetailScreen(navController = navController)
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, fontSize = 24.sp)
    }
}
