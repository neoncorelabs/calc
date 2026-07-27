package calc.engine

/**
 * Single entry point into the engine layer, per CALC-ARCH-01 §2/§4.
 * The ViewModel calls only this facade — it has no knowledge of
 * Lexer/Parser/Evaluator/Formatter individually.
 *
 * Pipeline: raw input string -> Lexer -> Parser -> Evaluator -> Formatter.
 *
 * Exposes two distinct calls because live preview (CALC-ENGINE-01 §6)
 * and final `=` evaluation (§8) have different error-surfacing rules,
 * even though they must otherwise use identical evaluation/precision
 * logic:
 *   - previewEvaluate: incompleteness (e.g. input ending in an
 *     operator) must NOT show an error state (§6) — any parse failure
 *     here collapses to EvalResult.Incomplete.
 *   - finalEvaluate: a malformed expression on `=` must be able to
 *     surface as the "Syntax Error" display state (§8) — a parse
 *     failure here surfaces as EvalResult.Error(SYNTAX_ERROR).
 *
 * Neither Parser nor Evaluator alone distinguishes "incomplete" from
 * "genuinely malformed" (Parser.parse() returns Failure for both) —
 * that UX-level distinction belongs here, at the point where the two
 * different call sites need two different answers to the same
 * Failure signal.
 */
object EngineFacade {

    /**
     * Evaluates the current in-progress input for live display (§6).
     * Never returns Error(SYNTAX_ERROR) — an unparseable (including
     * incomplete) expression becomes Incomplete, meaning "no update,
     * keep showing the last valid preview." A complete-but-erroring
     * expression (e.g. "5÷0") still returns Error(UNDEFINED) normally,
     * per §6: "If the expression is complete but evaluates to a real
     * error... preview shows Undefined live."
     */
    fun previewEvaluate(input: String): EvalResult {
        val tokens = Lexer.tokenize(input)
        if (tokens.isEmpty()) return EvalResult.Incomplete

        return when (val parseResult = Parser.parse(tokens)) {
            is ParseResult.Failure -> EvalResult.Incomplete
            is ParseResult.Success -> Evaluator.evaluate(parseResult.ast)
        }
    }

    /**
     * Evaluates the current input as a final, committed calculation
     * (the `=` press). A parse failure here is a real Syntax Error
     * per §8's error taxonomy — this is the one path where that
     * error kind can actually surface.
     */
    fun finalEvaluate(input: String): EvalResult {
        val tokens = Lexer.tokenize(input)
        if (tokens.isEmpty()) return EvalResult.Error(ErrorKind.SYNTAX_ERROR)

        return when (val parseResult = Parser.parse(tokens)) {
            is ParseResult.Failure -> EvalResult.Error(ErrorKind.SYNTAX_ERROR)
            is ParseResult.Success -> Evaluator.evaluate(parseResult.ast)
        }
    }

    /**
     * Formats an EvalResult for display. Thin pass-through to
     * Formatter, kept here so the ViewModel never imports Formatter
     * directly — it only ever talks to EngineFacade.
     */
    fun format(result: EvalResult): String = Formatter.format(result)
}
