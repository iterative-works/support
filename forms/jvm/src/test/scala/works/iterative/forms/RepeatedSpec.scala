// PURPOSE: Pins Repeated.instances, the shared expansion of a repeated group against form state
// PURPOSE: The __items convention, template fallback and totality are decided here, not per walker

package portaly.forms

import zio.test.*
import works.iterative.ui.model.forms.IdPath
import portaly.forms.impl.FormR

object RepeatedSpec extends ZIOSpecDefault:

    val base = IdPath.full("demo")

    val repeated = Repeated("items", optional = true)(
        Section("row")(Field("qty")),
        Section("alt")(Field("note"))
    )

    def spec = suite("Repeated.instances")(
        test("expands __items into per-item paths, matching templates and indices") {
            val state = FormR(Map(
                IdPath("demo.items.__items") -> List("first:row", "second:alt")
            ))
            val instances = Repeated.instances(base, repeated, state)
            assertTrue(
                instances.map(_.path.toHtmlName) ==
                    List("demo.items.first", "demo.items.second"),
                instances.map(_.segment.id.last) == List("row", "alt"),
                instances.map(_.index) == List(0, 1),
                instances.map(i => s"${i.item}:${i.itemType}") ==
                    List("first:row", "second:alt")
            )
        },
        test("unknown item types fall back to the first template") {
            val state = FormR(Map(IdPath("demo.items.__items") -> List("x:gone")))
            val instances = Repeated.instances(base, repeated, state)
            assertTrue(instances.map(_.segment.id.last) == List("row"))
        },
        test("no __items entries means no instances") {
            assertTrue(Repeated.instances(base, repeated, FormR.empty).isEmpty)
        },
        test("a repeated group without templates expands to nothing instead of crashing") {
            val empty = Repeated("items", None, optional = true, Nil)
            val state = FormR(Map(IdPath("demo.items.__items") -> List("first:row")))
            assertTrue(Repeated.instances(base, empty, state).isEmpty)
        }
    )
end RepeatedSpec
