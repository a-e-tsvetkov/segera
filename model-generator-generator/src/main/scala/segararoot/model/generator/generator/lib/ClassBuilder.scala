package segararoot.model.generator.generator.lib

import segararoot.generator.ast._

import scala.collection.mutable

object ClassBuilder {
  def apply(packageName: String, name: String): ClassBuilder = {
    new ClassBuilder(packageName = packageName, parent = null, name = name)
  }
}

class ClassBuilder(val packageName: String, parent: ClassBuilder, val name: String) extends TypeBuilder {
  private val implements: mutable.ListBuffer[TypeRef] = mutable.ListBuffer()
  private var extend: Option[TypeRef] = None
  private val fields: mutable.ListBuffer[FieldDecl] = mutable.ListBuffer()
  private val methods: mutable.ListBuffer[MethodDecl] = mutable.ListBuffer()
  private val innerClasses: mutable.ListBuffer[ClassBuilder] = mutable.ListBuffer()
  var isStatic: Boolean = true
  var visibility: VisibilityModifier = VisibilityPublic

  override def toTypeRef: ReferenceType = {
    ReferenceType(name)
  }

  def setExtends(ref: TypeRef): Unit = extend = Option(ref)

  def createInnerClass(name: String): ClassBuilder = {
    val innerClass = new ClassBuilder(packageName = null, parent = this, name = name)
    innerClasses.append(innerClass)
    innerClass
  }

  def addImplements(typeRef: TypeRef): Unit = {
    implements.append(typeRef)
  }

  def appendField(name: String, typeRef: TypeRef): FieldDecl = {
    val fieldDecl = FieldDecl(name, typeRef)
    fields.append(fieldDecl)
    fieldDecl
  }

  def appendConstructor(): MethodDecl = {
    val methodDecl = MethodDecl(isConstructor = true, null, null)
    methods.append(methodDecl)
    methodDecl
  }

  def appendMethod(name: String, typeRef: TypeRef): MethodDecl = {
    val methodDecl = MethodDecl(isConstructor = false, name, typeRef)
    methods.append(methodDecl)
    methodDecl
  }

  def toJavaCode: String = {
    val b = new FileBuilder()
    b.packageStatement(packageName)
    classDecl(b, isTopLevel = true)
    b.getText
  }

  private def classDecl(b: FileBuilder, isTopLevel: Boolean): Unit = {
    b.modifier(visibility.toJavaCode)
    if (isStatic && !isTopLevel) {
      b.modifier("static")
    }
    b.classHeader(name, extend, implements)

    fields.foreach { field =>
      b.modifier(field.visibility.toJavaCode)
      if (field.isFinal)
        b.modifier("final")
      b.field(field.name, field.typeRef, field.valueString)
    }
    methods.foreach { method =>
      b.modifier(method.visibility.toJavaCode)
      if (method.isFinal)
        b.modifier("final")
      if (method.isStatic)
        b.modifier("static")
      if (method.isConstructor) {
        b.constructorHeader(name)
      } else {
        b.methodHeader(method.name, method.typeRef)
      }
      b.methodParams(method.params) { (pb, p) =>
        pb.add(p.name, p.typeRef)
      }
      b.methodBody(method.body)
    }

    innerClasses.foreach { cls =>
      cls.classDecl(b, isTopLevel = false)
    }

    b.typeFooter()
  }
}

case class FieldDecl(name: String, typeRef: TypeRef) {
  var isFinal: Boolean = false
  var visibility: VisibilityModifier = VisibilityPrivate
  var valueString: String = _

  def value(callback: ExpressionBuilder => Unit): Unit = {
    val b = new StringBuilder
    callback(new ExpressionBuilder(b))
    valueString = b.mkString
  }
}

case class ParamDecl(name: String, typeRef: TypeRef)

case class MethodDecl(isConstructor: Boolean, name: String, typeRef: TypeRef) {
  var isDefault: Boolean = false
  var isFinal: Boolean = false
  var isStatic: Boolean = false
  var visibility: VisibilityModifier = VisibilityPrivate
  var body: String = _
  val params: mutable.ListBuffer[ParamDecl] = mutable.ListBuffer()

  def addParam(name: String, typeRef: TypeRef): ParamDecl = {
    val paramDecl = ParamDecl(name, typeRef)
    params.append(paramDecl)
    paramDecl
  }
}

sealed trait TypeRef {
  def addGenericParams(str: String): TypeRef = {
    GenercType(this, Seq(str))
  }

  def addGenericParams(str: TypeRef): TypeRef = {
    GenercType(this, Seq(str.toJavaCode))
  }

  def toJavaCode: String
}

object TypeRef {
  def apply(dataType: DataType): TypeRef = dataType match {
    case DataType_Long => LongType
    case DataType_Int => IntType
    case DataType_Byte => ByteType
    case DataType_FixedByteArray(subDataType, size) => ArrayType(TypeRef(subDataType))
  }

  def apply(clazz: Class[_]): ReferenceType = {
    ReferenceType(clazz.getCanonicalName)
  }
}

object LongType extends TypeRef {
  override def toJavaCode: String = "long"
}

object IntType extends TypeRef {
  override def toJavaCode: String = "int"
}

object ByteType extends TypeRef {
  override def toJavaCode: String = "byte"
}

object BooleanType extends TypeRef {
  override def toJavaCode: String = "boolean"
}

object VoidType extends TypeRef {
  override def toJavaCode: String = "void"
}

case class ArrayType(subType: TypeRef) extends TypeRef {
  override def toJavaCode: String = subType.toJavaCode + "[]"
}

case class ReferenceType(name: String) extends TypeRef {
  override def toJavaCode: String = name
}

case class GenercType(base: TypeRef, params: Seq[String]) extends TypeRef {
  override def toJavaCode: String = base.toJavaCode + "<" + params.mkString(", ") + ">"
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

