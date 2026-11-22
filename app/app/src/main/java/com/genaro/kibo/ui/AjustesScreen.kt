package com.genaro.kibo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.genaro.kibo.AppViewModel

@Composable
fun AjustesScreen(vm: AppViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Ajustes (Plan e Invitaciones)", style = MaterialTheme.typography.headlineSmall)
    }
}