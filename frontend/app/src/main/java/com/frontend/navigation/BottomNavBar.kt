package com.frontend.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(Routes.HOME, "홈", Icons.Filled.Home),
        BottomNavItem(Routes.WALK, "산책", Icons.Filled.Pets),
        BottomNavItem(Routes.RECORD, "기록", Icons.AutoMirrored.Filled.MenuBook),
        BottomNavItem(Routes.MY_INFO, "프로필", Icons.Filled.Person)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val borderColor = PointGreen.copy(alpha = 0.65f)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val strokeWidth = 2.5.dp.toPx()
            val cornerRadius = 28.dp.toPx()
            val lineY = strokeWidth / 2

            // 곡선 아래 흰색 채우기 (곡선 위는 투명 → 앱 배경 보임)
            val fillPath = Path().apply {
                moveTo(0f, cornerRadius)
                quadraticTo(0f, lineY, cornerRadius, lineY)
                lineTo(size.width - cornerRadius, lineY)
                quadraticTo(size.width, lineY, size.width, cornerRadius)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(fillPath, Color.White)

            // 연두색 곡선 선
            val strokePath = Path().apply {
                moveTo(0f, cornerRadius)
                quadraticTo(0f, lineY, cornerRadius, lineY)
                lineTo(size.width - cornerRadius, lineY)
                quadraticTo(size.width, lineY, size.width, cornerRadius)
            }
            drawPath(
                path = strokePath,
                color = borderColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        NavigationBar(
            containerColor = Color.White
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PointGreen,
                        selectedTextColor = PointGreen,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = Color.White
                    )
                )
            }
        }
    }
}
