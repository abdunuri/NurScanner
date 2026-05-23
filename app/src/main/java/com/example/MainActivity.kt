package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.repository.ReceiptRepository
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ReceiptViewModel
import com.example.ui.viewmodel.ReceiptViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize database and repository at Compose level using LocalContext
                val context = LocalContext.current
                val database = AppDatabase.getDatabase(context)
                val repository = ReceiptRepository(database.receiptDao(), context.applicationContext)
                val factory = ReceiptViewModelFactory(repository)
                
                // Expose our Shared Receipt ViewModel context across screens
                val viewModel: ReceiptViewModel = viewModel(factory = factory)
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "camera",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("camera") {
                            CameraScreen(
                                viewModel = viewModel,
                                onNavigateToReview = { navController.navigate("review") },
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("review") {
                            ReviewScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
