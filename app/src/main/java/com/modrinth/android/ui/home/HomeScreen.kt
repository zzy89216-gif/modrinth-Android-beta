package com.modrinth.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToCategories: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modrinth") },
                actions = {
                    TextButton(onClick = onNavigateToSearch) {
                        Text("搜索")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "欢迎使用 Modrinth Android",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onNavigateToCategories) {
                Text("浏览分类")
            }
        }
    }
}
