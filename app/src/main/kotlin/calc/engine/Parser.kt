package calc.engine

/**
 * Result of a parse attempt. Failure covers CALC-ENGINE-01 §8's
 * "Syntax Error" case — a malformed expression that can't be
 * auto-corrected. Kept as a sealed type rather than nullable Ast
 * so call sites read clearly and there's room to attach detail later.
 */
sealed class ParseResult {
    data class Success(val ast: Ast) : ParseResult()
    object Failure : ParseResult()
}

/**
 * Recursive-descent parser per CALC-ENGINE-01 §3/§4.
 * Precedence, low to high: + - , x/div/mod , ^ (right-assoc),
 * unary +/-, postfix ! ², parens/functions.
 *
 * Per §4.2, unmatched open parens are auto-closed before parsing
 * completes rather than failing.
 * Per §6, this must tolerate incomplete token streams (e.g. ending
 * in an operator) for live preview — those cases return Failure,
 * which the Evaluator/ViewModel treats as "no update", not an error
 * shown to the user. Failure is not itself the display error path.
 */
class Parser(private val tokens: List<Token>) {

    private var pos = 0

    companion object {
        fun parse(tokens: List<Token>): ParseResult {
            if (tokens.isEmpty()) return ParseResult.Failure
            val closed = autoCloseParens(tokens)
            val parser = Parser(closed)
            return try {
                val ast = parser.parseExpression()
                if (parser.pos != closed.size) {
                    ParseResult.Failure
                } else {
                    ParseResult.Success(ast)
                }
            } catch (e: ParseException) {
                ParseResult.Failure
            }
        }

        private fun autoCloseParens(tokens: List<Token>): List<Token> {
            var depth = 0
            for (t in tokens) {
                when (t) {
                    is Token.LeftParen -> depth++
                    is Token.RightParen -> depth--
                    else -> {}
                }
            }
            if (depth <= 0) return tokens
            val result = tokens.toMutableList()
            repeat(depth) { result.add(Token.RightParen()) }
            return result
        }
    }

    private class ParseException : Exception()

    private fun peek(): Token? = tokens.getOrNull(pos)

    private fun advance(): Token {
        val t = tokens.getOrNull(pos) ?: throw ParseException()
        pos++
        return t
    }

    // expression := term (( "+" | "-" ) term)*
    private fun parseExpression(): Ast {
        var left = parseTerm()
        while (true) {
            val t = peek()
            val opType = when {
                t is Token.Operator && t.type == Token.OperatorType.PLUS -> Token.OperatorType.PLUS
                t is Token.Operator && t.type == Token.OperatorType.MINUS -> Token.OperatorType.MINUS
                else -> null
            } ?: break
            advance()
            val right = parseTerm()
            left = Ast.BinaryOp(left, opType, right)
        }
        return left
    }

    // term := power (( "×" | "÷" | "mod" ) power)*
    private fun parseTerm(): Ast {
        var left = parsePower()
        while (true) {
            val t = peek()
            val opType = when {
                t is Token.Operator && t.type == Token.OperatorType.TIMES -> Token.OperatorType.TIMES
                t is Token.Operator && t.type == Token.OperatorType.DIVIDE -> Token.OperatorType.DIVIDE
                t is Token.Operator && t.type == Token.OperatorType.MOD -> Token.OperatorType.MOD
                else -> null
            } ?: break
            advance()
            val right = parsePower()
            left = Ast.BinaryOp(left, opType, right)
        }
        return left
    }

    // power := unary ( "^" power )?   (right-associative)
    private fun parsePower(): Ast {
        val left = parseUnary()
        val t = peek()
        if (t is Token.Operator && t.type == Token.OperatorType.POWER) {
            advance()
            val right = parsePower() // right-recursion for right-associativity
            return Ast.BinaryOp(left, Token.OperatorType.POWER, right)
        }
        return left
    }

    // unary := ("±")? postfix
    private fun parseUnary(): Ast {
        val t = peek()
        return if (t is Token.Negate) {
            advance()
            Ast.Negate(parsePostfix())
        } else if (t is Token.Operator && t.type == Token.OperatorType.MINUS) {
            // Defensive: a leading '-' read as a binary MINUS token with
            // nothing before it is treated as unary negate.
            advance()
            Ast.Negate(parsePostfix())
        } else {
            parsePostfix()
        }
    }

    // postfix := primary ( "!" | "²" | "%" )*
    private fun parsePostfix(): Ast {
        var node = parsePrimary()
        while (true) {
            when (peek()) {
                is Token.Factorial -> {
                    advance()
                    node = Ast.Factorial(node)
                }
                is Token.Square -> {
                    advance()
                    node = Ast.Square(node)
                }
                is Token.Percent -> {
                    advance()
                    node = Ast.Percent(node)
                }
                else -> return node
            }
        }
    }

    // primary := NUMBER | CONSTANT | "(" expression ")" | FUNCTION "(" expression ")"
    private fun parsePrimary(): Ast {
        val t = advance()
        return when (t) {
            is Token.Number -> Ast.NumberLiteral(t.value)
            is Token.Constant -> Ast.ConstantRef(t.type)
            is Token.LeftParen -> {
                val inner = parseExpression()
                val close = peek()
                if (close is Token.RightParen) {
                    advance()
                } else {
                    throw ParseException()
                }
                inner
            }
            is Token.Function -> {
                val open = peek()
                if (open is Token.LeftParen) {
                    advance()
                } else {
                    throw ParseException()
                }
                val arg = parseExpression()
                val close = peek()
                if (close is Token.RightParen) {
                    advance()
                } else {
                    throw ParseException()
                }
                Ast.FunctionCall(t.type, arg)
            }
            else -> throw ParseException()
        }
    }
}
