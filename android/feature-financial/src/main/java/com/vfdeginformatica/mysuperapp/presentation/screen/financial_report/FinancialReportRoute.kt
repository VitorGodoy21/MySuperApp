package com.vfdeginformatica.mysuperapp.presentation.screen.financial_report

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.FinancialReportEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.FinancialReportEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial_report.contract.ReportTab

@Composable
fun FinancialReportRoute(
    viewModel: FinancialReportViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FinancialReportEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
                is FinancialReportEffect.ShareText -> shareText(context, effect.subject, effect.text)
                is FinancialReportEffect.CopyText -> copyText(context, effect.text)
            }
        }
    }

    val showTextActions = state.selectedTab == ReportTab.TEXT && state.activeNarrative != null

    AppScaffold(
        title = "Relatório mensal",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost,
        toolbarActions = {
            if (showTextActions) {
                IconButton(onClick = { viewModel.onEvent(FinancialReportEvent.OnCopyClicked) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar texto")
                }
                IconButton(onClick = { viewModel.onEvent(FinancialReportEvent.OnShareClicked) }) {
                    Icon(Icons.Default.Share, contentDescription = "Compartilhar texto")
                }
            }
        },
    ) { padding ->
        FinancialReportScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding),
        )
    }
}

private fun shareText(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Relatório financeiro", text))
}
