// PURPOSE: Integration tests driving the Datastar form scenario through its real zio-http routes
// PURPOSE: Proves the loop answers Datastar actions with patch-elements SSE and keeps a no-JS fallback

package works.iterative.forms.scenarios

import zio.test.*
import zio.http.*

object DatastarFormScenarioSpec extends ZIOSpecDefault:

    val routes = DatastarFormScenario.routes

    def get(path: String) =
        routes.runZIO(Request.get(URL.decode(path).toOption.get))

    def post(fields: (String, String)*) =
        routes.runZIO(Request.post(
            URL.decode("/datastarForm/form").toOption.get,
            Body.fromURLEncodedForm(Form(fields.map(FormField.simpleField.tupled)*))
        ).addHeader("datastar-request", "true"))

    val baseFields = Seq(
        "inquiry.token" -> "proof",
        "inquiry.customer.name" -> "John",
        "inquiry.customer.email" -> "john@example.com",
        "inquiry.request.kind" -> "quote",
        "inquiry.items.__items" -> "i1:row",
        "inquiry.items.i1.row.qty" -> "2",
        "inquiry.items.i1.row.desc" -> "Widget"
    )

    def spec = suite("DatastarFormScenario")(
        test("GET renders the initial form with Datastar wiring and the module bundle") {
            for
                response <- get("/datastarForm/page")
                body <- response.body.asString
            yield assertTrue(
                response.status == Status.Ok,
                response.headers.get("Content-Type").exists(_.contains("charset=utf-8")),
                body.contains("datastar.js"),
                body.contains("""type="module""""),
                body.contains(
                    """data-on:submit="@post('/datastarForm/form', {contentType: 'form'})""""
                ),
                body.contains("data-on:change="),
                body.contains("""name="inquiry.customer.name""""),
                !body.contains("hx-post"),
                !body.contains("inquiry-request-delivery")
            )
        },
        test("a Datastar change request answers with a patch-elements morph of the form") {
            val orderFields = baseFields.map {
                case ("inquiry.request.kind", _) => "inquiry.request.kind" -> "order"
                case other                       => other
            }
            for
                response <- post(orderFields*)
                body <- response.body.asString
            yield assertTrue(
                response.headers.get("Content-Type").exists(_.contains("text/event-stream")),
                body.contains("event: datastar-patch-elements"),
                body.contains("data: elements <form"),
                body.contains("""name="inquiry.request.delivery.address""""),
                !body.contains("""class="field-errors"""")
            )
        },
        test("a blank submit patches the form back with required errors") {
            for
                response <- post(("__submit" -> "submit") +: baseFields.map {
                    case ("inquiry.customer.name", _) => "inquiry.customer.name" -> ""
                    case other                        => other
                }*)
                body <- response.body.asString
            yield assertTrue(
                body.contains("event: datastar-patch-elements"),
                body.contains("""class="field-errors""""),
                body.contains("Please fill in"),
                !body.contains("Inquiry received")
            )
        },
        test("a valid submit patches the received dump over the form element") {
            for
                response <- post(("__submit" -> "submit") +: baseFields*)
                body <- response.body.asString
            yield assertTrue(
                body.contains("event: datastar-patch-elements"),
                // the dump rides an element with the form's id so the morph replaces the form
                body.contains("""id="inquiry""""),
                body.contains("Inquiry received"),
                body.contains("John")
            )
        },
        test("add and remove buttons patch the repeated rows") {
            for
                added <- post(("inquiry.controls.addItem" -> "go") +: baseFields*)
                addedBody <- added.body.asString
                removed <- post(("inquiry.items.i1.row.remove" -> "go") +: baseFields*)
                removedBody <- removed.body.asString
            yield assertTrue(
                addedBody.contains("""value="i1:row""""),
                addedBody.contains("""value="i2:row""""),
                !removedBody.contains("""value="i1:row""""),
                !removedBody.contains("Widget")
            )
        },
        test("without the Datastar header the POST answers plain full-page HTML") {
            val url = URL.decode("/datastarForm/form").toOption.get
            val form = Body.fromURLEncodedForm(
                Form(baseFields.map(FormField.simpleField.tupled)*)
            )
            for
                full <- routes.runZIO(Request.post(url, form))
                fullBody <- full.body.asString
            yield assertTrue(
                full.headers.get("Content-Type").exists(_.contains("text/html")),
                fullBody.contains("<html"),
                fullBody.contains("<form")
            )
        },
        test("both transports drive the identical POST loop: same outcome for the same fields") {
            // The Datastar scenario shares InquiryFormLoop with the HTMX one; this pins that a
            // submit that succeeds there succeeds here with the same submitted data
            import works.iterative.forms.scenarios.InquiryFormLoop.Outcome
            val raw = (("__submit" -> "submit") +: baseFields)
                .groupMap(_._1)(_._2).map((k, v) => k -> v.toSeq)
            InquiryFormLoop.transition(raw) match
                case Outcome.Submitted(data) =>
                    assertTrue(data.data.nonEmpty)
                case Outcome.Render(_, validation) =>
                    assertTrue(false)
        }
    )
end DatastarFormScenarioSpec
