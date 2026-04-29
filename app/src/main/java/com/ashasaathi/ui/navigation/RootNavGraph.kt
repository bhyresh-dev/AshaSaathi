package com.ashasaathi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.ashasaathi.ui.LocalAppLanguage
import com.ashasaathi.ui.viewmodel.AppViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ashasaathi.service.model.ModelDownloadService
import com.ashasaathi.ui.screens.auth.LoginScreen
import com.ashasaathi.ui.screens.auth.ProfileSetupScreen
import com.ashasaathi.ui.screens.auth.SplashScreen
import com.ashasaathi.ui.screens.diary.DiaryScreen
import com.ashasaathi.ui.screens.home.HomeScreen
import com.ashasaathi.ui.screens.households.AddHouseholdScreen
import com.ashasaathi.ui.screens.households.HouseholdDetailScreen
import com.ashasaathi.ui.screens.households.HouseholdsScreen
import com.ashasaathi.ui.screens.mcp.MCPCardScreen
import com.ashasaathi.ui.screens.patient.PatientDetailScreen
import com.ashasaathi.ui.screens.planner.PlannerScreen
import com.ashasaathi.ui.screens.reports.ReportsScreen
import com.ashasaathi.ui.screens.settings.SettingsScreen
import com.ashasaathi.ui.screens.setup.ModelSetupScreen
import com.ashasaathi.ui.screens.tb.TBDotsScreen
import com.ashasaathi.ui.screens.vaccination.VaccinationScreen
import com.ashasaathi.ui.screens.visit.VisitFormScreen
import com.ashasaathi.ui.screens.voiceform.VoiceFormScreen
import com.ashasaathi.ui.viewmodel.AuthViewModel

@Composable
fun RootNavGraph(modelDownloadService: ModelDownloadService) {
    val navController = rememberNavController()
    val authVm: AuthViewModel = hiltViewModel()
    val appVm:  AppViewModel  = hiltViewModel()
    val isLoggedIn by authVm.isLoggedIn.collectAsState(initial = null)
    val language   by appVm.language.collectAsState()

    CompositionLocalProvider(LocalAppLanguage provides language) {
    NavHost(
        navController = navController,
        startDestination = Route.SPLASH
    ) {
        composable(Route.SPLASH) {
            SplashScreen(
                isLoggedIn = isLoggedIn,
                onNavigate = { dest ->
                    val resolvedDest = if (dest == Route.HOME && !modelDownloadService.isWhisperReady())
                        Route.MODEL_SETUP else dest
                    navController.navigate(resolvedDest) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.MODEL_SETUP) {
            ModelSetupScreen(onSetupComplete = {
                navController.navigate(Route.HOME) {
                    popUpTo(Route.MODEL_SETUP) { inclusive = true }
                }
            })
        }

        composable(Route.LOGIN) {
            LoginScreen(onLoggedIn = {
                val dest = if (modelDownloadService.isWhisperReady()) Route.HOME else Route.MODEL_SETUP
                navController.navigate(dest) {
                    popUpTo(Route.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Route.PROFILE_SETUP) {
            ProfileSetupScreen(onComplete = {
                navController.navigate(Route.HOME) {
                    popUpTo(Route.PROFILE_SETUP) { inclusive = true }
                }
            })
        }

        composable(Route.HOME) {
            HomeScreen(navController = navController)
        }

        composable(Route.HOUSEHOLDS) {
            HouseholdsScreen(navController = navController)
        }

        composable(
            route = Route.HOUSEHOLD_DETAIL,
            arguments = listOf(navArgument("householdId") { type = NavType.StringType })
        ) { HouseholdDetailScreen(navController = navController) }

        composable(Route.ADD_HOUSEHOLD) {
            AddHouseholdScreen(navController = navController)
        }

        composable(
            route = Route.PATIENT_DETAIL,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { PatientDetailScreen(navController = navController) }

        composable(
            route = Route.VISIT_FORM,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { VisitFormScreen(navController = navController) }

        composable(Route.VACCINATION) { VaccinationScreen(navController = navController) }

        composable(Route.TB_DOTS) { TBDotsScreen(navController = navController) }

        composable(Route.DIARY) { DiaryScreen(navController = navController) }

        composable(
            route = Route.MCP_CARD,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { MCPCardScreen(navController = navController) }

        composable(Route.PLANNER) { PlannerScreen(navController = navController) }

        composable(Route.REPORTS) { ReportsScreen(navController = navController) }

        composable(Route.SETTINGS) { SettingsScreen(navController = navController) }

        composable(Route.VOICE_FORM) { VoiceFormScreen(navController = navController) }
    }
    } // CompositionLocalProvider
}
