package com.drakahell.claudesearch.ui.trace

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import android.widget.Toast
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.drakahell.claudesearch.data.lens.GoogleLens
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drakahell.claudesearch.R
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** Own accent palette for the selection glow and the trace stroke. */
private val GlowColors = listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF), Color(0xFFFF4081))

@Composable
fun TraceRoute(viewModel: TraceViewModel, onClose: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val actions = remember(viewModel) {
        SearchPanelActions(
            onClearSelection = viewModel::clearSelection,
            onAskFollowUp = viewModel::askFollowUp,
            onRetry = viewModel::retry,
            onShowLens = viewModel::showLens,
            onOpenGoogleApp = {
                val crop = (viewModel.uiState.value as? TraceUiState.Ready)?.selection?.crop
                if (crop != null) {
                    scope.launch {
                        val result = GoogleLens.open(context, crop.asAndroidBitmap())
                        if (result == GoogleLens.Result.FAILED) {
                            Toast.makeText(context, R.string.search_lens_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
        )
    }

    TraceScreen(
        uiState = uiState,
        onSelectionMade = viewModel::onSelectionMade,
        actions = actions,
        onClose = onClose,
    )
}

@Composable
fun TraceScreen(
    uiState: TraceUiState,
    onSelectionMade: (Rect) -> Unit,
    actions: SearchPanelActions,
    onClose: () -> Unit,
) {
    val selection = (uiState as? TraceUiState.Ready)?.selection

    // "Back" first removes the selection, the second "Back" closes the screen.
    BackHandler(enabled = selection != null, onBack = actions.onClearSelection)

    // Height of the bottom sheet, used to move the image so the selection stays visible.
    var panelHeightPx by remember { mutableIntStateOf(0) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenHeight = maxHeight
        when (uiState) {
            TraceUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is TraceUiState.Error -> Text(
                text = stringResource(uiState.messageRes),
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            is TraceUiState.Ready -> TraceCanvas(
                image = uiState.image,
                selection = uiState.selection?.bounds,
                bottomInsetPx = if (selection != null) panelHeightPx.toFloat() else 0f,
                onSelectionMade = onSelectionMade,
            )
        }

        TopBar(
            showHint = uiState is TraceUiState.Ready && selection == null,
            onClose = onClose,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        SearchPanel(
            selection = selection,
            actions = actions,
            screenHeight = screenHeight,
            onHeightChanged = { panelHeightPx = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Shows the image and handles the finger: draws a glowing stroke while tracing,
 * then dims everything outside the selection and outlines it with an animated border.
 */
@Composable
private fun TraceCanvas(
    image: ImageBitmap,
    selection: Rect?,
    bottomInsetPx: Float,
    onSelectionMade: (Rect) -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val tapSizePx = with(density) { 140.dp.toPx() }
    val paddingPx = with(density) { 12.dp.toPx() }

    var containerSize by remember { mutableStateOf(Size.Zero) }
    val fit = remember(containerSize, image) {
        if (containerSize.width <= 0f || containerSize.height <= 0f) null
        else ImageFit.fit(containerSize, IntSize(image.width, image.height))
    }
    // The gesture loop is started once, so it must always read the latest values.
    val currentFit by rememberUpdatedState(fit)
    val currentOnSelectionMade by rememberUpdatedState(onSelectionMade)

    val stroke = remember { mutableStateListOf<Offset>() }
    val strokeAlpha = remember { Animatable(1f) }

    val selectionProgress = remember { Animatable(0f) }
    LaunchedEffect(selection) {
        if (selection != null) {
            selectionProgress.snapTo(0f)
            selectionProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            )
        } else {
            selectionProgress.snapTo(0f)
        }
    }

    val glowAngle by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 4000, easing = LinearEasing)),
        label = "glowAngle",
    )

    // Slide the image up when the bottom sheet would cover the selection.
    val marginPx = with(density) { 24.dp.toPx() }
    val topReservedPx = with(density) { 72.dp.toPx() }
    val targetShift = remember(selection, fit, bottomInsetPx, containerSize) {
        if (selection == null || fit == null || bottomInsetPx <= 0f) {
            0f
        } else {
            SelectionMath.verticalShiftToReveal(
                selectionInView = fit.imageToView(selection),
                containerHeight = containerSize.height,
                bottomInset = bottomInsetPx + marginPx,
                topReserved = topReservedPx,
            )
        }
    }
    val shift by animateFloatAsState(targetShift, label = "imageShift")

    Box(
        Modifier
            .fillMaxSize()
            // Placed before pointerInput, so touches are mapped back to unshifted coordinates.
            .graphicsLayer { translationY = shift }
            .onSizeChanged { containerSize = it.toSize() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    scope.launch { strokeAlpha.snapTo(1f) }
                    stroke.clear()
                    stroke.add(down.position)

                    // Follow the first finger until it is lifted.
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        stroke.add(change.position)
                        change.consume()
                        if (!change.pressed) break
                    }

                    val fitNow = currentFit
                    val rect = fitNow?.let {
                        SelectionMath.selectionFromStroke(
                            points = stroke.toList(),
                            fit = it,
                            touchSlopPx = viewConfiguration.touchSlop,
                            tapSizePx = tapSizePx,
                            paddingPx = paddingPx,
                        )
                    }
                    if (rect != null) {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        currentOnSelectionMade(rect)
                    }
                    scope.launch {
                        strokeAlpha.animateTo(0f, tween(durationMillis = 450))
                        stroke.clear()
                    }
                }
            }
    ) {
        // Layer 1: the image itself.
        Canvas(Modifier.fillMaxSize()) {
            val f = fit ?: return@Canvas
            val dst = f.displayedRect
            drawImage(
                image = image,
                dstOffset = IntOffset(dst.left.roundToInt(), dst.top.roundToInt()),
                dstSize = IntSize(dst.width.roundToInt(), dst.height.roundToInt()),
                filterQuality = FilterQuality.High,
            )
        }

        // Layer 2: dimming, selection outline and the finger stroke.
        // Offscreen compositing is required so BlendMode.Clear cuts a hole only in this layer.
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val f = fit ?: return@Canvas
            if (selection != null) {
                drawSelection(
                    target = f.imageToView(selection),
                    progress = selectionProgress.value,
                    angleDegrees = glowAngle,
                )
            }
            if (stroke.size > 1) {
                drawStroke(stroke, strokeAlpha.value, glowAngle)
            }
        }
    }
}

private fun DrawScope.drawSelection(target: Rect, progress: Float, angleDegrees: Float) {
    val p = progress.coerceIn(0f, 1.2f)
    // The frame "pops" from 92% to 100% of its size.
    val scale = 0.92f + 0.08f * p
    val rect = Rect(
        left = target.center.x - target.width * scale / 2f,
        top = target.center.y - target.height * scale / 2f,
        right = target.center.x + target.width * scale / 2f,
        bottom = target.center.y + target.height * scale / 2f,
    )
    val alpha = p.coerceIn(0f, 1f)
    val corner = CornerRadius(20.dp.toPx())

    drawRect(Color.Black.copy(alpha = 0.55f * alpha))
    drawRoundRect(
        color = Color.Transparent,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = corner,
        blendMode = BlendMode.Clear,
    )

    val brush = rotatingGradient(rect.center, max(rect.width, rect.height) / 2f, angleDegrees)
    drawRoundRect(
        brush = brush,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = corner,
        alpha = 0.35f * alpha,
        style = Stroke(width = 10.dp.toPx()),
    )
    drawRoundRect(
        brush = brush,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = corner,
        alpha = alpha,
        style = Stroke(width = 3.dp.toPx()),
    )
}

private fun DrawScope.drawStroke(points: List<Offset>, alpha: Float, angleDegrees: Float) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    val brush = rotatingGradient(center, size.minDimension / 2f, angleDegrees)
    // Wide translucent glow underneath, thin bright line on top.
    drawPath(
        path = path,
        brush = brush,
        alpha = 0.35f * alpha,
        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = path,
        color = Color.White,
        alpha = alpha,
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** Linear gradient whose direction rotates around [center] — gives a "flowing" glow. */
private fun rotatingGradient(center: Offset, radius: Float, angleDegrees: Float): Brush {
    val rad = Math.toRadians(angleDegrees.toDouble())
    val dir = Offset(cos(rad).toFloat(), sin(rad).toFloat()) * radius.coerceAtLeast(1f)
    return Brush.linearGradient(
        colors = GlowColors,
        start = center - dir,
        end = center + dir,
        tileMode = TileMode.Mirror,
    )
}

@Composable
private fun TopBar(showHint: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClose,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.trace_close),
            )
        }
        Spacer(Modifier.width(12.dp))
        AnimatedVisibility(visible = showHint, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = stringResource(R.string.trace_hint),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

