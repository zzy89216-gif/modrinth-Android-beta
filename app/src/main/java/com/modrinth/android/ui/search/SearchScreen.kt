package com.modrinth.android.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.modrinth.domain.model.Project

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val projects by viewModel.projects.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索模组") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn {
                    items(projects) { project ->
                        ProjectItem(project = project)
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectItem(project: Project) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = project.description.take(100) + "...",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "下载量: ${project.downloads}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
