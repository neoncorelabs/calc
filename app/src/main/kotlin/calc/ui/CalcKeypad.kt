package calc.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neoncore.theme.NeonAccent
import neoncore.theme.NeonDark
import neoncore.theme.NeonMotion

/**
 * The calculator keypad (CALC-UI-01 §6): 4 columns x 5 rows, 72x72dp
 * buttons, 8dp spacing, 16dp radius, three visual variants (Number,
 * Operator, Equals). Grid content exactly matches §4's wireframe:
 *
 *   AC   ±    %    ÷
 *   7    8    9    ×
 *   4    5    6    −
 *   1    2    3    +
 *        0    .    =
 *
 * All button glyphs sent via onKeyOrAction are the EXACT characters
 * calc.engine.Lexer already tokenizes natively (verified against
 * Lexer.kt before writing this: '±', '%', '×', '÷', '−', '+' are all
 * literal cases in the lexer's `when` block) — so this composable
 * does no translation, just forwards the glyph as typed.
 *
 * "AC" and "=" are not expression characters — they map to
 * onClear()/onEquals() respectively, handled via the sealed
 * CalcKeyAction below rather than being sent through onKeyOrAction as
 * text.
 */
sealed class CalcKeyAction {
    data class Insert(val text: String) : CalcKeyAction()
    data object Clear : CalcKeyAction()
    data object Equals : CalcKeyAction()
}

private data class KeySpec(
    val label: String,
    val action: CalcKeyAction,
    val variant: CalcKeyVariant
)

private enum class CalcKeyVariant { NUMBER, OPERATOR, EQUALS }

@Composable
fun CalcKeypad(
    onAction: (CalcKeyAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // Row-by-row per §4's wireframe. The last row is 3 cells wide
    // (0, ., =) with "=" taking the remaining width rather than a
    // single grid cell — §6 says Equals is "Entire width... 152dp",
    // read as: within its row, Equals fills whatever width the row
    // has left after "0" and "." each take one normal button cell.
    // (152dp is roughly 2 button cells (72+72) plus the 8dp gap
    // between them, i.e. two grid columns' worth of width — consistent
    // with "0" and "." occupying the first column each and "=" filling
    // the remaining two.)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CalcKeyRow(listOf(
            KeySpec("AC", CalcKeyAction.Clear, CalcKeyVariant.NUMBER),
            KeySpec("±", CalcKeyAction.Insert("±"), CalcKeyVariant.NUMBER),
            KeySpec("%", CalcKeyAction.Insert("%"), CalcKeyVariant.NUMBER),
            KeySpec("÷", CalcKeyAction.Insert("÷"), CalcKeyVariant.OPERATOR)
        ), onAction)

        CalcKeyRow(listOf(
            KeySpec("7", CalcKeyAction.Insert("7"), CalcKeyVariant.NUMBER),
            KeySpec("8", CalcKeyAction.Insert("8"), CalcKeyVariant.NUMBER),
            KeySpec("9", CalcKeyAction.Insert("9"), CalcKeyVariant.NUMBER),
            KeySpec("×", CalcKeyAction.Insert("×"), CalcKeyVariant.OPERATOR)
        ), onAction)

        CalcKeyRow(listOf(
            KeySpec("4", CalcKeyAction.Insert("4"), CalcKeyVariant.NUMBER),
            KeySpec("5", CalcKeyAction.Insert("5"), CalcKeyVariant.NUMBER),
            KeySpec("6", CalcKeyAction.Insert("6"), CalcKeyVariant.NUMBER),
            KeySpec("−", CalcKeyAction.Insert("−"), CalcKeyVariant.OPERATOR)
        ), onAction)

        CalcKeyRow(listOf(
            KeySpec("1", CalcKeyAction.Insert("1"), CalcKeyVariant.NUMBER),
            KeySpec("2", CalcKeyAction.Insert("2"), CalcKeyVariant.NUMBER),
            KeySpec("3", CalcKeyAction.Insert("3"), CalcKeyVariant.NUMBER),
            KeySpec("+", CalcKeyAction.Insert("+"), CalcKeyVariant.OPERATOR)
        ), onAction)

        // Final row: "0" and "." are normal single-cell number
        // buttons; "=" fills the remaining two-column width.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalcKeyButton(
                spec = KeySpec("0", CalcKeyAction.Insert("0"), CalcKeyVariant.NUMBER),
                onAction = onAction,
                modifier = Modifier.size(72.dp)
            )
            CalcKeyButton(
                spec = KeySpec(".", CalcKeyAction.Insert("."), CalcKeyVariant.NUMBER),
                onAction = onAction,
                modifier = Modifier.size(72.dp)
            )
            CalcKeyButton(
                spec = KeySpec("=", CalcKeyAction.Equals, CalcKeyVariant.EQUALS),
                onAction = onAction,
                // Two cells (72+72) plus the 8dp gap between them —
                // matches §6's 152dp Equals width exactly.
                modifier = Modifier
                    .width(152.dp)
                    .height(72.dp)
            )
        }
    }
}

@Composable
private fun CalcKeyRow(keys: List<KeySpec>, onAction: (CalcKeyAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { spec ->
            CalcKeyButton(
                spec = spec,
                onAction = onAction,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

/**
 * A single keypad button. Variant-specific styling per §6:
 *
 * NUMBER: Card surface, 24sp medium text. Pressed: surface rises 2dp
 *   (NeonElevation.Surface1's 2dp offset — visually approximated here
 *   via a border rather than a real shadow/elevation change, see NOTE
 *   below) + green border, 120ms.
 * OPERATOR: Background2 surface, green text. Pressed: green fills the
 *   button entirely.
 * EQUALS: Magenta background, white icon/text. Pressed: magenta
 *   darkens + green edge flash.
 *
 * NOTE on "surface rises 2dp": true Compose elevation (tonal/shadow)
 * reads as a heavier visual effect than this spec's restrained,
 * no-heavy-shadows design principle wants (NEON//CORE's own Elevation
 * doc: "avoids heavy shadows... layered surfaces instead," "No
 * colored shadows. Ever."). Implemented the pressed number-button
 * state as the green border appearing (which the spec also calls for
 * in the same breath) without a literal Modifier.shadow — a visible
 * lift effect without violating the no-heavy-shadow principle. If a
 * literal 2dp elevation is wanted instead, swap the Box for a
 * Surface(tonalElevation = ...) here.
 */
@Composable
private fun CalcKeyButton(
    spec: KeySpec,
    onAction: (CalcKeyAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when (spec.variant) {
            CalcKeyVariant.NUMBER -> NeonDark.Card
            CalcKeyVariant.OPERATOR -> if (isPressed) NeonAccent.Green else NeonDark.Background2
            CalcKeyVariant.EQUALS -> if (isPressed) darkenMagenta() else NeonAccent.Magenta
        },
        animationSpec = tween(durationMillis = NeonMotion.PRESS_DURATION_MS),
        label = "keyBackground"
    )

    val textColor = when (spec.variant) {
        CalcKeyVariant.NUMBER -> NeonDark.TextPrimary
        CalcKeyVariant.OPERATOR -> if (isPressed) NeonDark.Background0 else NeonAccent.Green
        CalcKeyVariant.EQUALS -> Color.White
    }

    val borderColor = when {
        spec.variant == CalcKeyVariant.NUMBER && isPressed -> NeonAccent.Green
        spec.variant == CalcKeyVariant.EQUALS && isPressed -> NeonAccent.Green
        else -> Color.Transparent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(16.dp))
            .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = spec.label
            ) { onAction(spec.action) }
            .padding(4.dp)
    ) {
        Text(
            text = spec.label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/** Equals button's pressed-state darkened magenta (§6: "Magenta darkens"). */
private fun darkenMagenta(): Color {
    val base = NeonAccent.Magenta
    return Color(
        red = base.red * 0.8f,
        green = base.green * 0.8f,
        blue = base.blue * 0.8f,
        alpha = base.alpha
    )
}
