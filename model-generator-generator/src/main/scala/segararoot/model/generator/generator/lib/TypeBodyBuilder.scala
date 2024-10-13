package segararoot.model.generator.generator.lib

trait TypeBodyBuilder {
}

case class InterfaceBodyBuilder(private val builder: StringBuilder, container: TypeRef) extends TypeBodyBuilder with ParentContainer {
  def typeRef: TypeRef = container

  def appendMethod(name: String, typeRef: JavaType)(params: ParamsBuilder => Unit): Unit = {
    b.append(typeRef.toJavaCode).append(" ").append(name)
    b.append("(")
    params(ParamsBuilder(b))
    b.append(")")
    b.append(";\n")
  }

  def appendDefaultMethod(
                           name: String,
                           typeRef: JavaType
                         )
                         (
                           params: ParamsBuilder => Unit)(
                           body: BodyBuilder => Unit
                         ): Unit = {
    b.append("default ")
    b.append(typeRef.toJavaCode).append(" ").append(name)
    b.append("(")
    params(ParamsBuilder(b))
    b.append(")")
    b.append("{")
    body(BodyBuilder(b))
    b.append("}")
  }

  override val b: StringBuilder = builder
}

case class EnumBodyBuilder(private val builder: StringBuilder, container: TypeRef, className: String) extends TypeBodyBuilder with ParentContainer {
  override val b: StringBuilder = builder

  def appendValue(name: String): Unit = {
    b.append(name).append(",\n")
  }
}

case class ClassBodyBuilder(private val builder: StringBuilder, container: TypeRef, className: String) extends TypeBodyBuilder with ParentContainer {
  def thisTypeRef: TypeRef = container

  override def appendStatic(b: StringBuilder, isStatic: Boolean): Unit = {
    if (isStatic) {
      b.append("static ")
    }
  }

  def appendField(name: String,
                  typeRef: JavaType,
                  visibility: VisibilityModifier = VisibilityPrivate,
                  isFinal: Boolean = false
                 ): Unit = {
    appendVisibility(visibility)
    if (isFinal) {
      b.append("final ")
    }
    b.append(typeRef.toJavaCode).append(" ").append(name).append(";\n")
  }

  def appendFieldWithValue(name: String,
                           typeRef: JavaType,
                           visibility: VisibilityModifier = VisibilityPrivate
                          )(value: ExpressionBuilder => Unit): Unit = {
    appendVisibility(visibility)
    b.append(typeRef.toJavaCode).append(" ").append(name)
    b.append(" = ")
    value(new ExpressionBuilder(b))
    b.append(";\n")
  }

  private def appendVisibility(visibility: VisibilityModifier) = {
    b.append(visibility.toJavaCode).append(" ")
  }

  def appendMethod(name: String,
                   typeRef: JavaType,
                   visibility: VisibilityModifier = VisibilityPrivate,
                   isStatic: Boolean = false
                  )(
                    params: ParamsBuilder => Unit
                  )(
                    body: BodyBuilder => Unit
                  ): Unit = {
    appendVisibility(visibility)
    if (isStatic) {
      b.append("static ")
    }
    b.append(typeRef.toJavaCode).append(" ").append(name)
    b.append("(")
    params(ParamsBuilder(b))
    b.append(")")
    b.append("{\n")
    body(BodyBuilder(b))
    b.append("}\n")
  }

  def appendCtor(
                  visibility: VisibilityModifier = VisibilityPrivate
                )(
                  params: ParamsBuilder => Unit
                )(
                  body: BodyBuilder => Unit
                ): Unit = {
    appendVisibility(visibility)
    b /*.append(typeRef.toJavaCode).append(" ")*/.append(className)
    b.append("(")
    params(ParamsBuilder(b))
    b.append(")")
    b.append("{\n")
    body(BodyBuilder(b))
    b.append("}\n")
  }

  override val b: StringBuilder = builder
}

case class ParamsBuilder(private val b: StringBuilder) {
  private var first = true

  def newParam(name: String, typeRef: JavaType): Unit = {
    if (!first) {
      b.append(", ")
    } else {
      first = false
    }
    b.append(typeRef.toJavaCode).append(" ").append(name)
  }
}
