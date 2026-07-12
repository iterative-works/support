// PURPOSE: Every walker expands repeated groups identically — typed templates, fallback, no templates
// PURPOSE: Rows are never silently dropped: unrenderable items keep their __items entry and data

package works.iterative.forms.scenarios

import zio.test.*
import zio.json.ast.{Json, JsonCursor}
import works.iterative.core.{Language, MessageCatalogue}
import works.iterative.forms.*
import works.iterative.ui.model.forms.*

object RepeatedConformanceSpec extends ZIOSpecDefault:

    given MessageCatalogue = ConformanceCorpus.messages
    given Language = Language.EN

    private val builder = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))

    private def repeatedForm(optional: Boolean)(templates: SectionSegment*): Form =
        Form("rep", "1")(Repeated("items", None, optional)(templates*))

    private val rowA = Section("rowA")(Field("a", optional = true))
    private val rowB = Section("rowB")(Field("b", optional = true))

    private def group(form: Form, state: FormState): UIRepeatedGroup =
        builder.buildForm(form, state, FormValidationState.valid, None)
            .children.collectFirst { case g: UIRepeatedGroup => g }.get

    private def encoded(form: Form, state: FormState) =
        FormRJsonEncoder().toJsonAST(form, state)
            .get(JsonCursor.field("data") >>> JsonCursor.isObject >>> JsonCursor.field("items"))

    def spec = suite("Repeated conformance")(
        test("typed templates: each item renders its matching template") {
            val form = repeatedForm(optional = true)(rowA, rowB)
            val state = FormData.parse(Map(
                "rep.items.__items" -> Seq("i1:rowA", "i2:rowB"),
                "rep.items.i1.rowA.a" -> Seq("va"),
                "rep.items.i2.rowB.b" -> Seq("vb")
            ))
            val rows = group(form, state).rows
            val json = encoded(form, state)
            assertTrue(
                rows.map(r => (r.item, r.itemType)) == Seq("i1" -> "rowA", "i2" -> "rowB"),
                json.map(_.toString).exists(s => s.contains("va") && s.contains("vb"))
            )
        },
        test("an unmatched item type falls back to the first template in every walker") {
            val form = repeatedForm(optional = true)(rowA, rowB)
            val state = FormData.parse(Map(
                "rep.items.__items" -> Seq("i1:zzz"),
                "rep.items.i1.rowA.a" -> Seq("fallback-value")
            ))
            val rows = group(form, state).rows
            val json = encoded(form, state)
            assertTrue(
                rows.map(r => (r.item, r.itemType)) == Seq("i1" -> "zzz"),
                rows.head.children.nonEmpty,
                json.map(_.toString).exists(_.contains("fallback-value"))
            )
        },
        test("a group without templates keeps its rows: data survives, nothing renders") {
            val form = repeatedForm(optional = true)()
            val state = FormData.parse(Map("rep.items.__items" -> Seq("i1:zzz", "i2:zzz")))
            val rows = group(form, state).rows
            val html = UIFormHtmlRenderer(blankDisplays)
                .render(
                    builder.buildForm(form, state, FormValidationState.valid, None),
                    "/conformance"
                ).render
            val json = encoded(form, state)
            assertTrue(
                rows.map(r => (r.item, r.itemType, r.children.isEmpty)) ==
                    Seq(("i1", "zzz", true), ("i2", "zzz", true)),
                // The __items entries ride the POST loop even though the rows render nothing
                html.contains("""value="i1:zzz""""),
                html.contains("""value="i2:zzz""""),
                // The JSON view of an unrenderable row has no template to shape it — empty
                json == Right(Json.Arr())
            )
        },
        test("a required group is satisfied by items, with or without templates") {
            val withItems = FormData.parse(Map("rep.items.__items" -> Seq("i1:zzz")))
            val empty = FormData.parse(Map.empty)
            val itemsPath = IdPath.full("rep.items")
            def errs(templates: List[SectionSegment], state: FormState) =
                DeclaredValidation.validate(
                    repeatedForm(optional = false)(templates*),
                    state
                ).errors(itemsPath)
            assertTrue(
                errs(List(rowA), withItems).isEmpty,
                errs(Nil, withItems).isEmpty,
                errs(List(rowA), empty).nonEmpty,
                errs(Nil, empty).nonEmpty
            )
        }
    )

    private val blankDisplays =
        new DisplayResolver[FormState, scalatags.Text.all.Frag]:
            def resolve(id: IdPath, state: FormState)(using
                MessageCatalogue,
                Language
            ): scalatags.Text.all.Frag = scalatags.Text.all.frag()
end RepeatedConformanceSpec
