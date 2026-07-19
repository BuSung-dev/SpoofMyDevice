package com.devicespooflab.hooks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.devicespooflab.hooks.R

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun ProfileEditorLoadingScreen(
    creating: Boolean,
    onBack: () -> Unit,
) {
    ProfileEditorScaffold(
        title = stringResource(if (creating) R.string.profile_create_title else R.string.profile_edit_title),
        saveEnabled = false,
        snackbarHostState = remember { SnackbarHostState() },
        onSave = {},
        onBack = onBack,
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LoadingIndicator(modifier = Modifier.progressSemantics())
                Spacer(Modifier.height(MdSpacing.sm))
                Text(
                    text = stringResource(R.string.profile_editor_loading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun ProfileEditorScreen(
    appState: SpoofAppState,
    creating: Boolean,
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = appState.message
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            appState.consumeMessage()
        }
    }

    ProfileEditorScaffold(
        title = stringResource(if (creating) R.string.profile_create_title else R.string.profile_edit_title),
        saveEnabled = profileName.isNotBlank() && !appState.loading,
        snackbarHostState = snackbarHostState,
        onSave = onSave,
        onBack = onBack,
    ) { contentPadding ->
        ProfileEditorForm(
            appState = appState,
            scaffoldPadding = contentPadding,
            profileName = profileName,
            onProfileNameChange = onProfileNameChange,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun ProfileEditorScaffold(
    title: String,
    saveEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onSave: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSave, enabled = saveEnabled) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = stringResource(R.string.save_profile),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}
