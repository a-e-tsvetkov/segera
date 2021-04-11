package segararoot.model.generator.generator

import segararoot.generator.ast._
import segararoot.model.generator.generator.lib._

import scala.collection.JavaConverters._

class FliweightGenerator(basePackage: String) {


  def generate(ast: AST): java.util.Collection[CompilationUnit] = {
    val readersInterfaces = ast.messageDef
      .map(message =>
        generateGetInterface(
          message,
          basePackage + ".writers"
        ))
    val buildersInterfaces = ast.messageDef
      .map(message => generateBuilderInterface(
        message,
        basePackage + ".readers"
      ))

    val builderFactory = generateBuilderFactory(buildersInterfaces, basePackage)
    val readerVisitor = generateReaderVisitor(readersInterfaces, basePackage)

    (readersInterfaces ++ buildersInterfaces :+ builderFactory :+ readerVisitor)
      .map { x =>
        toCompilationUnit(x)
      }
      .asJavaCollection
  }

  private def toCompilationUnit(enum: TypeBuilder) = {
    CompilationUnit(enum.packageName, enum.name, enum.toJavaCode)
  }

  private def generateBuilderFactory(builderInterfaces: Seq[InterfaceBuilder], basePackage: String) = {
    val factory = InterfaceBuilder(basePackage, "BuilderFactory")

    builderInterfaces.foreach { builder =>
      factory.appendMethod("create" + builder.name, builder.toTypeRef)
    }

    factory
  }

  private def generateReaderVisitor(readersInterfaces: Seq[InterfaceBuilder], basePackage: String) = {
    val factory = InterfaceBuilder(basePackage, "ReadersVisitor")

    readersInterfaces.foreach { reader =>
      val methodDecl = factory.appendMethod("visit", VoidType)
      methodDecl.addParam("value", reader.toTypeRef)
      methodDecl.isDefault = true
      methodDecl.body = ""
    }

    factory
  }

  private def generateGetInterface(message: Message,
                                   messagesBasePackage: String) = {
    val builder = InterfaceBuilder(messagesBasePackage, message.name + "Builder")

    message.fieldDef.foreach { message =>
      builder.appendMethod(message.name, builder.toTypeRef)
        .addParam("value", TypeRef(message.dataType))
    }
    builder.appendMethod("send", VoidType)

    builder
  }

  private def generateBuilderInterface(message: Message,
                                       messagesBasePackage: String) = {
    val reader = InterfaceBuilder(messagesBasePackage, message.name + "Reader")

    message.fieldDef.foreach { message =>
      reader.appendMethod(message.name, TypeRef(message.dataType))
    }

    reader
  }
}
