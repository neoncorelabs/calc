package calc.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neoncore.theme.NeonAccent
import neoncore.theme.NeonDark
import neoncore.theme.NeonMotion

/**
 * Scientific Mode's additional function keypad (CALC-UI-01 §9).
 *
 * v1 scope is basic scientific only — trig/log (sin/cos/tan/ln/log)
 * are cut per the doc's own scope note. Retained functions, exactly
 * as listed in §9: π, √, e, x², xʸ, (), mod, !. That's 8 functions,
 * not the original 12 the spec's grid note assumed, so this renders
 * as a 4x2 grid (per §9's own note: "With 8 functions remaining,
 * this becomes a 4×2 grid — implementer should confirm final
 * arrangement keeps consistent spacing... rather than leaving a
 * half-empty row." A 4x2 grid with 8 items has no empty cells, so
 * that's the arrangement used here) rather than 4x3 with 4 empty
 * cells.
 *
 * Buttons are 64dp per §9 ("Scientific buttons / Smaller / 64dp"),
 * smaller than the Home Screen keypad's 72dp NUMBER/OPERATOR buttons,
 * but otherwise reuse CalcKeypad's OPERATOR visual language (green
 * text/border, Background2 surface) since scientific functions are
 * operators/functions in the same sense ×, ÷, mod already are, and
 * §9 doesn't call for a distinct third visual variant just for this
 * row. Spacing is NeonSpacing.Tight (8dp) between cells, same gap
 * CalcKeypad already uses ("Still aligned" per §9 — i.e. this grid
 * should visually align with the Home Screen keypad's columns, so it
 * reuses the same 8dp gap rather than inventing a different one).
 *
 * Glyph-to-token mapping (all verified against calc.engine.Lexer's
 * `when` block before writing this, same verification CalcKeypad's
 * own doc comment already establishes as this project's convention):
 *   π  -> literal 'π'          (Token.Constant(PI))
 *   √  -> literal '√'          (Token.Function(SQRT); needs its own
 *                               following '(' from the user, same as
 *                               any function call per CALC-ENGINE-01
 *                               §3 grammar — this button inserts only
 *                               '√', not '√(', so the next keypress
 *                               is whatever the user types next,
 *                               consistent with every other button
 *                               here doing no multi-token insertion)
 *   e  -> literal 'e'          (Token.Constant(E))
 *   x² -> literal '²'          (Token.Square(), postfix)
 *   xʸ -> literal '^'          (Token.Operator(POWER) — '^' is the
 *                               lexer's actual POWER case; there is
 *                               no separate 'xʸ' character, ^ is what
 *                               CALC-ARCH-01/ENGINE docs already use
 *                               throughout for this operator)
 *   () -> literal '('          (Token.LeftParen(); see the doc
 *                               comment above the button spec list
 *                               below for why this is '(' only, not
 *                               a smart open/close toggle)
 *   mod -> literal "mod"       (Token.Operator(MOD); the lexer
 *                               matches the 3-char literal string
 *                               "mod" via input.startsWith("mod", i),
 *                               so this button must insert the whole
 *                               word, not a symbol)
 *   !  -> literal '!'          (Token.Factorial(), postfix)
 *
 * All of the above are inserted via the same CalcKeyAction.Insert
 * used by CalcKeypad's number/operator buttons — no new action type
 * needed, no new plumbing in CalcViewModel (onKeyPress already
 * accepts any raw token string and forwards to previewEvaluate/
 * EngineFacade, which already fully supports these tokens; engine
 * layer is unchanged by this batch, confirmed by reading
 * Lexer/Parser/Evaluator before writing this file — nothing here
 * required an engine change).
 *
 * ON THE "()" BUTTON — WHY IT INSERTS '(' ONLY, NOT A SMART TOGGLE:
 * §9 lists "()" as a single item, and neither CALC-UI-01 nor
 * CALC-ENGINE-01 defines any open/close-toggle behavior. Inventing
 * one would mean tracking unmatched-paren depth as new UI state
 * nothing else in this project does. CALC-ENGINE-01 §4.1 already
 * establishes the opposite design intent explicitly: "there's no way
 * to type `2(` without an explicit operator anyway, since `(` isn't
 * adjacent to a number key in the UI" — i.e. the keypad is meant to
 * stay dumb and let the engine handle smartness. §4.2's auto-close-
 * unmatched-parens-on-`=` behavior exists for exactly this reason:
 * so a plain '(' button, with no matching ')' button at all, is
 * sufficient for every case the v1 scope needs. A user who wants to
 * close a paren early simply can't with this keypad alone in v1 —
 * consistent with the engine doc's own framing, not a gap introduced
 * here. If a future session decides manual closing is needed, that's
 * a new decision, not something this batch should invent silently.
 */
@Composable
fun ScientificKeypad(
    onAction: (CalcKeyAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScientificKeyRow(listOf(
            ScientificKeySpec("π", CalcKeyAction.Insert("π")),
            ScientificKeySpec("√", CalcKeyAction.Insert("√")),
            ScientificKeySpec("e", CalcKeyAction.Insert("e")),
            ScientificKeySpec("x²", CalcKeyAction.Insert("²"))
        ), onAction)

        ScientificKeyRow(listOf(
            ScientificKeySpec("xʸ", CalcKeyAction.Insert("^")),
            ScientificKeySpec("()", CalcKeyAction.Insert("(")),
            ScientificKeySpec("mod", CalcKeyAction.Insert("mod")),
            ScientificKeySpec("!", CalcKeyAction.Insert("!"))
        ), onAction)
    }
}

private data class ScientificKeySpec(
    val label: String,
    val action: CalcKeyAction.Insert
)

@Composable
private fun ScientificKeyRow(keys: List<ScientificKeySpec>, onAction: (CalcKeyAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { spec ->
            ScientificKeyButton(
                spec = spec,
                onAction = onAction,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

/**
 * A single scientific function button, 64dp (§9). Visually reuses
 * CalcKeypad's OPERATOR variant styling (Background2 surface, green
 * text, green fill when pressed) rather than defining a fourth
 * distinct variant — §9 doesn't call for one, and these are all
 * operators/functions in the same visual category the Home Screen
 * keypad's ÷/×/−/+/mod buttons already occupy.
 */
@Composable
private fun ScientificKeyButton(
    spec: ScientificKeySpec,
    onAction: (CalcKeyAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) NeonAccent.Green else NeonDark.Background2,
        animationSpec = tween(durationMillis = NeonMotion.PRESS_DURATION_MS),
        label = "scientificKeyBackground"
    )

    val textColor = if (isPressed) NeonDark.Background0 else NeonAccent.Green

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = spec.label
            ) { onAction(spec.action) }
    ) {
        Text(
            text = spec.label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
