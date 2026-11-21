package com.drakahell.claudesearch.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drakahell.claudesearch.R
import com.drakahell.claudesearch.ui.theme.ClaudeSearchTheme
import com.drakahell.claudesearch.ui.trace.TraceActivity
import com.drakahell.claudesearch.ui.trace.formatPauseEnd
import com.drakahell.claudesearch.ui.settings.SettingsActivity
import com.drakahell.claudesearch.data.settings.SearchProvider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource

/** Screen entry point: collects state from the ViewModel and passes it to the UI. */
@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // System Photo Picker: no storage permission needed.
    val pickImage = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) context.startActivity(TraceActivity.imageIntent(context, uri))
    }

    HomeScreen(
        uiState = uiState,
        onPickPhotoClick = {
            pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        },
        onUseSampleClick = { context.startActivity(TraceActivity.sampleIntent(context)) },
        onSettingsClick = { context.startActivity(SettingsActivity.intent(context)) },
    )
}

/** Stateless screen: only renders [uiState]. Easy to preview and test. */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPickPhotoClick: () -> Unit,
    onUseSampleClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(onClick = onSettingsClick, modifier = Modifier.align(Alignment.End)) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.home_settings),
                )
            }
            HeroBadge()
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onPickPhotoClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(stringResource(R.string.home_pick_photo))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onUseSampleClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(stringResource(R.string.home_use_sample))
            }

            Spacer(Modifier.height(40.dp))
            Text(
                text = stringResource(R.string.home_setup_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusCard(
                    title = stringResource(R.string.home_assistant_title),
                    description = stringResource(
                        if (uiState.isAssistantEnabled) R.string.home_assistant_enabled
                        else R.string.home_assistant_disabled
                    ),
                    isOk = uiState.isAssistantEnabled,
                )
                val pausedUntil = uiState.geminiPausedUntilMillis
                StatusCard(
                    title = stringResource(R.string.home_search_title),
                    description = when {
                        uiState.preferredProvider == SearchProvider.GOOGLE_LENS ->
                            stringResource(R.string.home_search_lens)
                        pausedUntil != null -> stringResource(
                            R.string.home_search_gemini_paused,
                            formatPauseEnd(LocalContext.current, pausedUntil),
                        )
                        else -> stringResource(R.string.home_search_gemini)
                    },
                    isOk = true,
                    onClick = onSettingsClick,
                )
            }
        }
    }
}

/** Round gradient badge used as a placeholder logo on the home screen. */
@Composable
private fun HeroBadge() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(
                brush = Brush.sweepGradient(
                    listOf(colors.primary, colors.tertiary, colors.secondary, colors.primary)
                ),
                shape = CircleShape,
            )
            .padding(6.dp)
            .background(colors.surface, CircleShape),
    )
}

@Composable
private fun StatusCard(
    title: String,
    description: String,
    isOk: Boolean,
    onClick: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            // Non-clickable cards are "disabled" but must look the same.
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(
                        color = if (isOk) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
            )
            Spacer(Modifier.size(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ClaudeSearchTheme {
        HomeScreen(
            uiState = HomeUiState(),
            onPickPhotoClick = {},
            onUseSampleClick = {},
            onSettingsClick = {},
        )
    }
}
