package segararoot.model.generator.generator

import segararoot.generator.ast._
import segararoot.model.generator.generator.lib._
import segeraroot.connectivity.Connection
import segeraroot.connectivity.callbacks.{ByteBufferFactory, WriteCallback}
import segeraroot.connectivity.impl.deserializers.{ByteArrayDeserializer, ByteDeserializer, IntDeserializer, LongDeserializer}
import segeraroot.connectivity.impl.{ByteBufferHolder, ReaderCallbackBase}

import java.nio.ByteBuffer

class FlyweightGenerator(basePackage: PackageRef) {


  def generate(ast: AST): java.util.Collection[CompilationUnit] = {
    val project = ProjectBuilder()
    val readersInterfaces = ast.messageDef
      .map(message =>
        (message,
          generateReaderInterface(
            project,
            message,
            basePackage.subNamespace("writers")
          )
        ))

    val buildersInterfaces = ast.messageDef
      .map(message => (
        message,
        generateBuilderInterface(
          project,
          message,
          basePackage.subNamespace("readers")
        )))

    val builderFactoryInterface = generateBuilderFactoryInterface(
      project,
      buildersInterfaces,
      basePackage)
    generateBuilderFactoryImplementation(
      project,
      builderFactoryInterface,
      buildersInterfaces,
      basePackage.subNamespace("impl"))
    val readerVisitor = generateReaderVisitor(
      project,
      readersInterfaces.map(_._2),
      basePackage)
    generateMessageDeserializer(
      project,
      ast,
      readerVisitor,
      readersInterfaces,
      basePackage.subNamespace("impl"))

    project.toCompilationUnits
  }

  private def generateMessageDeserializer(projectBuilder: ProjectBuilder,
                                          ast: AST,
                                          readerVisitor: TypeRef,
                                          readers: Seq[(Message, TypeRef)],
                                          basePackage: PackageRef) = {

    projectBuilder.newCompilationUnit(basePackage, "MessageDeserializerImpl") { b =>
      b.newClassBuilder("MessageDeserializerImpl",
        extendsClause = Some(JavaType(classOf[ReaderCallbackBase[_]])
          .addGenericParams(readerVisitor))) { b =>
        b.appendCtor(VisibilityPublic) { b =>
          b.newParam("callback", readerVisitor)
        } { b =>
          b.statement { b =>
            b.callSuper { b => b.addParameter("callback") }
          }
        }

        readers.foreach { case (m, i) =>
          val deserializer = b.newClassBuilder(m.name + "Deserializer", implements = Seq(i)) { b =>
            m.fieldDef.foreach { fieldDef =>
              val (t, v) = createDeserializer(fieldDef.dataType)
              b.appendFieldWithValue(fieldDef.name, t) { b =>
                b.raw(v)
              }
            }

            m.fieldDef.foreach { fieldDef =>
              b.appendMethod(
                fieldDef.name,
                JavaType(fieldDef.dataType),
                VisibilityPublic
              ) { b => } { b =>
                b.returnStatement { b =>
                  b.invoke("getValue") { b =>
                    b.variable(fieldDef.name)
                  } { b => }
                }
              }
            }

            b.appendField("position", IntType)

            b.appendMethod("onMessage", BooleanType, VisibilityPublic) { b =>
              b.newParam("buffer", JavaType(classOf[ByteBuffer]))
            } { b =>
              b.switch(m.fieldDef.zipWithIndex) { b =>
                  b.variable("position")
                } { case ((f, i), b) =>
                  b.variable(i.toString)
                } { case ((f, i), b) =>
                  if (i == m.fieldDef.length - 1) {
                    b.raw(
                      s"""
                         |if (${f.name}.onMessage(buffer)) {
                         |  return true;
                         |}
               """.stripMargin)
                  } else {
                    b.raw(
                      s"""
                         |if (${f.name}.onMessage(buffer)) {
                         |  position = ${i + 1};
                         |}
               """.stripMargin)
                  }
                }
                .returnStatement("false")
                .getText
            }

            b.appendMethod("reset", VoidType, VisibilityPublic) { b => } { b =>
              m.fieldDef.foreach { f =>
                b.statement { b =>
                  b.invoke("reset") { b =>
                    b.variable(f.name)
                  } { b => }
                }
              }
              b.assignStatement("position", "0")
            }
          }

          b.appendFieldWithValue("deserializer" + m.name, deserializer) { b =>
            b.newExpression(deserializer) { b => }
          }
        }

        b.appendMethod("parseBody", VoidType, VisibilityProtected) { b =>
          b.newParam("connection", JavaType(classOf[Connection]))
          b.newParam("buffer", JavaType(classOf[ByteBuffer]))
        } { b =>

          b.switch(ast.messageDef.zipWithIndex) { b =>
            b.variable("messageType")
          } { case ((_, i), b) =>
            b.variable(i.toString)
          } { case ((m, _), b) =>
            val value = "deserializer" + m.name
            b.raw(
              s"""
                 |if ($value.onMessage(buffer)) {
                 |   readingState = ReadingState.START;
                 |   callback.visit(connection, $value);
                 |   $value.reset();
                 |}
                 |""".stripMargin)
          }
        }
      }
    }
  }

  private def createDeserializer(t: DataType): (TypeRef, String) = {
    def createDeserializer(ref: ReferenceType) = {
      val value = ExpressionBuilder()
      value.newExpression(ref) { b => }
      (ref, value.getText)
    }

    t match {
      case DataType_Long =>
        createDeserializer(JavaType(classOf[LongDeserializer]))

      case DataType_Int =>
        createDeserializer(JavaType(classOf[IntDeserializer]))

      case DataType_Byte =>
        createDeserializer(JavaType(classOf[ByteDeserializer]))

      case DataType_FixedByteArray(DataType_Byte, size) =>
        val ref = JavaType(classOf[ByteArrayDeserializer])
        val value = ExpressionBuilder()
        value.newExpression(ref) { b => b.addParameter(size.toString) }
        (ref, value.getText)

      case _ => throw new RuntimeException(s"unexpected type [$t]")
    }
  }

  private def generateBuilderFactoryInterface(projectBuilder: ProjectBuilder,
                                              builderInterfaces: Seq[(Message, TypeRef)],
                                              basePackage: PackageRef) = {

    projectBuilder.newCompilationUnit(basePackage, "BuilderFactory") { b =>
      b.newInterfaceBuilder("BuilderFactory") { b =>

        builderInterfaces.foreach { case (m, t) =>
          b.appendMethod("create" + m.name, t) { b => }
        }
      }
    }
  }

  private def generateBuilderFactoryImplementation(projectBuilder: ProjectBuilder,
                                                   factoryInterface: TypeRef,
                                                   builderInterfaces: Seq[(Message, TypeRef)],
                                                   basePackage: PackageRef) = {

    val byteBuilderFactory = JavaType(classOf[ByteBufferFactory])

    projectBuilder.newCompilationUnit(basePackage, "BuilderFactoryImpl") { b =>
      b.newClassBuilder(
        "BuilderFactoryImpl",
        implements = Seq(factoryInterface, JavaType(classOf[ByteBufferHolder]))
      ) { b =>
        b.appendField("byteBufferFactory", byteBuilderFactory)

        b.appendMethod(
          "set",
          VoidType,
          visibility = VisibilityPublic) { b =>
          b.newParam("value", byteBuilderFactory)
        } { b =>
          b.assignStatement("byteBufferFactory", "value")
        }


        builderInterfaces.foreach { case (message, builder) =>
          val builderImpl = builderImplGenerator(b, builder, message)


          val builderFieldName = "builder" + message.name
          b.appendFieldWithValue(builderFieldName, builderImpl) { b =>
            b.newExpression(builderImpl) { _ => }
          }

          b.appendMethod("create" + message.name, builder, VisibilityPublic) { _ => } { b =>
            b.returnStatement(builderFieldName)
          }
        }
      }
    }
  }

  private def builderImplGenerator(factory: ClassBodyBuilder,
                                   builder: TypeRef,
                                   message: Message) = {
    val writeCallbackRef = JavaType(classOf[WriteCallback])

    factory.newClassBuilder(message.name + "Impl",
      visibility = VisibilityPrivate,
      isStatic = false,
      implements = Seq(builder, writeCallbackRef)) { b =>

      b.appendMethod(
        "send",
        BooleanType,
        visibility = VisibilityPublic) { b => } { b =>
        b.returnStatement { builder =>
          builder.invoke("write") { builder =>
            builder.variable("byteBufferFactory")
          } { builder =>
            builder.addParameter("this")
          }
        }
      }

      message.fieldDef.foreach { fieldDef =>
        fieldDef.dataType match {
          case DataType_FixedByteArray(s, l) =>
            b.appendFieldWithValue(fieldDef.name, JavaType(fieldDef.dataType)) { b =>
              b.newArrayExpression(JavaType(s)) { p =>
                p.addParameter(l.toString)
              }
            }
          case _ =>
            b.appendField(fieldDef.name, JavaType(fieldDef.dataType))
        }

        b.appendMethod(
          name = fieldDef.name,
          typeRef = builder,
          visibility = VisibilityPublic
        ) { b =>
          b.newParam("value", JavaType(fieldDef.dataType))
        } { b =>
          copyValueStatement(b, fieldDef.name, "value", fieldDef.dataType)
          b.returnStatement("this")
        }
      }

      b.appendMethod(
        name = "tryWrite",
        typeRef = BooleanType,
        visibility = VisibilityPublic
      ) { b =>
        b.newParam("buffer", JavaType(classOf[ByteBuffer]))
      } { b =>
        val size = 1 + message.fieldDef.map(x => x.dataType.size).sum
        b.statement { b =>
          b.ifStatement { b =>
            b.compare("<") { b =>
              b.invoke("capacity") { b => b.variable("buffer") } { _ => }
            } { b =>
              b.raw(size.toString)
            }
          } { b =>
            b.returnStatement("false")
          }
          b.invoke("put") { b =>
            b.variable("buffer")
          } { b =>
            b.addParameter("(byte)" + message.number)
          }
        }
        message.fieldDef.foreach { fieldDef =>
          fieldDef.dataType match {
            case DataType_Long =>
              invokePut(b, "buffer", "putLong", fieldDef.name)
            case DataType_Int =>
              invokePut(b, "buffer", "putInt", fieldDef.name)
            case DataType_Byte =>
              invokePut(b, "buffer", "put", fieldDef.name)
            case DataType_FixedByteArray(s, l) =>
              invokePut(b, "buffer", "put", fieldDef.name)
          }
        }
        b.returnStatement("true")
      }
    }
  }

  private def invokePut(acceptBB: BodyBuilder, buffer: String, methodName: String, variable: String) = {
    acceptBB.statement { b =>
      b.invoke(methodName) { b =>
        b.variable(buffer)
      } { b =>
        b.addParameter(variable)
      }
    }
  }

  private def copyValueStatement(bb: BodyBuilder, to: String, from: String, dataType: DataType) = {
    dataType match {
      case DataType_FixedByteArray(s, l) =>
        bb.statement { b =>
          b.invoke("arraycopy") { b =>
            b.variable(classOf[System].getCanonicalName)
          } { b =>
            b.addParameter(from)
            b.addParameter("0")
            b.addParameter(to)
            b.addParameter("0")
            b.addParameter(l.toString)
          }
        }
      case _ =>
        bb.assignStatement(to, from)
    }
  }

  private def generateReaderVisitor(projectBuilder: ProjectBuilder,
                                    readersInterfaces: Seq[TypeRef],
                                    basePackage: PackageRef) = {

    val connection = classOf[Connection]
    projectBuilder.newCompilationUnit(basePackage, "ReadersVisitor") { b =>
      b.newInterfaceBuilder(
        "ReadersVisitor",
      ) { b =>
        readersInterfaces.foreach { reader =>
          b.appendDefaultMethod("visit", VoidType) { b =>
            b.newParam("connection", JavaType(connection))
            b.newParam("value", reader)
          } { b => }
        }
      }
    }
  }

  private def generateBuilderInterface(projectBuilder: ProjectBuilder,
                                       message: Message,
                                       messagesBasePackage: PackageRef) = {
    projectBuilder.newCompilationUnit(messagesBasePackage, message.name + "Builder") { b =>
      b.newInterfaceBuilder(message.name + "Builder") { b =>

        message.fieldDef.foreach { message =>
          b.appendMethod(message.name, b.typeRef) { b =>

            b.newParam("value", JavaType(message.dataType))
          }
        }
        b.appendMethod("send", BooleanType) { b => }
      }
    }
  }

  private def generateReaderInterface(projectBuilder: ProjectBuilder,
                                      message: Message,
                                      messagesBasePackage: PackageRef) = {

    projectBuilder.newCompilationUnit(messagesBasePackage, message.name + "Reader") { b =>
      b.newInterfaceBuilder(message.name + "Reader") { b =>
        message.fieldDef.foreach { message =>
          b.appendMethod(message.name, JavaType(message.dataType)) { b => }
        }
      }
    }
  }
}
