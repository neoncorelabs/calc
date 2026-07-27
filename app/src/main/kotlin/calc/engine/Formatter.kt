package calc.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Formats evaluation results for display per CALC-ENGINE-01 §5.
 * Internal computation stays full float64 precision (chaining continues
 * from the true result); only the *displayed* string is rounded here.
 */
object Formatter {

    private const val SIGNIFICANT_DIGITS = 12
    private const val SCI_UPPER_THRESHOLD = 1e12
    private const val SCI_LOWER_THRESHOLD = 1e-9

    /**
     * Formats an [EvalResult] into the exact string to show on screen.
     * Error cases map to the §8 display strings directly.
     */
    fun format(result: EvalResult): String {
        return when (result) {
            is EvalResult.Value -> formatValue(result.value)
            is EvalResult.Error -> when (result.kind) {
                ErrorKind.UNDEFINED -> "Undefined"
                ErrorKind.OVERFLOW -> "Overflow"
                ErrorKind.SYNTAX_ERROR -> "Syntax Error"
            }
            is EvalResult.Incomplete -> ""
        }
    }

    /**
     * Formats a raw Double per §5's rules. Exposed separately from
     * [format] in case callers already have a plain Double (e.g. from
     * a currency-converted standalone value) rather than an EvalResult.
     */
    fun formatValue(value: Double): String {
        if (value.isNaN()) return "Undefined"
        if (value.isInfinite()) return "Overflow"

        if (value == 0.0) return "0"

        val absValue = Math.abs(value)

        // Overflow: result magnitude exceeds float64 range (~1.8e308).
        // isInfinite() above already catches true overflow-to-Infinity,
        // this is a defensive secondary check against the documented
        // range boundary.
        if (absValue > Double.MAX_VALUE) return "Overflow"

        val useScientific = absValue >= SCI_UPPER_THRESHOLD || absValue < SCI_LOWER_THRESHOLD

        return if (useScientific) {
            formatScientific(value)
        } else {
            formatPlain(value)
        }
    }

    /**
     * Plain (non-scientific) formatting: round to 12 significant digits,
     * then suppress trailing zeros (and a trailing decimal point if the
     * result is a whole number).
     *
     * Uses BigDecimal.valueOf(value), NOT the BigDecimal(value) exact-double
     * constructor. The exact constructor preserves raw IEEE-754 binary
     * approximation noise (e.g. 1.2 is actually stored as
     * 1.1999999999999999555910790149937...), which can leak into the
     * rounded output for some magnitudes. valueOf() goes through
     * Double.toString() first, giving the canonical decimal representation
     * a user actually expects.
     */
    private fun formatPlain(value: Double): String {
        val bd = BigDecimal.valueOf(value).round(MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP))
        // stripTrailingZeros can produce scientific-notation-like unscaled
        // forms (e.g. 1E+2) for round numbers; toPlainString avoids that.
        var plain = bd.stripTrailingZeros().toPlainString()

        // stripTrailingZeros + toPlainString on an integer-valued BigDecimal
        // is already clean (e.g. "120"), but guard against a stray "-0".
        if (plain == "-0") plain = "0"

        return plain
    }

    /**
     * Scientific notation per §5: "1.23456789012 × 10¹²" style, rounded
     * to 12 significant digits, trailing zeros in the mantissa suppressed.
     * Same BigDecimal.valueOf() rationale as formatPlain() above.
     */
    private fun formatScientific(value: Double): String {
        val bd = BigDecimal.valueOf(value).round(MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP))
        val unscaled = bd.unscaledValue().abs().toString()
        val negative = bd.signum() < 0

        // Exponent such that mantissa is in [1, 10).
        val exponent = unscaled.length - 1 - bd.scale()

        var mantissaDigits = unscaled.trimEnd('0')
        if (mantissaDigits.isEmpty()) mantissaDigits = "0"

        val mantissa = if (mantissaDigits.length == 1) {
            mantissaDigits
        } else {
            "${mantissaDigits[0]}.${mantissaDigits.substring(1)}"
        }

        val sign = if (negative) "-" else ""
        val exponentStr = toSuperscript(exponent)

        return "$sign$mantissa × 10$exponentStr"
    }

    private fun toSuperscript(n: Int): String {
        val superscriptDigits = mapOf(
            '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
            '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹'
        )
        val negative = n < 0
        val digits = Math.abs(n).toString().map { superscriptDigits[it] ?: it }.joinToString("")
        return if (negative) "⁻$digits" else digits
    }
}
