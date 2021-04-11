package segararoot.model.generator.generator.lib

class FileBuilder() {
  private val b = new StringBuilder

  def getText: String = b.toString()

  def append(str: String): Unit = b.append(str)

  def packageStatement(packageName: String): Unit = {
    b.append("package ").append(packageName).append(";\n")
  }

  def classHeader(name: String, implements: Seq[TypeRef] = Seq()): Unit = {
    b.append("class ").append(name)
    if (implements.nonEmpty) {
      var first = true
      b.append(" implements ")
      implements.foreach { im =>
        if (!first) {
          b.append(", ")
          first = false
        }
        b.append(im.toJavaCode)
      }
    }
    b.append("{").append("\n")
  }

  def interfaceHeader(name: String): Unit = {
    b.append("interface ").append(name).append("{").append("\n")
  }

  def enumHeader(name: String): Unit = {
    b.append("enum ").append(name).append("{").append("\n")
  }

  def typeFooter(): Unit = {
    b.append("}").append("\n")
  }

  def enumValue(name: String): Unit = {
    b.append(name).append(",\n")
  }

  def field(name: String, typeRef: TypeRef): Unit = {
    b.append(typeRef.toJavaCode).append(" ").append(name).append(";\n")
  }

  def methodHeader(name: String, typeRef: TypeRef): Unit = {
    b.append(typeRef.toJavaCode).append(" ").append(name)
  }

  def constructorHeader(name: String): Unit = {
    b.append(name)
  }

  trait ParamBuilder {
    def add(name: String, typeRef: TypeRef)
  }

  def methodParams[T](params: Seq[T])(renderer: (ParamBuilder, T) => Unit): Unit = {
    b.append("(")
    var first = true
    params.foreach { p =>
      if (!first) {
        b.append(", ")
        first = false
      }
      renderer(
        (name: String, typeRef: TypeRef) => {
          b.append(typeRef.toJavaCode).append(" ").append(name)
        },
        p)
    }
    b.append(")")
  }

  def methodBody(body: String): Unit = {
    b.append("{\n").append(body).append("\n}\n")
  }

  def modifier(modifier: String): Unit = {
    b.append(modifier).append(" ")
  }
}
