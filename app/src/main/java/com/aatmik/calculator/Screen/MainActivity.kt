package com.aatmik.calculator.Screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aatmik.calculator.ViewModel.MainScreenViewModel
import com.aatmik.calculator.components.MainScreen
import com.aatmik.calculator.ui.theme.CalculatorTheme

class MainActivity : ComponentActivity() {
    //private val viewModel: CalculatorViewModel by viewModels()
    private val viewModel: MainScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CalculatorTheme() {

                mainScreen()

            }
        }
    }

    @Composable
    private fun mainScreenOld() {
        //val state = viewModel.state
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            /* Calculator(
                 state = state,
                 modifier = Modifier
                     .fillMaxSize()
                     .padding(16.dp),
                 onAction = viewModel::onAction,
                 buttonSpacing = 12.dp
             )*/
        }
    }

    @Composable
    private fun mainScreen() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                category = viewModel::onCategoryClick,
                buttonSpacing = 12.dp
            )
        }
    }
}