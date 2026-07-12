// PURPOSE: Characterization tests pinning UIFormBuilder's Form -> UIForm fold semantics
// PURPOSE: Safety net for the forms consolidation (FC-D1); pins current behavior before any refactor

package portaly.forms

import zio.test.*
import works.iterative.ui.model.forms.*
import portaly.forms.impl.FormR

object UIFormBuilderSpec extends ZIOSpecDefault:

    val defaultLayout: LayoutResolver = LayoutResolver.grid(PartialFunction.empty)

    def build(
        form: Form,
        state: FormState = FormR.empty,
        validation: FormValidationState = FormValidationState.valid
    ): UIForm =
        UIFormBuilder(defaultLayout).buildForm(form, state, validation, None)

    def fieldsOf(ui: UIForm): Seq[UIFormElement] =
        def walk(e: UIFormElement): Seq[UIFormElement] = e match
            case s: UIFormSection => e +: s.children.flatMap(walk)
            case g: UIGrid        => e +: g.children.flatten.flatMap(_.children.flatMap(walk))
            case r: UIFlexRow     => e +: r.children.flatMap(walk)
            case other            => Seq(other)
        ui.children.flatMap(walk)

    def spec = suite("UIFormBuilder characterization")(
        test("form id, message key and section nesting reflect the id path") {
            val form = Form("demo", "1")(
                Section("contact")(Field("name"))
            )
            val ui = build(form)
            val sections = fieldsOf(ui).collect { case s: UIFormSection => s }
            assertTrue(
                ui.id == "demo",
                ui.messageKey == works.iterative.core.MessageId("demo"),
                sections.map(_.id) == Seq("demo-contact"),
                sections.head.level == 2
            )
        },
        test("sections lay out as a one-column grid by default") {
            val form = Form("demo", "1")(
                Section("contact")(Field("name"), Field("phone"))
            )
            val ui = build(form)
            val section = fieldsOf(ui).collectFirst { case s: UIFormSection => s }.get
            val grid = section.children.collectFirst { case g: UIGrid => g }.get
            assertTrue(
                grid.children.size == 2,
                grid.children.forall(row => row.size == 1 && row.head.size == 1)
            )
        },
        test("text field renders labeled with state value, default fallback and required decoration") {
            val form = Form("demo", "1")(
                Section("contact")(
                    Field("name"),
                    Field("city", default = Some("Brno")),
                    Field("email", FieldType("email"), optional = true)
                )
            )
            val state = FormR.strings("demo.contact.name" -> "John")
            val fields = fieldsOf(build(form, state)).collect { case f: UILabeledField => f }
            val byId = fields.map(f => f.id -> f).toMap
            val name = byId("demo-contact-name")
            val city = byId("demo-contact-city")
            val email = byId("demo-contact-email")
            assertTrue(
                name.field.asInstanceOf[UITextField].rawValue == Some("John"),
                name.field.asInstanceOf[UITextField].fieldName == "demo.contact.name",
                name.decorations == List(UIFieldDecoration.Required),
                city.field.asInstanceOf[UITextField].rawValue == Some("Brno"),
                email.field.asInstanceOf[UITextField].fieldType == "email",
                email.decorations == Nil
            )
        },
        test("hidden field type renders UIHiddenField with default fallback") {
            val form = Form("demo", "1")(
                Section("meta")(Field("token", FieldType("hidden"), default = Some("s3cret")))
            )
            val hidden = fieldsOf(build(form)).collectFirst { case h: UIHiddenField => h }.get
            assertTrue(
                hidden.id == "demo-meta-token",
                hidden.fieldName == "demo.meta.token",
                hidden.value == Some("s3cret")
            )
        },
        test("enum renders a choice field with options and default; bool helper gives true/false") {
            val form = Form("demo", "1")(
                Section("prefs")(Enum.bool("subscribe", default = Some(false)))
            )
            val choice = fieldsOf(build(form)).collect { case f: UILabeledField => f }
                .head.field.asInstanceOf[UIChoiceField]
            assertTrue(
                choice.values.map(_.value) == List("true", "false"),
                choice.rawValue == Some("false")
            )
        },
        test("date renders as optional text field of type date") {
            val form = Form("demo", "1")(Section("s")(Date("birth")))
            val field = fieldsOf(build(form)).collect { case f: UILabeledField => f }.head
            assertTrue(
                field.field.asInstanceOf[UITextField].fieldType == "date",
                field.decorations == Nil
            )
        },
        test("required enum and date carry the Required decoration") {
            val form = Form("demo", "1")(
                Section("s")(
                    Enum("choice", optional = false)("a", "b"),
                    Date("birth", optional = false)
                )
            )
            val fields = fieldsOf(build(form)).collect { case f: UILabeledField => f }
            val byId = fields.map(f => f.id -> f).toMap
            assertTrue(
                byId("demo-s-choice").decorations == List(UIFieldDecoration.Required),
                byId("demo-s-birth").decorations == List(UIFieldDecoration.Required)
            )
        },
        test("button and display render UIButton and UIBlock") {
            val form = Form("demo", "1")(Section("s")(Button("lookup"), Display("info")))
            val elems = fieldsOf(build(form))
            val button = elems.collectFirst { case b: UIButton => b }.get
            val block = elems.collectFirst { case b: UIBlock => b }.get
            assertTrue(
                button.name == "demo.s.lookup",
                button.buttonType == "button",
                block.id == "demo-s-info"
            )
        },
        test("ShowIf IsEqual renders the element only when state matches") {
            def form = Form("demo", "1")(
                Section("main")(Field("kind")),
                ShowIf(
                    Condition.IsEqual(".demo.main.kind", "special"),
                    Section("extra")(Field("detail"))
                )
            )
            val without = build(form)
            val withMatch = build(form, FormR.strings("demo.main.kind" -> "special"))
            def sectionIds(ui: UIForm) =
                fieldsOf(ui).collect { case s: UIFormSection => s.id }
            assertTrue(
                !sectionIds(without).contains("demo-extra"),
                sectionIds(withMatch).contains("demo-extra")
            )
        },
        test("ShowIf NonEmpty hides the element while the referenced value is blank") {
            def form = Form("demo", "1")(
                Section("main")(Field("note")),
                ShowIf(Condition.NonEmpty(".demo.main.note"), Section("extra")(Field("detail")))
            )
            def sectionIds(state: FormState) =
                fieldsOf(build(form, state)).collect { case s: UIFormSection => s.id }
            assertTrue(
                !sectionIds(FormR.strings("demo.main.note" -> "")).contains("demo-extra"),
                sectionIds(FormR.strings("demo.main.note" -> "x")).contains("demo-extra")
            )
        },
        test("ShowIf with an empty combinator renders instead of crashing") {
            def form = Form("demo", "1")(
                ShowIf(Condition.AllOf(), Section("extra")(Field("detail")))
            )
            val present = fieldsOf(build(form)).collect { case s: UIFormSection => s.id }
            assertTrue(present.contains("demo-extra"))
        },
        test("Repeated expands items from the __items convention with repeat indices") {
            val form = Form("demo", "1")(
                Repeated("items", optional = true)(
                    Section("row")(Field("qty"))
                )
            )
            val state = FormR(Map(
                IdPath("demo.items.__items") -> List("first:row", "second:row"),
                IdPath("demo.items.first.row.qty") -> List("1"),
                IdPath("demo.items.second.row.qty") -> List("2")
            ))
            val ui = build(form, state)
            val rows = fieldsOf(ui).collect {
                case s: UIFormSection if s.id.endsWith("-row") => s
            }
            val values = fieldsOf(ui).collect { case f: UILabeledField => f }
                .map(_.field.asInstanceOf[UITextField].rawValue)
            assertTrue(
                rows.map(_.repeatIndex) == Seq(Some(0), Some(1)),
                values == Seq(Some("1"), Some("2"))
            )
        },
        test("Repeated emits hidden __items fields so item state round-trips through forms") {
            val form = Form("demo", "1")(
                Repeated("items", optional = true)(Section("row")(Field("qty")))
            )
            val state = FormR(Map(
                IdPath("demo.items.__items") -> List("first:row", "second:row")
            ))
            val hidden = fieldsOf(build(form, state)).collect { case h: UIHiddenField => h }
                .filter(_.fieldName == "demo.items.__items")
            assertTrue(hidden.map(_.value) == Seq(Some("first:row"), Some("second:row")))
        },
        test("Repeated renders nothing without __items entries") {
            val form = Form("demo", "1")(
                Repeated("items", optional = true)(Section("row")(Field("qty")))
            )
            val ui = build(form)
            assertTrue(fieldsOf(ui).collect { case s: UIFormSection => s }.isEmpty)
        },
        test("validation errors attach as ErrorMessage decorations on the labeled field") {
            val form = Form("demo", "1")(
                Section("contact")(Field("name"), Field("phone"))
            )
            val msg = works.iterative.core.UserMessage("error.required")
            val validation = MapFormValidationState(
                Map(IdPath.full("demo.contact.name") -> List(msg))
            )
            val fields = fieldsOf(build(form, validation = validation))
                .collect { case f: UILabeledField => f }
            val byId = fields.map(f => f.id -> f).toMap
            assertTrue(
                byId("demo-contact-name").decorations.contains(
                    UIFieldDecoration.ErrorMessage(msg)
                ),
                byId("demo-contact-name").decorations.contains(UIFieldDecoration.Required),
                !byId("demo-contact-phone").decorations.exists {
                    case _: UIFieldDecoration.ErrorMessage => true
                    case _                                 => false
                }
            )
        },
        test("form hook post-processes the built tree") {
            val form = Form("demo", "1")(Section("s")(Field("f")))
            val hooked = UIFormBuilder(defaultLayout, Some(f => f.copy(id = "hooked")))
                .buildForm(form, FormR.empty, FormValidationState.valid, None)
            assertTrue(hooked.id == "hooked")
        }
    )
end UIFormBuilderSpec
