package com.drakahell.claudesearch.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drakahell.claudesearch.R
import com.drakahell.claudesearch.data.settings.SearchProvider
import com.drakahell.claudesearch.ui.theme.ClaudeSearchTheme
import com.drakahell.claudesearch.ui.trace.formatPauseEnd

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onProviderSelected = viewModel::onProviderSelected,
        onResumeGemini = viewModel::onResumeGemini,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onProviderSelected: (SearchProvider) -> Unit,
    onResumeGemini: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(vertical = 8.dp)
        ) {
            SectionTitle(stringResource(R.string.settings_search_section))
            Column(Modifier.selectableGroup()) {
                ProviderOption(
                    title = stringResource(R.string.provider_gemini),
                    description = stringResource(R.string.settings_gemini_description),
                    selected = uiState.preferredProvider == SearchProvider.GEMINI,
                    onClick = { onProviderSelected(SearchProvider.GEMINI) },
                )
                ProviderOption(
                    title = stringResource(R.string.provider_google_lens),
                    description = stringResource(R.string.settings_lens_description),
                    selected = uiState.preferredProvider == SearchProvider.GOOGLE_LENS,
                    onClick = { onProviderSelected(SearchProvider.GOOGLE_LENS) },
                )
            }
            val pausedUntil = uiState.geminiPausedUntilMillis
            if (uiState.preferredProvider == SearchProvider.GEMINI && pausedUntil != null) {
                PausedCard(pausedUntil, onResumeGemini)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
private fun ProviderOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // The whole row is clickable, so the radio button itself has no click handler.
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PausedCard(pausedUntilMillis: Long, onResumeGemini: () -> Unit) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_gemini_paused, formatPauseEnd(context, pausedUntilMillis)),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onResumeGemini, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.settings_resume_gemini))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ClaudeSearchTheme {
        SettingsScreen(
            uiState = SettingsUiState(),
            onBack = {},
            onProviderSelected = {},
            onResumeGemini = {},
        )
    }
}
