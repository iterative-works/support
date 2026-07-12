// PURPOSE: Characterization tests pinning FormR's data algebra (parse, typed getters, combine/override, under)
// PURPOSE: FormR is replaced by FormData in the consolidation; these pins define the semantics the replacement must keep or knowingly change

package works.iterative.forms
package impl

import zio.test.*
import works.iterative.ui.model.forms.IdPath

object FormRSpec extends ZIOSpecDefault:

    def spec = suite("FormR characterization")(
        test("parse ingests a raw multi-value map keyed by dotted paths") {
            val data = FormR.parse(Map(
                "contact.name" -> Seq("John"),
                "contact.tags" -> Seq("a", "b")
            ))
            assertTrue(
                data.getString(IdPath.full("contact.name")) == Some("John"),
                data.getStringList(IdPath.full("contact.tags")) == Some(List("a", "b")),
                data.getFirstId("contact.name") == Some("John")
            )
        },
        test("typed getters coerce strings and ignore non-matching values") {
            val data = FormR.strings("a" -> "42", "b" -> "not-a-number")
            assertTrue(
                data.getInt(IdPath.full("a")) == Some(42),
                data.getInt(IdPath.full("b")) == None,
                data.getDouble(IdPath.full("a")) == Some(42.0)
            )
        },
        test("itemsFor reads the __items convention as key:type pairs") {
            val data = FormR(Map(
                IdPath("items.__items") -> List("first:row", "second:row")
            ))
            assertTrue(
                data.itemsFor(IdPath.full("items")) == List("first" -> "row", "second" -> "row")
            )
        },
        test("combineWith keeps existing keys and adds missing ones") {
            val a = FormR.strings("x" -> "1")
            val b = FormR.strings("x" -> "2", "y" -> "3")
            val combined = a.combineWith(b)
            assertTrue(
                combined.getString(IdPath.full("x")) == Some("1"),
                combined.getString(IdPath.full("y")) == Some("3")
            )
        },
        test("overrideWith currently also keeps existing keys (defect: identical to combineWith)") {
            // Pins the current behavior so the FormData replacement is a KNOWING change:
            // overrideWith is expected to prefer the other side's values but does not.
            val a = FormR.strings("x" -> "1")
            val b = FormR.strings("x" -> "2")
            val overridden = a.overrideWith(b)
            assertTrue(overridden.getString(IdPath.full("x")) == Some("1"))
        },
        test("under rebases all data below a prefix") {
            val data = FormR.strings("name" -> "John").under(IdPath("contact"))
            assertTrue(data.getString(IdPath.full("contact.name")) == Some("John"))
        },
        test("add appends to existing values") {
            val data = FormR.empty.add(IdPath("tags"), "a").add(IdPath("tags"), "b")
            assertTrue(data.getStringList(IdPath.full("tags")) == Some(List("a", "b")))
        },
        test("combining under different bases rebases to the common prefix") {
            val a = FormR.data(IdPath.full("form.a"), IdPath("x") -> List("1"))
            val b = FormR.data(IdPath.full("form.b"), IdPath("y") -> List("2"))
            val combined = a.combineWith(b)
            assertTrue(
                combined.getString(IdPath.full("form.a.x")) == Some("1"),
                combined.getString(IdPath.full("form.b.y")) == Some("2")
            )
        }
    )
end FormRSpec
