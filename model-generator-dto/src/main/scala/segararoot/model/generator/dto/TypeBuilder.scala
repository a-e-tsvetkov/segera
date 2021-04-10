package segararoot.model.generator.dto

trait TypeBuilder {
  def name: String

  def packageName: String

  def toJavaCode: String

  def toTypeRef: TypeRef = ReferenceType(packageName + "." + name)
}
