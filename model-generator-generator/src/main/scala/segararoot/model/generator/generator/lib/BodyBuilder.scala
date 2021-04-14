package segararoot.model.generator.generator.lib

case class BodyBuilder() {
  private val b = new StringBuilder

  def getText: String = b.toString()

  def statement(callback: ExpressionBuilder => Unit): BodyBuilder = {
    callback(new ExpressionBuilder(b))
    b.append(";\n")
    this
  }

  def assignStatement(name: String, value: String): BodyBuilder = {
    b.append(name).append("=").append(value).append(";\n")
    this
  }

  def switch[T](list: Seq[T])
               (c1: ExpressionBuilder => Unit)
               (c2: (T, ExpressionBuilder) => Unit)
               (c3: (T, ExpressionBuilder) => Unit): BodyBuilder = {
    b.append("switch(")
    c1(new ExpressionBuilder(b))
    b.append(") {\n")
    list.foreach { t =>
      b.append("case ")
      c2(t, new ExpressionBuilder(b))
      b.append(" :{ ")
      c3(t, new ExpressionBuilder(b))
      b.append(" break;\n ")
      b.append(" }\n ")
    }
    b.append("}\n")
    this
  }

  def returnStatement(value: String): BodyBuilder = {
    b.append("return ").append(value).append(";\n")
    this
  }

  def returnStatement(callback: ExpressionBuilder => Unit): BodyBuilder = {
    b.append("return ")
    callback(new ExpressionBuilder(b))
    b.append(";\n")
    this
  }
}

object ExpressionBuilder {
  def apply(): ExpressionBuilder = {
    new ExpressionBuilder(new StringBuilder)
  }
}

class ExpressionBuilder(private val b: StringBuilder) {

  def newExpression(referenceType: ReferenceType)(callback: ParameterBuilder => Unit): Unit = {
    b.append("new ").append(referenceType.toJavaCode).append("(")
    callback(new ParameterBuilder(b))
    b.append(")")
  }

  def newArrayExpression(subType: TypeRef)(callback: ParameterBuilder => Unit): Unit = {
    b.append("new ").append(subType.toJavaCode).append("[")
    callback(new ParameterBuilder(b))
    b.append("]")
  }


  def variable(name: String): Unit = {
    b.append(name)
  }

  def raw(text: String): Unit = {
    b.append(text)
  }

  def invoke(name: String)
            (callback: ExpressionBuilder => Unit)
            (paramCallback: ParameterBuilder => Unit): Unit = {
    callback(new ExpressionBuilder(b))
    b.append(".").append(name).append("(")
    paramCallback(new ParameterBuilder(b))
    b.append(")")
  }

  def callSuper(paramCallback: ParameterBuilder => Unit): Unit = {
    b.append("super(")
    paramCallback(new ParameterBuilder(b))
    b.append(")")
  }

  def getText: String = b.toString()
}

class ParameterBuilder(private val b: StringBuilder) {
  private var first = true

  def addParameter(value: String): Unit = {
    if (!first) {
      b.append(", ")
    } else {
      first = false
    }
    b.append(value)
  }
}
