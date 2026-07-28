package calc.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neoncore.theme.NeonDark
import neoncore.theme.NeonSpacing

/**
 * The Display Area (CALC-UI-01 §5): fixed 192dp height, right-aligned
 * expression above the result.
 *
 * `pulseTrigger` drives the Execution Pulse (§18): an ever-increasing
 * counter, expected to be incremented by the caller once per
 * successful `=` (COMPUTING -> READY only — CalcScreen owns detecting
 * that transition, this composable just reacts to the counter
 * changing). Using a counter rather than a Boolean means two
 * completions in a row each get their own full animation, rather than
 * a second completion silently no-oping because a boolean "didn't
 * change." Same pattern as ExecutionSweepLine, which this composable
 * embeds directly beneath the result.
 *
 * Implements §18 steps 1-3 (the two ViewModel/status-driven steps —
 * 4, the status color change, and 5, the haptic — are CalcScreen's
 * responsibility, not this composable's, since they don't involve
 * anything Display renders):
 *   1. Expression fades to 60% opacity over 80ms.
 *   2. Result's numeric transition over 180ms — NOTE: implemented
 *      here as a plain, immediate text swap, NOT a true digit-by-digit
 *      interpolation (e.g. old digits sliding out, new digits sliding
 *      in per-position). §5 calls for "digit interpolation" and §18.2
 *      says "smooth numeric transition" — building genuine per-digit
 *      interpolation is a materially bigger effort (needs to diff old
 *      vs new result strings digit-by-digit and animate each
 *      position independently) that didn't fit this batch's scope.
 *      Flagging clearly rather than silently shipping a plain swap
 *      under a comment that implies more than it delivers.
 *   3. Sweep line beneath the result — delegated to ExecutionSweepLine.
 *
 * expression/result are still plain strings (unchanged from before
 * this batch) — this composable stays previewable/testable without a
 * ViewModel or AppContainer in scope.
 */
@Composable
fun CalcDisplay(
    expression: String,
    result: String,
    pulseTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val expressionAlpha = remember { Animatable(1f) }

    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger > 0) {
            expressionAlpha.snapTo(1f)
            expressionAlpha.animateTo(
                targetValue = ExecutionPulseMotion.EXPRESSION_FADE_TARGET_ALPHA,
                animationSpec = tween(durationMillis = ExecutionPulseMotion.EXPRESSION_FADE_MS)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(192.dp)
            .padding(horizontal = NeonSpacing.MarginHorizontal),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        // Expression: 16sp, muted color, right-aligned (§5).
        // Opacity driven by expressionAlpha (§18 step 1) — stays at
        // 1f (full opacity) until the first pulseTrigger fires, then
        // fades toward EXPRESSION_FADE_TARGET_ALPHA each time.
        Text(
            text = expression,
            fontSize = 16.sp,
            color = NeonDark.TextMuted,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(expressionAlpha.value)
        )

        // Result: 56sp, bold, primary text (§5).
        Text(
            text = result,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = NeonDark.TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )

        // §18 step 3: green sweep line beneath the result.
        ExecutionSweepLine(
            trigger = pulseTrigger,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
