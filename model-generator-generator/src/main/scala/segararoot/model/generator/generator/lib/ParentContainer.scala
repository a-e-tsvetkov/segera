package segararoot.model.generator.generator.lib

trait ParentContainer {
  val b: StringBuilder
  val container: NamespaceRef

  def newClassBuilder(name: String,
                      visibility: VisibilityModifier = VisibilityPublic,
                      generics: Seq[String] = Seq(),
                      implements: Seq[TypeRef] = Seq(),
                      extendsClause: Option[TypeRef] = None,
                      isStatic: Boolean = true
                     )(callback: ClassBodyBuilder => Unit): ReferenceType = {

    val result = container.subType(name)

    b.append(visibility.toJavaCode).append(" ")
    appendStatic(b, isStatic)
    classHeader(name, generics = generics, extendsClause = extendsClause, implements = implements)

    callback(ClassBodyBuilder(b, result, name))

    b.append("}").append("\n")
    result
  }

  def appendStatic(b: StringBuilder, isStatic: Boolean): Unit = {}

  def newInterfaceBuilder(name: String,
                          visibility: VisibilityModifier = VisibilityPublic,
                          generics: Seq[String] = Seq(),
                          implements: Seq[TypeRef] = Seq()
                         )(callback: InterfaceBodyBuilder => Unit): TypeRef = {

    val fullName = container.subType(name)
    b.append(visibility.toJavaCode).append(" ")
    interfaceHeader(name, generics = generics, implements = implements)

    callback(InterfaceBodyBuilder(b, fullName))

    b.append("}").append("\n")
    fullName
  }

  def newEnumBuilder(name: String,
                     visibility: VisibilityModifier = VisibilityPublic,
                    )(callback: EnumBodyBuilder => Unit): ReferenceType = {

    val result = container.subType(name)

    b.append(visibility.toJavaCode).append(" ")
    b.append("enum ").append(name).append("{").append("\n")

    callback(EnumBodyBuilder(b, result, name))

    b.append("}").append("\n")
    result
  }

  private def interfaceHeader(name: String,
                              generics: Seq[String] = Seq(),
                              implements: Seq[TypeRef] = Seq()): Unit = {
    b.append("interface ").append(name)
    if (generics.nonEmpty) {
      b.append("<")
        .append(generics.mkString(", "))
        .append(">")
    }
    if (implements.nonEmpty) {
      b.append(" extends ").append(implements.map(x => x.toJavaCode).mkString(", "))
    }
    b.append("{").append("\n")
  }

  private def classHeader(name: String,
                          generics: Seq[String],
                          extendsClause: Option[TypeRef],
                          implements: Seq[TypeRef]): Unit = {
    b.append("class ").append(name)
    if (generics.nonEmpty) {
      b.append("<")
        .append(generics.mkString(", "))
        .append(">")
    }
    b.append(" ")
    extendsClause.foreach(t => b.append("extends ").append(t.toJavaCode).append(" "))
    if (implements.nonEmpty) {
      b.append("implements ").append(implements.map(x => x.toJavaCode).mkString(", "))
    }
    b.append("{").append("\n")
  }
}
