package com.example.mc_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import com.example.mc_project.ui.theme.MC_ProjectTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    private val requestedTheme = mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // If app launched from notification
        requestedTheme.value = readThemeFromIntent(intent)

        setContent {
            val systemDark = isSystemInDarkTheme()
            val profileVm: ProfileViewModel = viewModel()

            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }

            // theme applied only by clicking notification
            var appliedDark by rememberSaveable { mutableStateOf<Boolean?>(null) }

            // when notification clicked -> apply theme once
            LaunchedEffect(requestedTheme.value) {
                requestedTheme.value?.let {
                    appliedDark = it
                    requestedTheme.value = null
                }
            }

            // when switch OFF (SYSTEM) -> give control back to phone theme
            LaunchedEffect(themeMode) {
                if (themeMode == ThemeMode.SYSTEM) appliedDark = null
            }

            val effectiveDark =
                if (themeMode == ThemeMode.SYSTEM) systemDark else (appliedDark ?: systemDark)

            MC_ProjectTheme(darkTheme = effectiveDark) {
                Surface(Modifier.fillMaxSize()) {
                    Scaffold { innerPadding ->
                        MC_ProjectNavHost(
                            modifier = Modifier.padding(innerPadding),
                            currentDarkMode = effectiveDark,
                            profileVm = profileVm,
                            themeMode = themeMode,
                            onThemeModeChange = { themeMode = it }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTheme.value = readThemeFromIntent(intent)
    }

    private fun readThemeFromIntent(intent: Intent?): Boolean? {
        if (intent == null) return null
        return if (intent.hasExtra("apply_theme")) {
            intent.getBooleanExtra("apply_theme", false)
        } else null
    }
}

// Code from online source "developer" has been used as a base here
@Composable
fun MC_ProjectNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    currentDarkMode: Boolean,
    profileVm: ProfileViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    //val profileVm: ProfileViewModel = viewModel()

    // Only  run the sensor whem theme is set to follow sensor data
    if (themeMode == ThemeMode.SENSOR) {
        StartLightSensorFeed(vm = profileVm, currentDarkMode = currentDarkMode)
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.SECOND) {
            ConversationScreen(
                vm = profileVm,
                onGoToProfile = {
                    navController.navigate(Routes.MAIN) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            ProfileScreen(
                vm = profileVm,
                onGoToConversation = {
                    navController.navigate(Routes.SECOND) {
                        popUpTo(Routes.SECOND) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenCamera = {
                    navController.navigate(Routes.CAMERA)
                },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                vm = profileVm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
