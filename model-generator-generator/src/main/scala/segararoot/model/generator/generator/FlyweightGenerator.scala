package segararoot.model.generator.generator

import segararoot.generator.ast._
import segararoot.model.generator.generator.lib.{InterfaceBuilder, _}
import segeraroot.connectivity.util._
import segeraroot.connectivity.{Connection, ConnectionCallback}

import java.nio.ByteBuffer
import java.util.function.Consumer
import scala.collection.JavaConverters._

class FlyweightGenerator(basePackage: String) {


  def generate(ast: AST): java.util.Collection[CompilationUnit] = {
    val readersInterfaces = ast.messageDef
      .map(message =>
        generateReaderInterface(
          message,
          basePackage + ".writers"
        ))
    val buildersInterfaces = ast.messageDef
      .map(message => generateBuilderInterface(
        message,
        basePackage + ".readers"
      ))

    val builderFactoryInterface = generateBuilderFactoryInterface(buildersInterfaces, basePackage)
    val builderFactoryImplementation = generateBuilderFactoryImplementation(
      builderFactoryInterface,
      buildersInterfaces.zip(ast.messageDef),
      basePackage + ".impl")
    val readerVisitor = generateReaderVisitor(readersInterfaces, basePackage)
    val messageDeserializer = generateMessageDeserializer(
      ast,
      readerVisitor,
      readersInterfaces.zip(ast.messageDef),
      basePackage + ".impl")

    (readersInterfaces ++
      buildersInterfaces :+
      builderFactoryInterface :+
      readerVisitor :+
      builderFactoryImplementation :+
      messageDeserializer)
      .map { x =>
        toCompilationUnit(x)
      }
      .asJavaCollection
  }

  private def toCompilationUnit(enum: TypeBuilder) = {
    CompilationUnit(enum.packageName, enum.name, enum.toJavaCode)
  }


  private def generateMessageDeserializer(ast: AST,
                                          readerVisitor: InterfaceBuilder,
                                          readers: Seq[(InterfaceBuilder, Message)],
                                          basePackage: String) = {
    val builder = ClassBuilder(basePackage, "MessageDeserializerImpl")
    val readerVisitorRef = readerVisitor.toTypeRef.addGenericParams("?")
    builder.setExtends(
      TypeRef(classOf[MessageDeserializerBase[_]])
        .addGenericParams(readerVisitorRef))

    val ctor = builder.appendConstructor()
    ctor.visibility = VisibilityPublic
    ctor.addParam("callback", readerVisitorRef)
    ctor.body = BodyBuilder()
      .statement { b =>
        b.callSuper { b => b.addParameter("callback") }
      }
      .getText

    readers.foreach { case (i, m) =>
      val deserializer = builder.createInnerClass(m.name + "Deserializer")
      deserializer.addImplements(i.toTypeRef)

      m.fieldDef.foreach { fieldDef =>
        val (t, v) = createDeserializer(fieldDef.dataType)
        val name = deserializer.appendField(fieldDef.name, t)
        name.valueString = v


        val getter = deserializer.appendMethod(fieldDef.name, TypeRef(fieldDef.dataType))
        getter.visibility = VisibilityPublic
        getter.body = BodyBuilder()
          .returnStatement { b =>
            b.invoke("getValue") { b =>
              b.variable(fieldDef.name)
            } { b => }
          }
          .getText
      }

      val deserializerField = builder.appendField("deserializer" + m.name, deserializer.toTypeRef)
      deserializerField.value { b =>
        b.newExpression(deserializer.toTypeRef) { b => }
      }

      deserializer.appendField("position", IntType)

      val onMessageDecl = deserializer.appendMethod("onMessage", BooleanType)
      onMessageDecl.addParam("buffer", TypeRef(classOf[ByteBuffer]))
      onMessageDecl.visibility = VisibilityPublic
      onMessageDecl.body = BodyBuilder()
        .switch(m.fieldDef.zipWithIndex) { b =>
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


      //      public void reset() {
      //        position = 0;
      //        symbol.reset();
      //        price.reset();
      //        volume.reset();
      //        date.reset();
      //      }
      val resetDecl = deserializer.appendMethod("reset", VoidType)
      resetDecl.visibility = VisibilityPublic
      val resetDeclBB = BodyBuilder()
      m.fieldDef.foreach { f =>
        resetDeclBB.statement { b =>
          b.invoke("reset") { b =>
            b.variable(f.name)
          } { b => }
        }
      }
      resetDeclBB.assignStatement("position", "0")
      resetDecl.body = resetDeclBB.getText
    }

    val parseBodyMethod = builder.appendMethod("parseBody", VoidType)
    parseBodyMethod.visibility = VisibilityProtected
    parseBodyMethod.addParam("connection", TypeRef(classOf[Connection]))
    parseBodyMethod.addParam("buffer", TypeRef(classOf[ByteBuffer]))
    parseBodyMethod.body = BodyBuilder()
      .switch(ast.messageDef.zipWithIndex) { b =>
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
      .getText


    builder
  }

  private def createDeserializer(t: DataType): (TypeRef, String) = {
    def createDeserializer(ref: ReferenceType) = {
      val value = ExpressionBuilder()
      value.newExpression(ref) { b => }
      (ref, value.getText)
    }

    t match {
      case DataType_Long =>
        createDeserializer(TypeRef(classOf[LongDeserializer]))

      case DataType_Int =>
        createDeserializer(TypeRef(classOf[IntDeserializer]))

      case DataType_Byte =>
        createDeserializer(TypeRef(classOf[ByteDeserializer]))

      case DataType_FixedByteArray(DataType_Byte, size) =>
        val ref = TypeRef(classOf[ByteArrayDeserializer])
        val value = ExpressionBuilder()
        value.newExpression(ref) { b => b.addParameter(size.toString) }
        (ref, value.getText)
    }
  }

  private def generateBuilderFactoryInterface(builderInterfaces: Seq[InterfaceBuilder], basePackage: String) = {
    val factory = InterfaceBuilder(basePackage, "BuilderFactory")

    builderInterfaces.foreach { builder =>
      factory.appendMethod("create" + builder.name, builder.toTypeRef)
    }

    factory
  }

  private def generateBuilderFactoryImplementation(factoryInterface: InterfaceBuilder, builderInterfaces: Seq[(InterfaceBuilder, Message)], basePackage: String) = {
    val factory = ClassBuilder(basePackage, "BuilderFactoryImpl")
    factory.addImplements(factoryInterface.toTypeRef)
    factory.addImplements(TypeRef(classOf[ByteBufferHolder]))


    val byteBuilderFactory = TypeRef(classOf[ByteBufferFactory])


    factory.appendField("byteBufferFactory", byteBuilderFactory)
    val setMethod = factory.appendMethod("set", VoidType)
    setMethod.addParam("value", byteBuilderFactory)
    setMethod.visibility = VisibilityPublic
    setMethod.body = BodyBuilder()
      .assignStatement("byteBufferFactory", "value")
      .getText

    builderInterfaces.foreach { case (builder, message) =>
      val builderImpl = builderImplGenerator(factory, builder, message)


      val builderField = factory.appendField("builder" + builder.name, builderImpl.toTypeRef)
      builderField.value { builder =>
        builder.newExpression(builderImpl.toTypeRef) { _ => }
      }

      val methodDecl = factory.appendMethod("create" + builder.name, builder.toTypeRef)
      methodDecl.visibility = VisibilityPublic
      methodDecl.body = BodyBuilder()
        .returnStatement(builderField.name)
        .getText
    }

    factory
  }

  private def builderImplGenerator(factory: ClassBuilder,
                                   builder: InterfaceBuilder,
                                   message: Message) = {
    val consumerRef = TypeRef(classOf[Consumer[_]])
      .addGenericParams(TypeRef(classOf[ByteBuffer])
        .toJavaCode)

    val builderImpl = factory.createInnerClass(builder.name + "Impl")
    builderImpl.addImplements(builder.toTypeRef)
    builderImpl.addImplements(consumerRef)
    builderImpl.isStatic = false
    builderImpl.visibility = VisibilityPrivate


    val sendMethod = builderImpl.appendMethod("send", VoidType)
    sendMethod.visibility = VisibilityPublic
    sendMethod.body = BodyBuilder()
      .statement { builder =>
        builder.invoke("write") { builder =>
          builder.variable("byteBufferFactory")
        } { builder =>
          builder.addParameter("this")
        }
      }
      .getText

    message.fieldDef.foreach { fieldDef =>
      val field = builderImpl.appendField(fieldDef.name, TypeRef(fieldDef.dataType))
      fieldDef.dataType match {
        case DataType_FixedByteArray(s, l) =>
          field.value { b =>
            b.newArrayExpression(TypeRef(s)) { p =>
              p.addParameter(l.toString)
            }
          }
        case _ =>
      }

      val setterMethod = builderImpl.appendMethod(fieldDef.name, builder.toTypeRef)
      setterMethod.addParam("value", TypeRef(fieldDef.dataType))
      setterMethod.visibility = VisibilityPublic
      val bb = BodyBuilder()
      copyValueStatement(bb, fieldDef.name, "value", fieldDef.dataType)
      bb.returnStatement("this")
      setterMethod.body = bb.getText
    }

    val acceptMethod = builderImpl.appendMethod("accept", VoidType)
    acceptMethod.visibility = VisibilityPublic
    acceptMethod.addParam("buffer", TypeRef(classOf[ByteBuffer]))
    val acceptBB = BodyBuilder()
    acceptBB.statement { b =>
      b.invoke("put") { b =>
        b.variable("buffer")
      } { b =>
        b.addParameter("(byte)" + message.number)
      }
    }
    message.fieldDef.foreach { fieldDef =>
      fieldDef.dataType match {
        case DataType_Long =>
          invokePut(acceptBB, "buffer", "putLong", fieldDef.name)
        case DataType_Int =>
          invokePut(acceptBB, "buffer", "putInt", fieldDef.name)
        case DataType_Byte =>
          invokePut(acceptBB, "buffer", "put", fieldDef.name)
        case DataType_FixedByteArray(s, l) =>
          invokePut(acceptBB, "buffer", "put", fieldDef.name)
      }
    }
    acceptMethod.body = acceptBB.getText

    builderImpl
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

  private def generateReaderVisitor(readersInterfaces: Seq[InterfaceBuilder], basePackage: String) = {
    val factory = InterfaceBuilder(basePackage, "ReadersVisitor")
    val connection = classOf[Connection]
    val connectionCallback = classOf[ConnectionCallback[_]]
    factory.addGeneric("BuilderFactory")
    factory.addImplements(TypeRef(connectionCallback).addGenericParams("BuilderFactory"))


    readersInterfaces.foreach { reader =>
      val methodDecl = factory.appendMethod("visit", VoidType)
      methodDecl.addParam("connection", TypeRef(connection))
      methodDecl.addParam("value", reader.toTypeRef)
      methodDecl.isDefault = true
      methodDecl.body = ""
    }

    factory
  }

  private def generateBuilderInterface(message: Message,
                                       messagesBasePackage: String) = {
    val builder = InterfaceBuilder(messagesBasePackage, message.name + "Builder")

    message.fieldDef.foreach { message =>
      builder.appendMethod(message.name, builder.toTypeRef)
        .addParam("value", TypeRef(message.dataType))
    }
    builder.appendMethod("send", VoidType)

    builder
  }

  private def generateReaderInterface(message: Message,
                                      messagesBasePackage: String) = {
    val reader = InterfaceBuilder(messagesBasePackage, message.name + "Reader")

    message.fieldDef.foreach { message =>
      reader.appendMethod(message.name, TypeRef(message.dataType))
    }

    reader
  }
}
