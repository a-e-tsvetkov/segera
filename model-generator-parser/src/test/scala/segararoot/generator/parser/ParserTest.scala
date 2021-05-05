package segararoot.generator.parser

import segararoot.generator.ast._

import scala.reflect.{ClassTag, classTag}

class ParserTest extends org.scalatest.FunSuite {
  test("parse simple message") {
    val result = Parser.parse("message a {}")
    val ast = assertResult[AST](result)

    assert(ast.messageDef.length == 1)
    val message = ast.messageDef.head
    assert(message.name == "a")
  }

  test("parse long field") {
    val result = Parser.parse("message a {f1:long}")
    val ast = assertResult[AST](result)
    val message = ast.messageDef.head
    val field = message.fieldDef.head
    assert(field.name == "f1")
    assert(field.dataType == DataType_Long)
  }

  test("parse int field") {
    val result = Parser.parse("message a {f1:int}")
    val ast = assertResult[AST](result)
    val message = ast.messageDef.head
    val field = message.fieldDef.head
    assert(field.name == "f1")
    assert(field.dataType == DataType_Int)
  }

  test("parse byte field") {
    val result = Parser.parse("message a {f1:byte}")
    val ast = assertResult[AST](result)
    val message = ast.messageDef.head
    val field = message.fieldDef.head
    assert(field.name == "f1")
    assert(field.dataType == DataType_Byte)
  }

  test("parse fixed buffer field") {
    val result = Parser.parse("message a {f1:seq[byte, 3]}")
    val ast = assertResult[AST](result)
    val message = ast.messageDef.head
    val field = message.fieldDef.head
    assert(field.name == "f1")
    assertDataType[DataType_FixedByteArray](field.dataType) { dt =>
      assert(dt.dataType == DataType_Byte)
      assert(dt.length == 3)
    }
  }


  private def toError(result: ParseResult): String = result match {
    case ParseResultFailure(error) => error
    case ParseResultSuccess(ast) => ""
  }

  private def assertResult[T: ClassTag](result: ParseResult) = {

    assert(result.isInstanceOf[ParseResultSuccess], toError(result))
    val statement = result.asInstanceOf[ParseResultSuccess].ast
    assert(classTag[T].runtimeClass == statement.getClass)
    statement.asInstanceOf[T]
  }

  private def assertDataType[T: ClassTag](dataType: DataType)(op: T => Unit): Unit = {
    assert(classTag[T].runtimeClass == dataType.getClass)
    val concreteDataType = dataType.asInstanceOf[T]
    assert(classTag[T].runtimeClass == concreteDataType.getClass)
    op(concreteDataType)
  }
}
