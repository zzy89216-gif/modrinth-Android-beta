package com.modrinth.android.ui.project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.modrinth.domain.model.Project

@Composable
fun ProjectDetailScreen(
    projectId: String,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val project by viewModel.project.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模组详情") },
                navigationIcon = {
                    // TODO: 添加返回按钮
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            project?.let { p ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    Text(
                        text = p.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = p.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "作者: ${p.author}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "下载量: ${p.downloads}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
