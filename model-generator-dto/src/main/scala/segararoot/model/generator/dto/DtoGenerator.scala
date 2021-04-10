package segararoot.model.generator.dto

import segararoot.generator.ast._

import scala.collection.JavaConverters._

class DtoGenerator(basePackage: String) {

  def generate(ast: AST): java.util.Collection[CompilationUnit] = {
    ast.messageDef
      .map(generate)
      .asJavaCollection
  }


  private def generate(message: Message): CompilationUnit = {
    val dto = ClassBuilder(basePackage, message.name)

    val builder = dto.createInnerClass(message.name + "Builder")


    val BUILDER_PARAM_NAME = "builder"
    val dtoConstructor = dto.appendConstructor()
    dtoConstructor.visibility = VisibilityPrivate
    dtoConstructor.addParam(BUILDER_PARAM_NAME, builder.toTypeRef)
    val dtoConstructorBody = BodyBuilder()

    message.fieldDef.foreach { fieldDef =>
      val typeRef = convertType(fieldDef.dataType)
      val field = dto.appendField(fieldDef.name, typeRef)
      field.isFinal = true
      val getter = dto.appendMethod(fieldDef.name, typeRef)
      getter.visibility = VisibilityPublic
      getter.body = BodyBuilder()
        .returnStatement(fieldDef.name)
        .getText


      dtoConstructorBody.assignStatement(fieldDef.name, BUILDER_PARAM_NAME + "." + fieldDef.name)

      builder.appendField(fieldDef.name, typeRef)

      val setter = builder.appendMethod(fieldDef.name, builder.toTypeRef)
      setter.visibility = VisibilityPublic
      val PARAMETER_NAME = "value"
      setter.addParam(PARAMETER_NAME, typeRef)
      setter.body = BodyBuilder()
        .assignStatement("this." + field.name, PARAMETER_NAME)
        .returnStatement("this")
        .getText
    }
    dtoConstructor.body = dtoConstructorBody.getText

    val buildMethod = builder.appendMethod("build", dto.toTypeRef)
    buildMethod.visibility = VisibilityPublic
    buildMethod.body = BodyBuilder()
      .returnStatement { b =>
        b.newExpression(dto.toTypeRef) { b =>
          b.addParameter("this")
        }
      }
      .getText

    CompilationUnit(basePackage, message.name, dto.toJavaCode)
  }

  def convertType(dataType: DataType): TypeRef = dataType match {
    case DataType_Long => LongType

    case DataType_Int => IntType

    case DataType_Byte => ByteType

    case DataType_FixedByteArray(subDataType, size) => ArrayType(convertType(subDataType))
  }
}
