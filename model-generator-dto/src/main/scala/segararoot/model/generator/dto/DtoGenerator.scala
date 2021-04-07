package segararoot.model.generator.dto

import segararoot.generator.ast.{AST, Message}

import scala.collection.JavaConverters._

class DtoGenerator(basePackage: String) {

  def generate(ast: AST): java.util.Collection[CompilationUnit] = {
    ast.messageDef
      .map(generate)
      .asJavaCollection
  }


  private def generate(message: Message): CompilationUnit = {
    val body: String = ""
    CompilationUnit(basePackage, message.name, body)
  }
}
