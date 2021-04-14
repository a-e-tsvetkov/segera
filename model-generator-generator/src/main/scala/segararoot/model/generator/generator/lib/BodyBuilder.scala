package segararoot.model.generator.generator.lib

case class BodyBuilder() {
  private val b = new StringBuilder

  def getText: String = b.toString()

  def statement(callback: ExpressonBuilder => Unit): BodyBuilder = {
    callback(new ExpressonBuilder(b))
    b.append(";\n")
    this
  }

  def assignStatement(name: String, value: String): BodyBuilder = {
    b.append(name).append("=").append(value).append(";\n")
    this
  }

  def returnStatement(value: String): BodyBuilder = {
    b.append("return ").append(value).append(";\n")
    this
  }

  def returnStatement(callback: ExpressonBuilder => Unit): BodyBuilder = {
    b.append("return ")
    callback(new ExpressonBuilder(b))
    b.append(";\n")
    this
  }
}

class ExpressonBuilder(private val b: StringBuilder) {

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

  def invoke(name: String)
            (callback: ExpressonBuilder => Unit)
            (paramCallback: ParameterBuilder => Unit): Unit = {
    callback(new ExpressonBuilder(b))
    b.append(".").append(name).append("(")
    paramCallback(new ParameterBuilder(b))
    b.append(")")
  }
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
