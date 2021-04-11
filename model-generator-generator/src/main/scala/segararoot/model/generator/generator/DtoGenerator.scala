package segararoot.model.generator.generator

import segararoot.generator.ast._
import segararoot.model.generator.generator.lib._

import scala.collection.JavaConverters._

class DtoGenerator(basePackage: String) {

  def generate(ast: AST): java.util.Collection[CompilationUnit] = {
    val messageEnum = generateEnum(ast.messageDef)
    val messageInterface = generateMessageInterface(messageEnum.toTypeRef)
    val messages = ast.messageDef
      .map(message => generate(
        message,
        basePackage + ".messages",
        messageEnum,
        messageInterface
      ))
    (messages :+ messageEnum :+ messageInterface)
      .map { x =>
        toCompilationUnit(x)
      }
      .asJavaCollection
  }

  private def toCompilationUnit(enum: TypeBuilder) = {
    CompilationUnit(enum.packageName, enum.name, enum.toJavaCode)
  }

  private def generateMessageInterface(messageEnum: TypeRef) = {
    val message = InterfaceBuilder(basePackage, "Message")
    message.appendMethod("messageType", messageEnum)
    message
  }

  private def generateEnum(messageDef: Seq[Message]) = {
    val enum = EnumBuilder(basePackage, "MessageType")

    messageDef.foreach { m =>
      enum.addValue(m.name)
    }
    enum
  }

  private def generate(message: Message,
                       messagesBasePackage: String,
                       messageType: EnumBuilder,
                       messageInterface: InterfaceBuilder) = {
    val dto = ClassBuilder(messagesBasePackage, message.name)
    dto.addImplements(messageInterface.toTypeRef)

    val builder = dto.createInnerClass(message.name + "Builder")


    val BUILDER_PARAM_NAME = "builder"
    val dtoConstructor = dto.appendConstructor()
    dtoConstructor.visibility = VisibilityPrivate
    dtoConstructor.addParam(BUILDER_PARAM_NAME, builder.toTypeRef)
    val dtoConstructorBody = BodyBuilder()

    val messageTypeMethod = dto.appendMethod("messageType", messageType.toTypeRef)
    messageTypeMethod.visibility = VisibilityPublic
    messageTypeMethod.body = BodyBuilder()
      .returnStatement(messageType.toTypeRef.toJavaCode + "." + message.name)
      .getText

    message.fieldDef.foreach { fieldDef =>
      val typeRef = TypeRef(fieldDef.dataType)
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

    val builderMethod = dto.appendMethod("builder", builder.toTypeRef)
    builderMethod.visibility = VisibilityPublic
    builderMethod.isStatic = true
    builderMethod.body = BodyBuilder()
      .returnStatement { b =>
        b.newExpression(builder.toTypeRef) { b =>
        }
      }
      .getText

    val buildMethod = builder.appendMethod("build", dto.toTypeRef)
    buildMethod.visibility = VisibilityPublic
    buildMethod.body = BodyBuilder()
      .returnStatement { b =>
        b.newExpression(dto.toTypeRef) { b =>
          b.addParameter("this")
        }
      }
      .getText

    dto
  }
}
