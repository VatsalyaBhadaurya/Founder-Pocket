package com.vatsalya.founderpocket.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.vatsalya.founderpocket.ui.capture.CaptureScreen
import com.vatsalya.founderpocket.ui.recall.RecallScreen
import com.vatsalya.founderpocket.ui.today.TodayScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    CAPTURE("Capture", Icons.Default.Home),
    RECALL("Recall",   Icons.Default.Search),
    TODAY("Today",     Icons.Default.Today)
}

@Composable
fun MainScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(Tab.CAPTURE) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected  = selectedTab == tab,
                        onClick   = { selectedTab = tab },
                        icon      = { Icon(tab.icon, contentDescription = tab.label) },
                        label     = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            Tab.CAPTURE -> CaptureScreen(
                onSaved = { /* stay on tab, just reset */ },
                modifier = Modifier.padding(padding)
            )
            Tab.RECALL  -> RecallScreen(
                onNavigateToDetail = onNavigateToDetail,
                modifier           = Modifier.padding(padding)
            )
            Tab.TODAY   -> TodayScreen(
                onNavigateToDetail = onNavigateToDetail,
                modifier           = Modifier.padding(padding)
            )
        }
    }
}
