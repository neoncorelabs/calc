package calc.engine

/**
 * Token types per CALC-ENGINE-01 §2.
 * Produced by [Lexer], consumed by [Parser].
 */
sealed class Token {
    data class Number(val value: Double) : Token()

    enum class OperatorType { PLUS, MINUS, TIMES, DIVIDE, MOD, POWER }
    data class Operator(val type: OperatorType) : Token()

    data class Percent(val dummy: Unit = Unit) : Token()
    data class Negate(val dummy: Unit = Unit) : Token()

    data class LeftParen(val dummy: Unit = Unit) : Token()
    data class RightParen(val dummy: Unit = Unit) : Token()

    enum class FunctionType { SQRT }
    data class Function(val type: FunctionType) : Token()

    enum class ConstantType { PI, E }
    data class Constant(val type: ConstantType) : Token()

    data class Square(val dummy: Unit = Unit) : Token()
    data class Factorial(val dummy: Unit = Unit) : Token()
}
