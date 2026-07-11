// PURPOSE: Integration tests driving the SSR form scenario through its real zio-http routes
// PURPOSE: Proves the GET/POST loop: render, validate, re-render with errors, repeat add/remove, submit

package works.iterative.forms.scenarios

import zio.test.*
import zio.http.*

object SsrFormScenarioSpec extends ZIOSpecDefault:

    val routes = SsrFormScenario.routes

    def get(path: String) =
        routes.runZIO(Request.get(URL.decode(path).toOption.get))

    def post(path: String, fields: (String, String)*) =
        routes.runZIO(Request.post(
            URL.decode(path).toOption.get,
            Body.fromURLEncodedForm(Form(fields.map(FormField.simpleField.tupled)*))
        ))

    val baseFields = Seq(
        "inquiry.token" -> "proof",
        "inquiry.customer.name" -> "John",
        "inquiry.customer.email" -> "john@example.com",
        "inquiry.request.kind" -> "quote",
        "inquiry.items.__items" -> "i1:row",
        "inquiry.items.i1.row.qty" -> "2",
        "inquiry.items.i1.row.desc" -> "Widget"
    )

    def spec = suite("SsrFormScenario")(
        test("GET renders the initial form with inputs and htmx wiring") {
            for
                response <- get("/ssrForm/page")
                body <- response.body.asString
            yield assertTrue(
                response.status == Status.Ok,
                // browsers fall back to Latin-1 form submission without an explicit charset,
                // mangling any non-ASCII input
                response.headers.get("Content-Type").exists(_.contains("charset=utf-8")),
                body.contains("""<meta charset="utf-8""""),
                body.contains("""name="inquiry.customer.name""""),
                body.contains("""hx-post="/ssrForm/form""""),
                body.contains("""name="inquiry.items.__items""""),
                body.contains("""value="i1:row""""),
                !body.contains("inquiry-request-delivery")
            )
        },
        test("POST submit with blank required fields re-renders with errors") {
            for
                response <- post("/ssrForm/form", ("__submit" -> "submit") +: baseFields.map {
                    case ("inquiry.customer.name", _) => "inquiry.customer.name" -> ""
                    case other                        => other
                }*)
                body <- response.body.asString
            yield assertTrue(
                body.contains("""class="field-errors""""),
                body.contains("Please fill in"),
                !body.contains("Inquiry received")
            )
        },
        test("POST submit with all required data reaches the success page") {
            for
                response <- post("/ssrForm/form", ("__submit" -> "submit") +: baseFields*)
                body <- response.body.asString
            yield assertTrue(
                body.contains("Inquiry received"),
                body.contains("John")
            )
        },
        test("selecting order reveals the conditional delivery section and requires it") {
            val orderFields = baseFields.map {
                case ("inquiry.request.kind", _) => "inquiry.request.kind" -> "order"
                case other                       => other
            }
            for
                rerender <- post("/ssrForm/form", orderFields*)
                rerenderBody <- rerender.body.asString
                submit <- post("/ssrForm/form", ("__submit" -> "submit") +: orderFields*)
                submitBody <- submit.body.asString
            yield assertTrue(
                rerenderBody.contains("""name="inquiry.request.delivery.address""""),
                !rerenderBody.contains("""class="field-errors""""),
                submitBody.contains("""class="field-errors""""),
                !submitBody.contains("Inquiry received")
            )
        },
        test("add button appends a repeated row, remove button drops one") {
            for
                added <- post("/ssrForm/form", ("inquiry.controls.addItem" -> "go") +: baseFields*)
                addedBody <- added.body.asString
                removed <- post(
                    "/ssrForm/form",
                    ("inquiry.items.i1.row.remove" -> "go") +: baseFields*
                )
                removedBody <- removed.body.asString
            yield assertTrue(
                addedBody.contains("""value="i1:row""""),
                addedBody.contains("""value="i2:row""""),
                addedBody.contains("""name="inquiry.items.i2.row.qty""""),
                !removedBody.contains("""value="i1:row""""),
                !removedBody.contains("""name="inquiry.items.i1.row.qty""""),
                !removedBody.contains("Widget")
            )
        },
        test("removing one of two rows keeps the other row and its data") {
            val twoRows = baseFields ++ Seq(
                "inquiry.items.__items" -> "i2:row",
                "inquiry.items.i2.row.qty" -> "5",
                "inquiry.items.i2.row.desc" -> "Gadget"
            )
            for
                removed <- post(
                    "/ssrForm/form",
                    ("inquiry.items.i1.row.remove" -> "go") +: twoRows*
                )
                body <- removed.body.asString
            yield assertTrue(
                body.contains("""value="i2:row""""),
                body.contains("""name="inquiry.items.i2.row.qty""""),
                body.contains("Gadget"),
                !body.contains("""value="i1:row""""),
                !body.contains("Widget")
            )
        },
        test("the declaration survives serialize/reload and renders the identical page") {
            import zio.json.*
            import portaly.forms.service.impl.rest.FormPersistenceCodecs.given
            import portaly.forms.{Form, FormValidationState}
            val json = SsrFormScenario.formDeclaration.toJson
            val reloaded = json.fromJson[Form]
            def render(form: Form) = SsrFormScenario
                .renderFormTag(form, SsrFormScenario.initialState, FormValidationState.valid)
                .render
            assertTrue(
                reloaded.map(render) == Right(render(SsrFormScenario.formDeclaration))
            )
        },
        test("HX-Request responds with just the form fragment, plain POST with a full page") {
            val url = URL.decode("/ssrForm/form").toOption.get
            val form = Body.fromURLEncodedForm(
                Form(baseFields.map(FormField.simpleField.tupled)*)
            )
            for
                fragment <- routes.runZIO(
                    Request.post(url, form).addHeader("HX-Request", "true")
                )
                fragmentBody <- fragment.body.asString
                full <- routes.runZIO(Request.post(url, form))
                fullBody <- full.body.asString
            yield assertTrue(
                fragmentBody.startsWith("<form"),
                !fragmentBody.contains("<html"),
                fullBody.contains("<html"),
                fullBody.contains("<form")
            )
        }
    )
end SsrFormScenarioSpec
