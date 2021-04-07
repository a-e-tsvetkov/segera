package segararoot.generator.parser

import segararoot.generator.ast.AST

sealed trait ParseResult {
}

case class ParseResultSuccess(ast: AST) extends ParseResult

case class ParseResultFailure(error: String) extends ParseResult
