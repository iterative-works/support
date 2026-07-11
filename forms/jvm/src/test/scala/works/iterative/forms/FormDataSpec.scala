// PURPOSE: Pins the semantics of FormData, the typed value currency replacing FormR's Any-typed maps
// PURPOSE: Posted-body ingestion, FormState reads, the __items convention and merge/override are decided here

package portaly.forms

import zio.test.*
import works.iterative.core.FileRef
import works.iterative.ui.model.forms.IdPath

object FormDataSpec extends ZIOSpecDefault:

    val name = IdPath.full("inquiry.person.name")
    val qty = IdPath.full("inquiry.items.i1.row.qty")

    def spec = suite("FormData")(
        test("parse turns posted names into absolute paths with text values") {
            val data = FormData.parse(Map(
                "inquiry.person.name" -> Seq("John"),
                "inquiry.items.i1.row.qty" -> Seq("3")
            ))
            assertTrue(
                data.getString(name).contains("John"),
                data.getString(qty).contains("3"),
                data.getString(IdPath.full("inquiry.missing")).isEmpty
            )
        },
        test("getString takes the first text value, getStringList all of them") {
            val data = FormData.parse(Map("inquiry.tags" -> Seq("a", "b")))
            val tags = IdPath.full("inquiry.tags")
            assertTrue(
                data.getString(tags).contains("a"),
                data.getStringList(tags).contains(List("a", "b"))
            )
        },
        test("getInt and getDouble parse the first text value or yield None") {
            val data = FormData.parse(Map(
                "inquiry.count" -> Seq("42"),
                "inquiry.price" -> Seq("3.5"),
                "inquiry.note" -> Seq("n/a")
            ))
            assertTrue(
                data.getInt(IdPath.full("inquiry.count")).contains(42),
                data.getDouble(IdPath.full("inquiry.price")).contains(3.5),
                data.getInt(IdPath.full("inquiry.note")).isEmpty,
                data.getDouble(IdPath.full("inquiry.note")).isEmpty
            )
        },
        test("getFileList returns file values and skips text, getString skips files") {
            val ref = FileRef.unsafe("scan.pdf", "files/scan.pdf")
            val attachment = IdPath.full("inquiry.attachment")
            val data = FormData(Map(
                attachment -> List(FieldValue.File(ref), FieldValue.Text("scan.pdf"))
            ))
            assertTrue(
                data.getFileList(attachment).contains(List(ref)),
                data.getString(attachment).contains("scan.pdf")
            )
        },
        test("itemsFor parses one key:type per __items entry and skips malformed ones") {
            val data = FormData.parse(Map(
                "inquiry.items.__items" -> Seq("i1:row", "i2:alt:extra", "malformed")
            ))
            assertTrue(
                data.itemsFor(IdPath.full("inquiry.items")) ==
                    List("i1" -> "row", "i2" -> "alt:extra")
            )
        },
        test("add appends a text value after the existing ones") {
            val items = IdPath.full("inquiry.items.__items")
            val data = FormData.parse(Map("inquiry.items.__items" -> Seq("i1:row")))
                .add(items, "i2:row")
            val fresh = FormData.empty.add(items, "i1:row")
            assertTrue(
                data.getStringList(items).contains(List("i1:row", "i2:row")),
                fresh.getStringList(items).contains(List("i1:row"))
            )
        },
        test("filterKeys keeps only the matching paths") {
            val data = FormData.parse(Map(
                "inquiry.items.i1.row.qty" -> Seq("3"),
                "inquiry.person.name" -> Seq("John")
            )).filterKeys(!_.serialize.startsWith("inquiry.items.i1."))
            assertTrue(
                data.getString(qty).isEmpty,
                data.getString(name).contains("John")
            )
        },
        test("combineWith keeps own values on conflict and adds missing keys") {
            val a = FormData.parse(Map("inquiry.person.name" -> Seq("John")))
            val b = FormData.parse(Map(
                "inquiry.person.name" -> Seq("Jane"),
                "inquiry.person.email" -> Seq("jane@example.com")
            ))
            val combined = a.combineWith(b)
            assertTrue(
                combined.getString(name).contains("John"),
                combined.getString(IdPath.full("inquiry.person.email"))
                    .contains("jane@example.com")
            )
        },
        test("overrideWith prefers the other side's values on conflict") {
            // FormR.overrideWith was byte-identical to combineWith and never
            // overrode; FormData pins the intended semantics
            val a = FormData.parse(Map("inquiry.person.name" -> Seq("John")))
            val b = FormData.parse(Map(
                "inquiry.person.name" -> Seq("Jane"),
                "inquiry.person.email" -> Seq("jane@example.com")
            ))
            val overridden = a.overrideWith(b)
            assertTrue(
                overridden.getString(name).contains("Jane"),
                overridden.getString(IdPath.full("inquiry.person.email"))
                    .contains("jane@example.com")
            )
        },
        test("all yields the unwrapped values behind the matching paths") {
            val ref = FileRef.unsafe("scan.pdf", "files/scan.pdf")
            val data = FormData(Map(
                IdPath.full("inquiry.person.name") -> List(FieldValue.Text("John")),
                IdPath.full("inquiry.attachment") -> List(FieldValue.File(ref))
            ))
            assertTrue(
                data.all(_.serialize.startsWith("inquiry.person")) == List("John"),
                data.all(_ => true).toSet == Set("John", ref)
            )
        },
        test("empty has no data and parse of an empty map is empty") {
            assertTrue(
                FormData.empty.isEmpty,
                FormData.parse(Map.empty).isEmpty,
                FormData.parse(Map("inquiry.person.name" -> Seq("John"))).nonEmpty
            )
        }
    )
end FormDataSpec
