package segararoot.generator.ast

case class AST(messageDef: Seq[Message])

case class Message(name: String, fieldDef: Seq[MessageField]) {
  var number: Integer = _
}

case class MessageField(name: String, dataType: DataType)

sealed trait DataType {
  def size: Int

}

object DataType_Long extends DataType {
  override def size: Int = 8
}

object DataType_Int extends DataType {
  override def size: Int = 4
}

object DataType_Byte extends DataType {
  override def size: Int = 1
}

case class DataType_FixedByteArray(dataType: DataType, length: Int) extends DataType {
  override def size: Int = dataType.size * length
}