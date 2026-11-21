package com.drakahell.claudesearch.ui.trace

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drakahell.claudesearch.R
import com.drakahell.claudesearch.data.ai.SearchFailure

/** Callbacks of the search panel, grouped to keep signatures short. */
data class SearchPanelActions(
    val onClearSelection: () -> Unit,
    val onAskFollowUp: (String) -> Unit,
    val onRetry: () -> Unit,
    /** Show Google Lens results inside the panel. */
    val onShowLens: () -> Unit,
    /** Hand the selection over to the Google app. */
    val onOpenGoogleApp: () -> Unit,
)

/** Drag distance (px) that expands, collapses or dismisses the sheet. */
private const val DRAG_THRESHOLD_PX = 60f

/**
 * Bottom sheet with the selected fragment and the Gemini answer or Google Lens results.
 * Drag the handle up to expand, down to collapse; dragging down a collapsed sheet
 * removes the selection.
 */
@Composable
fun SearchPanel(
    selection: TraceSelection?,
    actions: SearchPanelActions,
    screenHeight: Dp,
    onHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep showing the last selection while the panel slides out.
    val lastSelection = remember { mutableStateOf<TraceSelection?>(null) }
    if (selection != null) lastSelection.value = selection

    AnimatedVisibility(
        visible = selection != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        val shown = lastSelection.value ?: return@AnimatedVisibility
        // A new selection always starts collapsed.
        var expanded by rememberSaveable(shown.bounds.toString()) { mutableStateOf(false) }

        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { onHeightChanged(it.height) },
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                DragHandle(
                    onExpand = { expanded = true },
                    onCollapse = {
                        if (expanded) expanded = false else actions.onClearSelection()
                    },
                    onToggle = { expanded = !expanded },
                )
                Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                    PanelHeader(shown, actions)
                    Spacer(Modifier.height(12.dp))
                    when (val search = shown.search) {
                        SearchUiState.Idle -> LinearProgressIndicator(Modifier.fillMaxWidth())
                        is SearchUiState.Gemini -> GeminiContent(
                            search = search,
                            actions = actions,
                            maxAnswerHeight = if (expanded) screenHeight * 0.7f else 320.dp,
                        )
                        is SearchUiState.Lens -> LensContent(
                            search = search,
                            crop = shown,
                            height = if (expanded) screenHeight * 0.82f else screenHeight * 0.48f,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DragHandle(onExpand: () -> Unit, onCollapse: () -> Unit, onToggle: () -> Unit) {
    var dragged by remember { mutableFloatStateOf(0f) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .draggable(
                state = rememberDraggableState { delta -> dragged += delta },
                orientation = Orientation.Vertical,
                onDragStarted = { dragged = 0f },
                onDragStopped = {
                    when {
                        dragged < -DRAG_THRESHOLD_PX -> onExpand()
                        dragged > DRAG_THRESHOLD_PX -> onCollapse()
                    }
                },
            )
            .clickable(onClick = onToggle),
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
        )
    }
}

@Composable
private fun PanelHeader(selection: TraceSelection, actions: SearchPanelActions) {
    val isLens = selection.search is SearchUiState.Lens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            bitmap = selection.crop,
            contentDescription = stringResource(R.string.trace_crop_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(if (isLens) R.string.provider_google_lens else R.string.provider_gemini),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (isLens) {
            IconButton(onClick = actions.onOpenGoogleApp) {
                Icon(
                    painter = painterResource(R.drawable.ic_open_in_new),
                    contentDescription = stringResource(R.string.search_open_google_app),
                )
            }
        }
        TextButton(onClick = actions.onClearSelection) {
            Text(stringResource(R.string.trace_reset))
        }
    }
}

@Composable
private fun GeminiContent(
    search: SearchUiState.Gemini,
    actions: SearchPanelActions,
    maxAnswerHeight: Dp,
) {
    val scrollState = rememberScrollState()
    val animatedMax by animateDpAsState(maxAnswerHeight, label = "answerHeight")
    // Follow the answer while it is being written.
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.maxValue }.collect { scrollState.animateScrollTo(it) }
    }

    Column(
        modifier = Modifier
            .heightIn(max = animatedMax)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        search.turns.forEach { turn ->
            turn.question?.let { question ->
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            if (turn.answer.isNotEmpty()) {
                Text(text = turn.answer, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (search.isStreaming && search.turns.lastOrNull()?.answer.isNullOrEmpty()) {
            Text(
                text = stringResource(R.string.search_thinking),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        search.failure?.let { FailureContent(it, actions) }
    }

    if (search.failure == null && search.turns.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        FollowUpInput(enabled = !search.isStreaming, onSend = actions.onAskFollowUp)
    }
}

@Composable
private fun FailureContent(failure: SearchFailure, actions: SearchPanelActions) {
    val message = when (failure) {
        SearchFailure.QUOTA -> R.string.search_error_quota
        SearchFailure.REGION -> R.string.search_error_region
        SearchFailure.ACCESS_DENIED -> R.string.search_error_access_denied
        SearchFailure.NO_INTERNET -> R.string.search_error_no_internet
        SearchFailure.BLOCKED -> R.string.search_error_blocked
        SearchFailure.OTHER -> R.string.search_error_other
    }
    val canRetry = failure == SearchFailure.NO_INTERNET || failure == SearchFailure.OTHER
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (canRetry) {
                OutlinedButton(onClick = actions.onRetry) {
                    Text(stringResource(R.string.search_retry))
                }
            }
            FilledTonalButton(onClick = actions.onShowLens) {
                Text(stringResource(R.string.search_search_with_lens))
            }
        }
    }
}

@Composable
private fun FollowUpInput(enabled: Boolean, onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val send = {
        if (text.isNotBlank()) {
            onSend(text)
            text = ""
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        enabled = enabled,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_follow_up_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { send() }),
        trailingIcon = {
            IconButton(onClick = send, enabled = enabled && text.isNotBlank()) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = stringResource(R.string.search_send),
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LensContent(search: SearchUiState.Lens, crop: TraceSelection, height: Dp) {
    val context = LocalContext.current
    val animatedHeight by animateDpAsState(height, label = "lensHeight")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val note = when (search.reason) {
            LensReason.GEMINI_QUOTA -> stringResource(
                R.string.search_lens_quota_fallback,
                formatPauseEnd(context, search.geminiPausedUntilMillis),
            )
            LensReason.GEMINI_UNAVAILABLE -> stringResource(R.string.search_lens_unavailable_fallback)
            else -> null
        }
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LensWebView(
            crop = crop.crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .clip(RoundedCornerShape(16.dp)),
        )
    }
}

/** "14:05" for today, "22 нояб., 00:00" otherwise. */
fun formatPauseEnd(context: Context, epochMillis: Long): String {
    var flags = DateUtils.FORMAT_SHOW_TIME
    if (!DateUtils.isToday(epochMillis)) {
        flags = flags or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    }
    return DateUtils.formatDateTime(context, epochMillis, flags)
}
