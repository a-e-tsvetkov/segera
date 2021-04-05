package segararoot.generator.ast

sealed trait SToken

object SToken {

  case class ERROR(msg: String) extends SToken

  case class IDENTIFIER(value: String) extends SToken

  case class NUMBER(value: String) extends SToken

  case class LITERAL_NUMERIC(value: Long) extends SToken

  case object COMMA extends SToken

  case object COLON extends SToken

  case object BRACE_LEFT extends SToken

  case object BRACE_RIGHT extends SToken

  case object BRACKET_LEFT extends SToken

  case object BRACKET_RIGHT extends SToken

  //Keywords
  case object MESSAGE extends SToken

  case object LONG extends SToken

  case object INT extends SToken

  case object BYTE extends SToken

  case object SEQ extends SToken

}