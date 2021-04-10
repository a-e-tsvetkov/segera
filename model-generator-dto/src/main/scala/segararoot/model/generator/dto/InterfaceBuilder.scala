package segararoot.model.generator.dto

import scala.collection.mutable

case class InterfaceBuilder(packageName: String, name: String) extends TypeBuilder {
  private val visibility: VisibilityModifier = VisibilityPublic

  private val methods: mutable.ListBuffer[MethodDecl] = mutable.ListBuffer()

  def appendMethod(name: String, typeRef: TypeRef): MethodDecl = {
    val methodDecl = MethodDecl(isConstructor = false, name, typeRef)
    methods.append(methodDecl)
    methodDecl
  }

  def toJavaCode: String = {
    val b = new FileBuilder()
    b.packageStatement(packageName)

    b.modifier(visibility.toJavaCode)
    b.interfaceHeader(name)

    methods.foreach { method =>
      b.methodHeader(method.name, method.typeRef)
      b.methodParams(method.params) { (pb, p) =>
        pb.add(p.name, p.typeRef)
      }
      b.append(";\n")
    }

    b.typeFooter()

    b.getText
  }

}
