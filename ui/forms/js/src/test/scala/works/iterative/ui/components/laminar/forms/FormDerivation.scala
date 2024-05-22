package works.iterative.ui.components.laminar.forms

import zio.test.*

final case class ExampleForm(
    name: String,
    value: Option[String]
)

object DeriveFormSpec extends ZIOSpecDefault:
    override def spec = suite("DeriveFormSpec")(
        test("derive form"):
            // val form = Form.derive[ExampleForm]
            assertCompletes
    )
end DeriveFormSpec
