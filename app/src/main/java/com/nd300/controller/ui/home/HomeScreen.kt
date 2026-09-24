package com.nd300.controller.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nd300.controller.R
import com.nd300.controller.ui.viewmodel.ConnectionState
import com.nd300.controller.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen() {
    val viewModel: HomeViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringRes(R.string.home_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(20.dp))

        StatusCard(state.connectionState, state.isBusy) { viewModel.refreshStatus() }

        Spacer(Modifier.height(24.dp))

        ActionButton(
            text = stringRes(R.string.enable_internet),
            icon = Icons.Filled.Wifi,
            containerColor = MaterialTheme.colorScheme.primary,
            enabled = !state.isBusy
        ) { viewModel.enableInternet() }

        Spacer(Modifier.height(12.dp))

        ActionButton(
            text = stringRes(R.string.disable_internet),
            icon = Icons.Filled.WifiOff,
            containerColor = MaterialTheme.colorScheme.error,
            enabled = !state.isBusy
        ) { viewModel.disableInternet() }

        Spacer(Modifier.height(12.dp))

        ActionButton(
            text = stringRes(R.string.reboot_router),
            icon = Icons.Filled.RestartAlt,
            containerColor = MaterialTheme.colorScheme.secondary,
            enabled = !state.isBusy
        ) { viewModel.reboot() }

        Spacer(Modifier.height(20.dp))

        state.lastMessage?.let { msg ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(msg, modifier = Modifier.padding(14.dp))
            }
        }

        if (state.isBusy) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun StatusCard(connectionState: ConnectionState, isBusy: Boolean, onRefresh: () -> Unit) {
    val (label, color) = when (connectionState) {
        ConnectionState.CONNECTED -> stringRes(R.string.connected) to Color(0xFF2E7D32)
        ConnectionState.DISCONNECTED -> stringRes(R.string.disconnected) to Color(0xFFC62828)
        ConnectionState.UNKNOWN -> "—" to Color.Gray
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringRes(R.string.connection_status), style = MaterialTheme.typography.labelMedium)
                Text(label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = onRefresh, enabled = !isBusy) {
                Icon(Icons.Filled.Refresh, contentDescription = stringRes(R.string.refresh_status))
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.platform.LocalContext.current.getString(id)
