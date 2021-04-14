package segararoot.generator.parser

import segararoot.generator.ast._

import scala.util.parsing.combinator.{PackratParsers, Parsers}
import scala.util.parsing.input.Reader

object Parser {
  def parse(text: String): ParseResult = {
    val tokens = LexerInt.parse(text)
    ParserInt.parse(tokens) match {
      case Left(value) => value
      case Right(value) =>
        process(value)
        ParseResultSuccess(value)
    }
  }

  private def process(value: AST): Unit = {
    value.messageDef.zipWithIndex.foreach { case (m, i) =>
      m.number = i
    }
  }
}


private object ParserInt extends Parsers with PackratParsers {

  import SToken._

  override type Elem = SToken

  def parse(tokens: Reader[SToken]): Either[ParseResultFailure, AST] = {
    val value = goal(new PackratReader[SToken](tokens))
    value match {
      case Success(st, next) => Right(st)
      case NoSuccess(msg, next) => Left(ParseResultFailure(msg))
    }
  }

  val goal: Parser[AST] = phrase(file)

  lazy val file: Parser[AST] =
    messages ^^ AST |
      err("unknown statement")

  lazy val identifier: Parser[String] = {
    accept("<identifier>", { case id: IDENTIFIER => id.value })
  }

  lazy val number: Parser[Long] = {
    accept("<number>", { case id: LITERAL_NUMERIC => id.value })
  }

  lazy val messages: Parser[Seq[Message]] =
    rep(message)

  lazy val message: Parser[Message] =
    MESSAGE ~> identifier ~ messageBody ^^ { case name ~ fields => Message(name, fields) }

  lazy val messageBody: Parser[Seq[MessageField]] =
    BRACE_LEFT ~> repsep(field, COMMA) <~ BRACE_RIGHT

  lazy val field: Parser[MessageField] =
    identifier ~ COLON ~ dataType ^^ { case name ~ _ ~ dataType => MessageField(name, dataType) }

  lazy val dataType: Parser[DataType] =
    LONG ^^^ DataType_Long |
      INT ^^^ DataType_Int |
      BYTE ^^^ DataType_Byte |
      fixedSeqType

  val fixedSeqType: Parser[DataType] =
    SEQ ~> BRACKET_LEFT ~> dataType ~ COMMA ~ number <~ BRACKET_RIGHT ^^ { case dataType ~ _ ~ number => DataType_FixedByteArray(dataType, number.toInt) }

}

