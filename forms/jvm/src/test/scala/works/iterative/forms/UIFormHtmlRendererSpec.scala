// PURPOSE: Tests for the SSR HTML renderer that turns UIForm into a scalatags form page
// PURPOSE: Drives the ssr-html-interpreter slice (FC-D4): plain POST form with HTMX enrichment

package works.iterative.forms

import zio.test.*
import scalatags.Text.all.{Frag, span, stringFrag}
import works.iterative.core.{Language, MessageCatalogue}
import works.iterative.ui.model.forms.*
import works.iterative.forms.impl.FormR

object UIFormHtmlRendererSpec extends ZIOSpecDefault:

    given MessageCatalogue = MessageCatalogue.debug

    val noDisplay: DisplayResolver[FormState, Frag] = new DisplayResolver[FormState, Frag]:
        def resolve(id: IdPath, state: FormState)(using MessageCatalogue, Language): Frag =
            span("display:" + id.toHtmlName)

    val renderer = UIFormHtmlRenderer(noDisplay, FormTransport.htmx)

    def html(
        form: Form,
        state: FormState = FormR.empty,
        validation: FormValidationState = FormValidationState.valid
    ): String =
        val ui = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))
            .buildForm(form, state, validation, None)
        renderer.render(ui, "/submit").render

    def spec = suite("UIFormHtmlRenderer")(
        test("form-level transport attributes come from the transport") {
            import scalatags.Text.all.{attr, stringAttr}
            val sentinel = new FormTransport:
                def formAttributes(postAction: String): Seq[scalatags.Text.all.Modifier] =
                    Seq(attr("data-sentinel") := postAction)
            val ui = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))
                .buildForm(Form("demo", "1")(Section("s")(Field("name"))), FormR.empty,
                    FormValidationState.valid, None)
            val out = UIFormHtmlRenderer(noDisplay, sentinel).render(ui, "/submit").render
            assertTrue(
                out.contains("""data-sentinel="/submit""""),
                !out.contains("hx-post")
            )
        },
        test("renders a post form with htmx wiring and a submit button") {
            val out = html(Form("demo", "1")(Section("s")(Field("name"))))
            assertTrue(
                out.contains("""<form id="demo""""),
                out.contains("""method="post""""),
                out.contains("""action="/submit""""),
                out.contains("""hx-post="/submit""""),
                // only committed-choice controls re-render: outerHTML-swapping on text-field
                // change wipes values the user typed while the request was in flight
                out.contains(
                    """hx-trigger="change from:select, change from:input[type='checkbox'], change from:input[type='radio'], change from:input[type='date']""""
                ),
                out.contains("""hx-swap="outerHTML""""),
                // novalidate: change re-renders must fire while required fields are blank
                // (htmx validates forms unless noValidate), and the server is the validator
                out.contains("novalidate"),
                // named so the server can tell real submissions from change-triggered re-renders
                out.matches("(?s).*<button[^>]*name=\"__submit\"[^>]*type=\"submit\".*")
            )
        },
        test("text field renders label and named input with value and required attribute") {
            val form = Form("demo", "1")(
                Section("contact")(Field("name"), Field("email", FieldType("email"), optional = true))
            )
            val out = html(form, FormR.strings("demo.contact.name" -> "John"))
            assertTrue(
                out.contains("""<label for="demo-contact-name""""),
                out.contains("""name="demo.contact.name""""),
                out.contains("""id="demo-contact-name""""),
                out.contains("""value="John""""),
                out.contains("""type="email""""),
                out.matches("(?s).*<input[^>]*name=\"demo.contact.name\"[^>]*required.*")
            )
        },
        test("validation errors render next to the field") {
            val form = Form("demo", "1")(Section("contact")(Field("name")))
            val validation = MapFormValidationState(Map(
                IdPath.full("demo.contact.name") ->
                    List(works.iterative.core.UserMessage("error.required"))
            ))
            val out = html(form, validation = validation)
            assertTrue(
                out.contains("field-errors"),
                out.contains("error.required")
            )
        },
        test("hidden field renders a hidden input") {
            val form = Form("demo", "1")(
                Section("meta")(Field("token", FieldType("hidden"), default = Some("s3cret")))
            )
            val out = html(form)
            assertTrue(
                out.matches("(?s).*<input[^>]*type=\"hidden\"[^>]*name=\"demo.meta.token\".*"),
                out.contains("""value="s3cret"""")
            )
        },
        test("enum renders a select with options and selected value") {
            val form = Form("demo", "1")(
                Section("prefs")(Enum("subscribe", default = Some("false"))("true", "false"))
            )
            val out = html(form)
            assertTrue(
                out.contains("""<select"""),
                out.contains("""name="demo.prefs.subscribe""""),
                out.contains("""value="true""""),
                out.matches("(?s).*<option[^>]*value=\"false\"[^>]*selected.*")
            )
        },
        test("date renders an input of type date") {
            val out = html(Form("demo", "1")(Section("s")(Date("birth"))))
            assertTrue(out.contains("""type="date""""))
        },
        test("prose renders a textarea") {
            val out = html(Form("demo", "1")(Section("s")(Field("story", FieldType("prose")))))
            assertTrue(out.matches("(?s).*<textarea[^>]*name=\"demo.s.story\".*"))
        },
        test("unknown field types degrade to text inputs") {
            val out = html(Form("demo", "1")(Section("s")(Field("ico", FieldType("czech:ico")))))
            assertTrue(out.matches("(?s).*<input[^>]*type=\"text\"[^>]*name=\"demo.s.ico\".*"))
        },
        test("closed field kinds pick their html input type regardless of id spelling") {
            val out = html(Form("demo", "1")(Section("s")(
                Field("mail", FieldType("base:email")),
                Field("phone", FieldType("base:phone")),
                Field("qty", FieldType("number:natural")),
                Field("zip", FieldType("base:zip"))
            )))
            assertTrue(
                out.matches("(?s).*<input[^>]*type=\"email\"[^>]*name=\"demo.s.mail\".*"),
                out.matches("(?s).*<input[^>]*type=\"tel\"[^>]*name=\"demo.s.phone\".*"),
                out.matches("(?s).*<input[^>]*type=\"number\"[^>]*name=\"demo.s.qty\".*"),
                out.matches("(?s).*<input[^>]*type=\"text\"[^>]*name=\"demo.s.zip\".*")
            )
        },
        test("disabled field renders a disabled input with a hidden mirror keeping its value") {
            // disabled inputs never submit, so the value rides a hidden input or the
            // POST loop would drop disabled-field state on every round-trip
            val form = Form("demo", "1")(
                Section("s")(Field("locked", FieldType("string", None, disabled = true)))
            )
            val out = html(form, FormR.strings("demo.s.locked" -> "fixed"))
            assertTrue(
                out.matches("(?s).*<input[^>]*name=\"demo.s.locked\"[^>]*disabled.*") ||
                    out.matches("(?s).*<input[^>]*disabled[^>]*name=\"demo.s.locked\".*"),
                out.matches(
                    "(?s).*<input[^>]*type=\"hidden\"[^>]*name=\"demo.s.locked\"[^>]*value=\"fixed\".*"
                )
            )
        },
        test("file field renders a file input honoring multiple") {
            val form = Form("demo", "1")(
                Section("docs")(File("attachments", multiple = true, optional = true))
            )
            val out = html(form)
            assertTrue(
                out.matches("(?s).*<input[^>]*type=\"file\"[^>]*name=\"demo.docs.attachments\".*"),
                out.contains("multiple")
            )
        },
        test("button renders as a named submit button for server-side handling") {
            val out = html(Form("demo", "1")(Section("s")(Button("lookup"))))
            assertTrue(
                out.matches("(?s).*<button[^>]*name=\"demo.s.lookup\"[^>]*type=\"submit\".*") ||
                    out.matches("(?s).*<button[^>]*type=\"submit\"[^>]*name=\"demo.s.lookup\".*")
            )
        },
        test("declared submit button becomes the form's submit and replaces the chrome") {
            val out = html(Form("demo", "1")(Section("s")(Button("send", ButtonIntent.Submit))))
            assertTrue(
                out.matches("(?s).*<button[^>]*name=\"__submit\"[^>]*type=\"submit\".*") ||
                    out.matches("(?s).*<button[^>]*type=\"submit\"[^>]*name=\"__submit\".*"),
                !out.contains("form-actions")
            )
        },
        test("client action button renders inert without a name") {
            val out = html(Form("demo", "1")(Section("s")(
                Button("ares", ButtonIntent.ClientAction)
            )))
            assertTrue(
                out.matches("(?s).*<button[^>]*id=\"demo-s-ares\"[^>]*type=\"button\".*") ||
                    out.matches("(?s).*<button[^>]*type=\"button\"[^>]*id=\"demo-s-ares\".*"),
                !out.matches("(?s).*<button[^>]*name=\"demo.s.ares\".*")
            )
        },
        test("repeated rows render inside the group with hidden __items inputs per row") {
            val form = Form("demo", "1")(
                Repeated("items", optional = true)(Section("row")(Field("qty")))
            )
            val state = FormR(Map(
                IdPath("demo.items.__items") -> List("first:row", "second:row"),
                IdPath("demo.items.first.row.qty") -> List("1")
            ))
            val out = html(form, state)
            assertTrue(
                out.contains("""id="demo-items""""),
                out.contains("repeated-row"),
                out.matches(
                    "(?s).*<input[^>]*type=\"hidden\"[^>]*name=\"demo.items.__items\"[^>]*value=\"first:row\".*"
                ),
                out.matches(
                    "(?s).*<input[^>]*type=\"hidden\"[^>]*name=\"demo.items.__items\"[^>]*value=\"second:row\".*"
                ),
                out.contains("""value="1"""")
            )
        },
        test("errors on a required empty repeated group render inside the group") {
            val form = Form("demo", "1")(
                Repeated("items", optional = false)(Section("row")(Field("qty")))
            )
            val validation = MapFormValidationState(Map(
                IdPath.full("demo.items") ->
                    List(works.iterative.core.UserMessage("error.field.required"))
            ))
            val out = html(form, validation = validation)
            assertTrue(
                out.contains("field-errors"),
                out.contains("error.field.required")
            )
        },
        test("form-level and section-level errors render in the page") {
            val form = Form("demo", "1")(Section("contact")(Field("name")))
            val validation = MapFormValidationState(Map(
                IdPath.full("demo") ->
                    List(works.iterative.core.UserMessage("error.form.incomplete")),
                IdPath.full("demo.contact") ->
                    List(works.iterative.core.UserMessage("error.section.invalid"))
            ))
            val out = html(form, validation = validation)
            assertTrue(
                out.contains("form-errors"),
                out.contains("error.form.incomplete"),
                out.contains("error.section.invalid")
            )
        },
        test("display block renders resolved content") {
            val out = html(Form("demo", "1")(Section("s")(Display("info"))))
            assertTrue(out.contains("display:demo.s.info"))
        },
        test("display blocks with dashed segment ids resolve their real path") {
            // Ids used to be reconstructed by splitting the html id on "-", which
            // breaks for any segment that contains a dash itself
            val out = html(Form("demo", "1")(Section("s")(Display("extra-info"))))
            assertTrue(out.contains("display:demo.s.extra-info"))
        },
        test("section renders a heading from its message key") {
            val out = html(Form("demo", "1")(Section("contact")(Field("name"))))
            assertTrue(
                out.contains("""<section id="demo-contact""""),
                out.contains("contact.section")
            )
        }
    )
end UIFormHtmlRendererSpec
