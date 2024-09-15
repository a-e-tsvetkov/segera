package segararoot.model.generator.generator

import segararoot.generator.ast._
import segararoot.model.generator.generator.lib._

class DtoGenerator(basePackage: PackageRef) {

  def generate(ast: AST): java.util.Collection[CompilationUnit] = {
    val project = ProjectBuilder()

    val messageEnum = generateEnum(project, ast.messageDef)
    val messageInterface = generateMessageInterface(project, messageEnum)
    ast.messageDef
      .foreach(message => generate(
        project,
        message,
        basePackage.subNamespace("messages"),
        messageEnum,
        messageInterface
      ))

    project.toCompilationUnits
  }

  private def generateMessageInterface(project: ProjectBuilder, messageEnum: TypeRef) = {
    project.newCompilationUnit(basePackage, "Message") { b =>
      b.newInterfaceBuilder("Message") { b =>
        b.appendMethod("messageType", messageEnum) { b => }
      }
    }
  }

  private def generateEnum(project: ProjectBuilder, messageDef: Seq[Message]) = {
    project.newCompilationUnit(basePackage, "MessageType") { b =>
      b.newEnumBuilder("MessageType") { b =>
        messageDef.foreach { m =>
          b.appendValue(m.name)
        }
      }
    }
  }

  private def generate(project: ProjectBuilder,
                       message: Message,
                       messagesBasePackage: PackageRef,
                       messageType: TypeRef,
                       messageInterface: TypeRef) = {
    project.newCompilationUnit(messagesBasePackage, message.name) { b =>

      val BUILDER_PARAM_NAME = "builder"
      b.newClassBuilder(
        message.name,
        VisibilityPublic,
        implements = Seq(messageInterface)) { b =>

        val ref = b.thisTypeRef
        message.fieldDef.foreach { fieldDef =>
          val typeRef = JavaType(fieldDef.dataType)
          b.appendField(fieldDef.name, typeRef,
            visibility = VisibilityPrivate,
            isFinal = true)
          b.appendMethod(fieldDef.name, typeRef, VisibilityPublic) { b => } { b =>
            b.returnStatement(fieldDef.name)
          }
        }
        b.appendMethod("messageType", messageType, VisibilityPublic) { b => } { b =>
          b.returnStatement(messageType.toJavaCode + "." + message.name)
        }

        var builderRef = b.newClassBuilder(message.name + "Builder") { b =>

          b.appendMethod("build", ref, VisibilityPublic) { b => } { b =>
            b.returnStatement { b =>
              b.newExpression(ref) { b =>
                b.addParameter("this")
              }
            }
          }

          message.fieldDef.foreach { fieldDef =>
            val typeRef = JavaType(fieldDef.dataType)

            val PARAMETER_NAME = "value"
            b.appendField(fieldDef.name, typeRef)

            b.appendMethod(fieldDef.name, b.thisTypeRef, VisibilityPublic) { b =>
              b
                .newParam(PARAMETER_NAME, typeRef)
            } { b =>
              b
                .assignStatement("this." + fieldDef.name, PARAMETER_NAME)
                .returnStatement("this")
            }


          }
        }
        b.appendMethod("builder", builderRef, VisibilityPublic, isStatic = true) { b => } { b =>
          b.returnStatement { b =>
            b.newExpression(builderRef) { b => }
          }
        }
        b.appendCtor(VisibilityPrivate) { b =>
          b.newParam(BUILDER_PARAM_NAME, builderRef)
        } { b =>
          message.fieldDef.foreach { fieldDef =>
            b.assignStatement(fieldDef.name, BUILDER_PARAM_NAME + "." + fieldDef.name)
          }
        }
      }
    }
  }
}
