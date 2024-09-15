package segararoot.model.generator.generator.lib

import segararoot.generator.ast._

import scala.collection.mutable

sealed trait NamespaceRef {
  def subNamespace(name: String): PackageRef

  def subType(name: String): ReferenceType
}

case class PackageRef(fullName: String) extends NamespaceRef {
  override def subNamespace(name: String): PackageRef = PackageRef(fullName + "." + name)

  override def subType(name: String): ReferenceType = ReferenceType(fullName + "." + name)
}

sealed trait JavaType {
  def toJavaCode: String
}

sealed trait TypeRef extends NamespaceRef with JavaType {
  def addGenericParams(str: String): TypeRef = {
    GenercType(this, Seq(str))
  }

  def addGenericParams(str: TypeRef): TypeRef = {
    GenercType(this, Seq(str.toJavaCode))
  }
}

object JavaType {
  def apply(dataType: DataType): JavaType = dataType match {
    case DataType_Long => LongType
    case DataType_Int => IntType
    case DataType_Byte => ByteType
    case DataType_FixedByteArray(subDataType, size) => ArrayType(JavaType(subDataType))
  }

  def apply(clazz: Class[_]): ReferenceType = {
    ReferenceType(clazz.getCanonicalName)
  }
}

object LongType extends JavaType {
  override def toJavaCode: String = "long"
}

object IntType extends JavaType {
  override def toJavaCode: String = "int"
}

object ByteType extends JavaType {
  override def toJavaCode: String = "byte"
}

object BooleanType extends JavaType {
  override def toJavaCode: String = "boolean"
}

object VoidType extends JavaType {
  override def toJavaCode: String = "void"
}

case class ArrayType(subType: JavaType) extends JavaType {
  override def toJavaCode: String = subType.toJavaCode + "[]"
}

case class ReferenceType(fullName: String) extends TypeRef {
  override def toJavaCode: String = fullName

  override def subNamespace(name: String): PackageRef = PackageRef(fullName + "." + name)

  override def subType(name: String): ReferenceType = ReferenceType(fullName + "." + name)
}

case class GenercType(base: TypeRef, params: Seq[String]) extends TypeRef {
  override def toJavaCode: String = base.toJavaCode + "<" + params.mkString(", ") + ">"

  override def subNamespace(name: String): PackageRef = ???

  override def subType(name: String): ReferenceType = ???
}

sealed trait VisibilityModifier {
  def toJavaCode: String
}

object VisibilityPublic extends VisibilityModifier {
  override def toJavaCode: String = "public"
}

object VisibilityProtected extends VisibilityModifier {
  override def toJavaCode: String = "protected"
}

object VisibilityPrivate extends VisibilityModifier {
  override def toJavaCode: String = "private"
}

