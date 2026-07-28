package calc.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import neoncore.theme.NeonAccent

/**
 * CALC-specific motion durations for the Execution Pulse (CALC-UI-01
 * §18) — kept here in calc.ui, NOT added to neon-core's shared
 * NeonMotion object. NeonMotion is a shared module used across every
 * future NeonCoreLabs app; these three numbers (80/180/220ms) are
 * §18's specific animation for CALC's signature interaction, not a
 * general-purpose token another app would reuse. (180/220 happen to
 * numerically match NeonMotion.DIALOG_MS/DEFAULT_MS, but those are
 * named for unrelated purposes — reusing them under those names here
 * would be misleading, so these are defined fresh.)
 */
object ExecutionPulseMotion {
    /** Step 1: expression fades to 60% opacity (§18.1). */
    const val EXPRESSION_FADE_MS = 80

    /** Step 2: result's numeric transition (§18.2). */
    const val RESULT_TRANSITION_MS = 180

    /** Step 3: green sweep line beneath the result (§18.3). */
    const val SWEEP_MS = 220

    /** Opacity the expression fades TO (not from) during step 1. */
    const val EXPRESSION_FADE_TARGET_ALPHA = 0.6f
}

/**
 * The sweep-line visual from §18 step 3: "A 1.5dp neon-green line
 * sweeps from left to right beneath the result in 220ms, like a scan
 * completing."
 *
 * `trigger` is expected to be an ever-increasing key (e.g. a counter
 * incremented once per successful `=`) — each new value restarts the
 * sweep from the left edge. Using a counter rather than a Boolean
 * means two calculations completed back-to-back both get their own
 * full sweep, rather than a second completion silently no-oping
 * because the trigger "didn't change" from true to true.
 *
 * Drawn as a Box overlay with a fixed 1.5dp height rather than a
 * Canvas-only composable, so callers can just place this beneath
 * CalcDisplay's result Text with normal layout, no manual Canvas
 * sizing math needed at the call site.
 */
@Composable
fun ExecutionSweepLine(
    trigger: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            // snapTo(0f) first: guarantees a full left-to-right sweep
            // even if this fires again before the previous sweep
            // finished animating (e.g. two = presses in quick
            // succession) — without the snap, animateTo from
            // wherever progress currently sits would produce a
            // partial, visually inconsistent sweep on the second
            // trigger.
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ExecutionPulseMotion.SWEEP_MS,
                    easing = LinearEasing
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .drawWithContent {
                // Sweep left-to-right: draw a line whose visible
                // length grows with progress, rather than animating
                // full-width alpha (which would read as a fade, not a
                // "scan completing" per §18's own description).
                val lineY = size.height / 2
                val endX = size.width * progress.value
                drawLine(
                    color = NeonAccent.Green,
                    start = Offset(0f, lineY),
                    end = Offset(endX, lineY),
                    strokeWidth = size.height,
                    cap = StrokeCap.Butt
                )
            }
    )
}
