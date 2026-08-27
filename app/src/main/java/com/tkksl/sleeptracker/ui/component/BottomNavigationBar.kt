package com.tkksl.sleeptracker.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tkksl.sleeptracker.navigation.BottomNavItem


@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {

    val backStackEntry =
        navController.currentBackStackEntryAsState()

    val currentRoute =
        backStackEntry.value?.destination?.route


    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.History,
        BottomNavItem.Settings
    )


    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {

        items.forEach { item ->

            val selected =
                currentRoute == item.route


            NavigationBarItem(

                selected = selected,


                onClick = {

                    navController.navigate(item.route) {

                        // 防止Tab重复压栈
                        popUpTo(
                            navController.graph.startDestinationRoute!!
                        ) {

                            saveState = true

                        }


                        launchSingleTop = true

                        restoreState = true
                    }
                },


                icon = {

                    Icon(

                        imageVector = item.icon,

                        contentDescription = item.title

                    )
                },


                label = {

                    Text(
                        text = item.title
                    )
                },


                colors = NavigationBarItemDefaults.colors(

                    // 选中图标
                    selectedIconColor =
                        MaterialTheme.colorScheme.primary,


                    // 选中文字
                    selectedTextColor =
                        MaterialTheme.colorScheme.primary,


                    // 未选中图标
                    unselectedIconColor =
                        MaterialTheme.colorScheme.onSurfaceVariant,


                    // 未选中文字
                    unselectedTextColor =
                        MaterialTheme.colorScheme.onSurfaceVariant,


                    // 胶囊背景
                    indicatorColor =
                        MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}