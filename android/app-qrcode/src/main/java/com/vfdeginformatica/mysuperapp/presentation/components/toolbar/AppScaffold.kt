package com.vfdeginformatica.mysuperapp.presentation.components.toolbar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    canNavigateUp: Boolean,
    customizedNavigateUpIcon: ImageVector? = null,
    onNavigateUp: (() -> Unit)? = null,
    toolbarActions: @Composable RowScope.() -> Unit = {},
    snackBarHostState: SnackbarHostState,
    floatingActionButtonOnClick: (() -> Unit)? = null,
    floatingActionButtonExpanded: Boolean = true,
    floatingActionButtonIcon: ImageVector? = null,
    floatingActionButtonLabel: String? = null,
    onResume: () -> Unit = {},
    onStart: () -> Unit = {},
    content: @Composable (innerPadding: PaddingValues) -> Unit,
) {

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_START -> onStart()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                title = title,
                onNavigateUp = if (canNavigateUp) onNavigateUp else null,
                customizedNavigateUpIcon = if (canNavigateUp) customizedNavigateUpIcon else null,
                actions = toolbarActions,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (floatingActionButtonOnClick != null) {
                ExtendedFloatingActionButton(
                    expanded = floatingActionButtonExpanded,
                    onClick = floatingActionButtonOnClick,
                    icon = {
                        Icon(
                            floatingActionButtonIcon ?: Icons.Filled.Add,
                            contentDescription = floatingActionButtonLabel
                                ?: "floating action button icon"
                        )
                    },
                    text = { Text(floatingActionButtonLabel ?: "Novo") },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { innerPadding ->
        content(innerPadding)
    }
}