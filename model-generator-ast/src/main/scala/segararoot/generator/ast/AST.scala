package segararoot.generator.ast

case class AST(messageDef: Seq[Message])

case class Message(name: String, fieldDef: Seq[MessageField]) {
  var number: Integer = _
}

case class MessageField(name: String, dataType: DataType)

sealed trait DataType

object DataType_Long extends DataType

object DataType_Int extends DataType

object DataType_Byte extends DataType

case class DataType_FixedByteArray(dataType: DataType, size: Int) extends DataType