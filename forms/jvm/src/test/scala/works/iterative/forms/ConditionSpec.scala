// PURPOSE: Pins the normative semantics of Condition.eval, the one evaluator shared by every walker
// PURPOSE: Blank-filtering NonEmpty, total AnyOf/AllOf and the validity view are decided here, not per interpreter

package works.iterative.forms

import zio.test.*
import works.iterative.ui.model.forms.{AbsolutePath, IdPath}
import Condition.*

object ConditionSpec extends ZIOSpecDefault:

    val base: AbsolutePath = IdPath.full("demo.main")

    def eval(
        condition: Condition,
        values: Map[String, String] = Map.empty,
        valid: Set[String] = Set.empty
    ): Boolean =
        Condition.eval(
            condition,
            base,
            path => values.get(path.toHtmlName),
            path => valid.contains(path.toHtmlName)
        )

    def spec = suite("Condition.eval")(
        test("Never is false, Always is true") {
            assertTrue(!eval(Never), eval(Always))
        },
        test("IsEqual matches the state value at the resolved path") {
            val values = Map("demo.main.kind" -> "special")
            assertTrue(
                eval(IsEqual("kind", "special"), values),
                !eval(IsEqual("kind", "other"), values),
                !eval(IsEqual("missing", "special"), values)
            )
        },
        test("ids with a leading dot resolve absolutely, others against the base") {
            val values = Map("demo.main.kind" -> "rel", "other.kind" -> "abs")
            assertTrue(
                eval(IsEqual("kind", "rel"), values),
                eval(IsEqual(".other.kind", "abs"), values)
            )
        },
        test("NonEmpty filters blank values: absent, empty and whitespace are empty") {
            assertTrue(
                !eval(NonEmpty("note")),
                !eval(NonEmpty("note"), Map("demo.main.note" -> "")),
                !eval(NonEmpty("note"), Map("demo.main.note" -> "   ")),
                eval(NonEmpty("note"), Map("demo.main.note" -> "x"))
            )
        },
        test("IsValid delegates to the validity view") {
            assertTrue(
                eval(IsValid("ico"), valid = Set("demo.main.ico")),
                !eval(IsValid("ico"))
            )
        },
        test("AnyOf is exists, AllOf is forall") {
            assertTrue(
                eval(AnyOf(Never, Always)),
                !eval(AnyOf(Never, Never)),
                !eval(AllOf(Never, Always)),
                eval(AllOf(Always, Always))
            )
        },
        test("empty combinators are total: AnyOf() is false, AllOf() is true") {
            assertTrue(!eval(AnyOf()), eval(AllOf()))
        },
        test("references collects the resolved value and validity paths a condition reads") {
            val condition = AllOf(
                IsEqual("stat", "CZ"),
                AnyOf(NonEmpty(".other.note"), IsValid("ico")),
                Never
            )
            val refs = Condition.references(condition, base)
            assertTrue(
                refs.values.map(_.toHtmlName) == Set("demo.main.stat", "other.note"),
                refs.validity.map(_.toHtmlName) == Set("demo.main.ico")
            )
        },
        test("combinators nest") {
            val values = Map("demo.main.stat" -> "CZ")
            val condition = AllOf(IsEqual("stat", "CZ"), IsValid("ico"))
            assertTrue(
                eval(condition, values, valid = Set("demo.main.ico")),
                !eval(condition, values)
            )
        }
    )
end ConditionSpec
