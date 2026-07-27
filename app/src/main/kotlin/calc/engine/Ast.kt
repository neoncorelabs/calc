package calc.engine

/**
 * AST node types per CALC-ENGINE-01 §3 grammar.
 * Produced by [Parser], consumed by [Evaluator].
 */
sealed class Ast {
    data class NumberLiteral(val value: Double) : Ast()

    data class BinaryOp(
        val left: Ast,
        val operator: Token.OperatorType,
        val right: Ast
    ) : Ast()

    data class Negate(val operand: Ast) : Ast()

    data class Square(val operand: Ast) : Ast()

    data class Factorial(val operand: Ast) : Ast()

    data class Percent(val operand: Ast) : Ast()

    data class FunctionCall(
        val function: Token.FunctionType,
        val argument: Ast
    ) : Ast()

    data class ConstantRef(val constant: Token.ConstantType) : Ast()
}
