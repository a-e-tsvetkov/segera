package segararoot.model.generator.generator.lib

import java.util
import scala.collection.mutable

case class ProjectBuilder(private val compileUnits: mutable.ListBuffer[CompilationUnit] = mutable.ListBuffer[CompilationUnit]()) {
  def toCompilationUnits: util.Collection[CompilationUnit] = {
    import scala.jdk.CollectionConverters._
    compileUnits.asJavaCollection
  }

  def newCompilationUnit[T](packageName: PackageRef, name: String)(callback: CompileUnitBuilder => T): T = {
    val b = new StringBuilder
    val compileUnitBuilder = CompileUnitBuilder(b, packageName)

    b.append("package ").append(packageName.fullName).append(";\n")
    val result = callback(compileUnitBuilder)
    compileUnits += CompilationUnit(packageName, name, b.result())
    result
  }
}

case class CompileUnitBuilder(private val builder: StringBuilder, private val packageName: NamespaceRef) extends ParentContainer {
  override val container: NamespaceRef = packageName
  override val b: StringBuilder = builder
}
