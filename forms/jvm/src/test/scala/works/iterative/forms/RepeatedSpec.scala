// PURPOSE: Pins Repeated.instances, the shared expansion of a repeated group against form state
// PURPOSE: The __items convention, template fallback and totality are decided here, not per walker

package works.iterative.forms

import zio.test.*
import works.iterative.ui.model.forms.IdPath
import works.iterative.forms.impl.FormR

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
                instances.map(_.segment.map(_.id.last)) == List(Some("row"), Some("alt")),
                instances.map(_.index) == List(0, 1),
                instances.map(i => s"${i.item}:${i.itemType}") ==
                    List("first:row", "second:alt")
            )
        },
        test("unknown item types fall back to the first template") {
            val state = FormR(Map(IdPath("demo.items.__items") -> List("x:gone")))
            val instances = Repeated.instances(base, repeated, state)
            assertTrue(instances.map(_.segment.map(_.id.last)) == List(Some("row")))
        },
        test("no __items entries means no instances") {
            assertTrue(Repeated.instances(base, repeated, FormR.empty).isEmpty)
        },
        test("a group without templates keeps its items as segment-less instances") {
            val empty = Repeated("items", None, optional = true, Nil)
            val state = FormR(Map(IdPath("demo.items.__items") -> List("first:row")))
            val instances = Repeated.instances(base, empty, state)
            assertTrue(
                instances.map(i => (i.item, i.itemType, i.segment)) ==
                    List(("first", "row", None))
            )
        },
        test("template picks the matching item type, falls back to the first, none on empty") {
            assertTrue(
                Repeated.template(repeated.elems, "alt").map(_.id.last) == Some("alt"),
                Repeated.template(repeated.elems, "gone").map(_.id.last) == Some("row"),
                Repeated.template(Nil, "row").isEmpty
            )
        }
    )
end RepeatedSpec
