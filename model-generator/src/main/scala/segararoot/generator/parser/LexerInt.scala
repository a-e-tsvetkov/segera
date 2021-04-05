package segararoot.generator.parser

import segararoot.generator.ast.SToken
import segararoot.generator.ast.SToken._

import scala.util.parsing.combinator.lexical.Scanners
import scala.util.parsing.input.CharArrayReader.EofCh
import scala.util.parsing.input.{CharSequenceReader, Reader}

object LexerInt extends Scanners {

  private def letter = elem("letter", _.isLetter)

  private def digit = elem("digit", _.isDigit)

  private def chrExcept(cs: Char*) = elem("![" + cs.mkString("") + "]", ch => !cs.contains(ch))

  private def whitespaceChar = elem("space char", ch => ch <= ' ' && ch != EofCh)


  override type Token = SToken

  private val numericLiteral =
    rep1(digit) ^^ (txt => LITERAL_NUMERIC(txt.mkString("").toLong))

  private val literal = numericLiteral

  private val brace =
    '{' ^^^ BRACE_LEFT |
      '}' ^^^ BRACE_RIGHT

  private val bracket =
    '[' ^^^ BRACKET_LEFT |
      ']' ^^^ BRACKET_RIGHT

  private val separator =
    ',' ^^^ COMMA |
      ':' ^^^ COLON

  private val keyword = Map(
    "message" -> MESSAGE,
    "seq" -> SEQ,
    "long" -> LONG,
    "int" -> INT,
    "byte" -> BYTE,
  )

  private val identifierOrKeyword = letter ~ rep(letter | digit | '_') ^^ {
    case x ~ xs =>
      val ident = x :: xs mkString ""
      keyword.getOrElse(ident.toLowerCase, IDENTIFIER(ident))
  }
  private val delimitedIdentifier =
    ('\"' ~> rep(chrExcept('"')) <~ '"') ^^ { s => IDENTIFIER(s.mkString("")) }

  override def whitespace: Parser[Any] = rep[Any](
    whitespaceChar
      | '-' ~ '-' ~ rep(chrExcept(EofCh, '\n'))
  )


  override def token: Parser[Token] =
    identifierOrKeyword |
      delimitedIdentifier |
      literal |
      brace |
      bracket |
      separator |
      failure("Unknown token")


  override def errorToken(msg: String): Token = ERROR(msg)

  def parse(sqlText: String): Reader[Token] = {
    val reader = new CharSequenceReader(sqlText)
    new Scanner(reader)
  }
}
