package calc.engine

/**
 * Converts a raw expression string into a list of [Token]s.
 * Per CALC-ENGINE-01 §2. Numbers accept digits + a single optional
 * decimal point. Unrecognized characters are skipped defensively —
 * the keypad-driven UI should never actually produce them, but the
 * lexer must not throw (see §8: no thrown exceptions from the engine).
 */
object Lexer {

    fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < input.length) {
            val c = input[i]

            when {
                c.isWhitespace() -> {
                    i++
                }

                c.isDigit() || c == '.' -> {
                    val start = i
                    var seenDot = false
                    while (i < input.length && (input[i].isDigit() || (input[i] == '.' && !seenDot))) {
                        if (input[i] == '.') seenDot = true
                        i++
                    }
                    val text = input.substring(start, i)
                    tokens.add(Token.Number(text.toDouble()))
                }

                c == '+' -> {
                    tokens.add(Token.Operator(Token.OperatorType.PLUS))
                    i++
                }

                c == '-' || c == '−' -> {
                    tokens.add(Token.Operator(Token.OperatorType.MINUS))
                    i++
                }

                c == '*' || c == '×' -> {
                    tokens.add(Token.Operator(Token.OperatorType.TIMES))
                    i++
                }

                c == '/' || c == '÷' -> {
                    tokens.add(Token.Operator(Token.OperatorType.DIVIDE))
                    i++
                }

                c == '^' -> {
                    tokens.add(Token.Operator(Token.OperatorType.POWER))
                    i++
                }

                c == '%' -> {
                    tokens.add(Token.Percent())
                    i++
                }

                c == '(' -> {
                    tokens.add(Token.LeftParen())
                    i++
                }

                c == ')' -> {
                    tokens.add(Token.RightParen())
                    i++
                }

                c == '!' -> {
                    tokens.add(Token.Factorial())
                    i++
                }

                c == '²' -> {
                    tokens.add(Token.Square())
                    i++
                }

                c == '±' -> {
                    tokens.add(Token.Negate())
                    i++
                }

                c == 'π' -> {
                    tokens.add(Token.Constant(Token.ConstantType.PI))
                    i++
                }

                c == 'e' -> {
                    tokens.add(Token.Constant(Token.ConstantType.E))
                    i++
                }

                c == '√' -> {
                    tokens.add(Token.Function(Token.FunctionType.SQRT))
                    i++
                }

                input.startsWith("mod", i) -> {
                    tokens.add(Token.Operator(Token.OperatorType.MOD))
                    i += 3
                }

                else -> {
                    // Unrecognized character: skip defensively rather than throw.
                    // Keypad-driven input should never produce this in practice.
                    i++
                }
            }
        }

        return tokens
    }
}
