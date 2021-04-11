package segararoot.model.generator.generator.lib

case class BodyBuilder() {
  private val b = new StringBuilder

  def getText: String = b.toString()

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
    b.append("new ").append(referenceType.name).append("(")
    callback(new ParameterBuilder(b))
    b.append(")")
  }
}

class ParameterBuilder(private val b: StringBuilder) {
  private var first = true

  def addParameter(value: String): Unit = {
    if (!first) {
      b.append(", ")
      first = false
    }
    b.append(value)
  }
}
