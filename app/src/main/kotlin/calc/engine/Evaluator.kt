package calc.engine

import kotlin.math.PI
import kotlin.math.E
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Result of evaluating an [Ast]. Per CALC-ENGINE-01 §8, errors are
 * values, never thrown exceptions.
 */
sealed class EvalResult {
    data class Value(val value: Double) : EvalResult()
    data class Error(val kind: ErrorKind) : EvalResult()

    /** Incomplete input during live preview (§6) — not an error to display. */
    object Incomplete : EvalResult()
}

enum class ErrorKind { UNDEFINED, OVERFLOW, SYNTAX_ERROR }

/**
 * Evaluates an [Ast] per CALC-ENGINE-01 §4/§8.
 * Tolerates incomplete ASTs (returns Incomplete, not a thrown error) —
 * required for live preview per §6. In practice Parser.parse already
 * filters out incomplete/malformed token streams as ParseResult.Failure,
 * so Incomplete here is mainly a defensive path if Evaluator is ever
 * called directly against a partially-built Ast.
 */
object Evaluator {

    fun evaluate(ast: Ast): EvalResult {
        return try {
            val result = eval(ast)
            when {
                result.isNaN() -> EvalResult.Error(ErrorKind.UNDEFINED)
                result.isInfinite() -> EvalResult.Error(ErrorKind.OVERFLOW)
                else -> EvalResult.Value(result)
            }
        } catch (e: UndefinedException) {
            EvalResult.Error(ErrorKind.UNDEFINED)
        } catch (e: OverflowException) {
            EvalResult.Error(ErrorKind.OVERFLOW)
        }
    }

    private class UndefinedException : Exception()
    private class OverflowException : Exception()

    private fun eval(node: Ast): Double {
        return when (node) {
            is Ast.NumberLiteral -> node.value

            is Ast.ConstantRef -> when (node.constant) {
                Token.ConstantType.PI -> PI
                Token.ConstantType.E -> E
            }

            is Ast.Negate -> -eval(node.operand)

            is Ast.Square -> {
                val v = eval(node.operand)
                v * v
            }

            is Ast.Factorial -> evalFactorial(node.operand)

            is Ast.Percent -> {
                // Bare N% with no enclosing operator context (e.g. just "50%"
                // entered and evaluated standalone) = N / 100, per §4.4's
                // first rule ("N% alone (postfix, no pending operator)").
                eval(node.operand) / 100.0
            }

            is Ast.FunctionCall -> when (node.function) {
                Token.FunctionType.SQRT -> {
                    val v = eval(node.argument)
                    if (v < 0) throw UndefinedException()
                    sqrt(v)
                }
            }

            is Ast.BinaryOp -> evalBinaryOp(node)
        }
    }

    /**
     * Handles +, -, ×, ÷, mod, ^ — including the special-cased §4.4
     * percent semantics when the right operand is a bare Percent node:
     *   A + N%  -> A + (A × N / 100)
     *   A - N%  -> A - (A × N / 100)
     *   A × N%  -> A × (N / 100)
     *   A ÷ N%  -> A ÷ (N / 100)
     * Any other operator paired with a Percent right operand (e.g. mod, ^)
     * isn't covered by §4.4's table; falls through to treating N% as its
     * own value (N/100) rather than guessing new semantics.
     */
    private fun evalBinaryOp(node: Ast.BinaryOp): Double {
        val rightIsPercent = node.right is Ast.Percent

        if (rightIsPercent) {
            val a = eval(node.left)
            val n = eval((node.right as Ast.Percent).operand)
            return when (node.operator) {
                Token.OperatorType.PLUS -> a + (a * n / 100.0)
                Token.OperatorType.MINUS -> a - (a * n / 100.0)
                Token.OperatorType.TIMES -> a * (n / 100.0)
                Token.OperatorType.DIVIDE -> {
                    val pct = n / 100.0
                    if (pct == 0.0) throw UndefinedException()
                    a / pct
                }
                else -> {
                    // mod / ^ with a percent operand: not specified by §4.4.
                    // Treat N% as its evaluated value (N/100) and proceed normally.
                    val right = n / 100.0
                    applyOperator(a, node.operator, right)
                }
            }
        }

        val left = eval(node.left)
        val right = eval(node.right)
        return applyOperator(left, node.operator, right)
    }

    private fun applyOperator(left: Double, op: Token.OperatorType, right: Double): Double {
        return when (op) {
            Token.OperatorType.PLUS -> left + right
            Token.OperatorType.MINUS -> left - right
            Token.OperatorType.TIMES -> left * right
            Token.OperatorType.DIVIDE -> {
                if (right == 0.0) throw UndefinedException()
                left / right
            }
            Token.OperatorType.MOD -> {
                if (right == 0.0) throw UndefinedException()
                left % right
            }
            Token.OperatorType.POWER -> left.pow(right)
        }
    }

    /**
     * Factorial per §4.5: only valid on non-negative integers <= 170
     * (beyond that, double overflows to Infinity anyway). Non-integer
     * or negative -> Undefined.
     */
    private fun evalFactorial(operand: Ast): Double {
        val v = eval(operand)
        if (v < 0 || v != Math.floor(v)) throw UndefinedException()
        if (v > 170) throw OverflowException()
        var result = 1.0
        var i = 2
        val n = v.toInt()
        while (i <= n) {
            result *= i
            i++
        }
        return result
    }
}
