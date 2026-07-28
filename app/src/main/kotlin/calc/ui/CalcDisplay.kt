package calc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Deliberately NOT animating digit interpolation on `result` yet —
 * §5 calls for "Animated with digit interpolation," and §18's
 * Execution Pulse (expression fade 80ms -> result transition 180ms ->
 * green sweep 220ms -> haptic) is the fuller animation sequence this
 * needs to participate in. That's cross-cutting across this
 * composable, CalcScreen's status handling, and CalcViewModel's
 * COMPUTING window — building it as its own follow-up rather than a
 * partial, disconnected animation bolted onto just this file.
 *
 * expression/result are plain strings, not sourced directly from
 * CalcViewModel here — this composable takes only what it needs to
 * render, so it stays trivially previewable/testable without a
 * ViewModel or AppContainer in scope.
 */
@Composable
fun CalcDisplay(
    expression: String,
    result: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(192.dp)
            .padding(horizontal = NeonSpacing.MarginHorizontal),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        // Expression: 16sp, muted color, right-aligned (§5).
        Text(
            text = expression,
            fontSize = 16.sp,
            color = NeonDark.TextMuted,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
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
    }
}
