package works.iterative.ui.components.laminar.forms

import zio.test.*

object FormZipSpec extends ZIOSpecDefault:
  def spec = suite("Form using zip operator")(
    test("should form a tuple") {
      val fd = new FieldDescriptor:
        override def idString: String = ???
        override def name: String = ???
        override def placeholder: Option[String] = ???
        override def id: FieldId = ???
        override def label = ???
        override def help = ???

      given FieldBuilder[String] = FieldBuilder.requiredInput[String]

      given FormBuilderContext = new FormBuilderContext:
        override def formUIFactory: FormUIFactory = ???
        override def formMessagesResolver: FormMessagesResolver = ???

      val form = Form.Input[String](fd).zip(Form.Empty)
      val form2 = Form.Input[String](fd) +: Form.Empty
      assertTrue(
        form.isInstanceOf[Form[String *: EmptyTuple]],
        form2.isInstanceOf[Form[String *: EmptyTuple]]
      )
    }
  )
