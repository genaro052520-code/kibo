package com.genaro.kibo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.genaro.kibo.ui.theme.InventarioScreen
import com.genaro.kibo.ui.LoginScreen
import com.genaro.kibo.ui.theme.KIBOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            KIBOTheme {
                val vm: AppViewModel = viewModel()
                if (vm.sesion == null) LoginScreen(vm) else InventarioScreen(vm)
            }
        }
    }
}