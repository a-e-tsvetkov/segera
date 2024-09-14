package segararoot.model.generator.generator.lib

import scala.collection.mutable

case class InterfaceBuilder(packageName: String, name: String) extends TypeBuilder {
  private val visibility: VisibilityModifier = VisibilityPublic
  private val generics: mutable.ListBuffer[String] = mutable.ListBuffer()
  private val implements: mutable.ListBuffer[TypeRef] = mutable.ListBuffer()
  private val methods: mutable.ListBuffer[MethodDecl] = mutable.ListBuffer()

  def addGeneric(name: String): Unit = generics.append(name)

  def addImplements(typeRef: TypeRef): Unit = {
    implements.append(typeRef)
  }

  def appendMethod(name: String, typeRef: TypeRef): MethodDecl = {
    val methodDecl = MethodDecl(isConstructor = false, name, typeRef)
    methods.append(methodDecl)
    methodDecl
  }

  def toJavaCode: String = {
    val b = new FileBuilder()
    b.packageStatement(packageName)

    b.modifier(visibility.toJavaCode)
    b.interfaceHeader(name, generics = generics.toSeq, implements = implements.toSeq)

    methods.foreach { method =>
      if (method.isDefault) {
        b.modifier("default")
      }
      b.methodHeader(method.name, method.typeRef)
      b.methodParams(method.params.toSeq) { (pb, p) =>
        pb.add(p.name, p.typeRef)
      }
      if (method.isDefault) {
        b.methodBody(method.body)
      } else {
        b.append(";\n")
      }
    }

    b.typeFooter()

    b.getText
  }

}
