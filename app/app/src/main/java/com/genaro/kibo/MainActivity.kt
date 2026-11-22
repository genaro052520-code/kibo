package com.genaro.kibo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.genaro.kibo.ui.AgregarScreen
import com.genaro.kibo.ui.AjustesScreen
import com.genaro.kibo.ui.InicioScreen
import com.genaro.kibo.ui.InventarioScreen
import com.genaro.kibo.ui.LoginScreen
import com.genaro.kibo.ui.RegistroScreen
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

                BackHandler(enabled = vm.mostrandoAgregar) { vm.cerrarAgregar() }
                BackHandler(enabled = vm.sesion == null && vm.mostrandoRegistro) {
                    vm.cerrarRegistro()
                }
                BackHandler(enabled = vm.sesion != null && vm.pantallaActual != "inicio") {
                    vm.cambiarPantalla("inicio")
                }

                when {
                    vm.restaurandoSesion -> PantallaCargando()
                    vm.sesion == null && vm.mostrandoRegistro -> RegistroScreen(vm)
                    vm.sesion == null -> LoginScreen(vm)
                    vm.mostrandoAgregar -> AgregarScreen(vm)
                    else -> MainScreenWithNavigation(vm)
                }
            }
        }
    }
}

@Composable
fun PantallaCargando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MainScreenWithNavigation(vm: AppViewModel) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = vm.pantallaActual == "inicio",
                    onClick = { vm.cambiarPantalla("inicio") },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = vm.pantallaActual == "inventario",
                    onClick = { vm.cambiarPantalla("inventario") },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Inventario") },
                    label = { Text("Inventario") }
                )
                NavigationBarItem(
                    selected = vm.pantallaActual == "recetas",
                    onClick = { vm.cambiarPantalla("recetas") },
                    icon = { Icon(Icons.Filled.Restaurant, contentDescription = "Recetas") },
                    label = { Text("Recetas") }
                )
                NavigationBarItem(
                    selected = vm.pantallaActual == "ajustes",
                    onClick = { vm.cambiarPantalla("ajustes") },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (vm.pantallaActual) {
                "inicio" -> InicioScreen(vm)
                "inventario" -> InventarioScreen(vm)
                "recetas" -> PantallaEnConstruccion("Recetas")
                "ajustes" -> AjustesScreen(vm)
            }
        }
    }
}

@Composable
fun PantallaEnConstruccion(nombre: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Pantalla de $nombre en construcción 🚧")
    }
}