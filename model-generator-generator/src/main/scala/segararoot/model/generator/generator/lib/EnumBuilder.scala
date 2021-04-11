package segararoot.model.generator.generator.lib

import scala.collection.mutable

case class EnumBuilder(packageName: String, name: String) extends TypeBuilder {
  private val visibility: VisibilityModifier = VisibilityPublic

  private val values: mutable.ListBuffer[EnumValue] = mutable.ListBuffer()

  def addValue(name: String): EnumValue = {
    val value = EnumValue(name)
    values.append(value)
    value
  }

  def toJavaCode: String = {
    val b = new FileBuilder()
    b.packageStatement(packageName)

    b.modifier(visibility.toJavaCode)
    b.enumHeader(name)

    values.foreach { value =>
      b.enumValue(value.name)
    }

    b.typeFooter()

    b.getText
  }

}

case class EnumValue(name: String)