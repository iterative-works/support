// PURPOSE: Scenario proving the server-side HTML form loop: GET renders, POST validates and re-renders
// PURPOSE: Exercises conditions, repeated add/remove and Required validation without any client-side app

package works.iterative.forms.scenarios

import zio.http.{Response, Routes, Method, Root, Request, handler, string}
import zio.http.template.Html
import scalatags.Text.all.*
import scalatags.Text.tags2
import portaly.forms.*
import portaly.forms.impl.FormR
import works.iterative.core.{Language, MessageCatalogue}
import works.iterative.core.service.impl.InMemoryMessageCatalogue
import works.iterative.scenarios.Scenario
import works.iterative.ui.model.forms.{FormState, IdPath}

object SsrFormScenario extends Scenario:
    val id = "ssrForm"
    val label = "SSR Form"

    val formDeclaration: Form = Form("inquiry", "1")(
        Field("token", FieldType("hidden"), default = Some("proof")),
        Section("customer")(
            Field("name"),
            Field("email", FieldType("email")),
            Field("note", FieldType("prose"), optional = true)
        ),
        Section("request")(
            Enum("kind", default = Some("quote"))("quote", "order"),
            ShowIf(
                Condition.IsEqual(".inquiry.request.kind", "order"),
                Section("delivery")(Field("address"))
            ),
            Date("deadline"),
            Display("summary")
        ),
        Repeated("items", default = Some("i1" -> "row"), optional = true)(
            Section("row")(
                Field("qty", FieldType("number")),
                Field("desc"),
                Button("remove")
            )
        ),
        Section("controls")(Button("addItem"))
    )

    given MessageCatalogue = InMemoryMessageCatalogue(
        Language.EN,
        Map(
            "inquiry.title" -> "Inquiry",
            "inquiry.submit" -> "Send inquiry",
            "inquiry.customer.section" -> "Customer",
            "inquiry.customer.name.label" -> "Name",
            "inquiry.customer.email.label" -> "E-mail",
            "inquiry.customer.note.label" -> "Note",
            "inquiry.request.section" -> "Request",
            "inquiry.request.kind.label" -> "Kind",
            "inquiry.request.kind.quote.label" -> "Quote",
            "inquiry.request.kind.order.label" -> "Order",
            "inquiry.request.delivery.section" -> "Delivery",
            "inquiry.request.delivery.address.label" -> "Address",
            "inquiry.request.deadline.label" -> "Deadline",
            "inquiry.request.summary.title" -> "Summary",
            "inquiry.row.section" -> "Item %d",
            "inquiry.row.qty.label" -> "Quantity",
            "inquiry.row.desc.label" -> "Description",
            "inquiry.row.remove.label" -> "Remove item",
            "inquiry.controls.addItem.label" -> "Add item",
            "error.field.required" -> "Please fill in %s"
        )
    )

    private val itemsPath = IdPath.full("inquiry.items")
    private val addItemKey = "inquiry.controls.addItem"
    private val removeItemKey = "inquiry\\.items\\.([^.]+)\\.row\\.remove".r
    private val postAction = s"/$id/form"

    val initialState: FormR =
        FormR(Map(IdPath("inquiry.items.__items") -> List("i1:row")))

    private val displayResolver: DisplayResolver[FormState, Frag] =
        new DisplayResolver[FormState, Frag]:
            def resolve(path: IdPath, state: FormState)(using MessageCatalogue, Language): Frag =
                val kind = state.getString(IdPath.full("inquiry.request.kind")).getOrElse("quote")
                val items = state.itemsFor(itemsPath).size
                p(s"You are requesting a $kind with $items item(s).")

    private val renderer = UIFormHtmlRenderer(displayResolver)
    private val builder = UIFormBuilder(LayoutResolver.grid(PartialFunction.empty))

    def renderFormTag(form: Form, state: FormState, validation: FormValidationState): Tag =
        renderer.render(builder.buildForm(form, state, validation, None), postAction)

    private val style = """
        body { font-family: sans-serif; max-width: 40rem; margin: 2rem auto; }
        .field { margin: 0.5rem 0; }
        .field label { display: block; font-weight: bold; }
        .field input, .field textarea, .field select { width: 100%; box-sizing: border-box; }
        .required { color: #b00; margin-left: 0.2rem; }
        .field-errors { color: #b00; font-size: 0.9rem; }
        section { border-left: 3px solid #ddd; padding-left: 1rem; margin: 1rem 0; }
    """

    private def shell(inner: Frag): String =
        "<!doctype html>" + html(
            head(
                tags2.title("SSR Form"),
                script(src := "https://unpkg.com/htmx.org@2.0.2"),
                tag("style")(raw(style))
            ),
            body(inner)
        ).render

    // Response.html would prepend its own doctype, breaking fragment swaps
    private def htmlResponse(content: String): Response =
        Response(
            body = zio.http.Body.fromString(content),
            headers = zio.http.Headers(zio.http.Header.ContentType(zio.http.MediaType.text.html))
        )

    override def page: Html =
        Html.raw(shell(renderFormTag(formDeclaration, initialState, FormValidationState.valid)))

    def respond(raw: Map[String, Seq[String]], hxRequest: Boolean): Response =
        val data = FormR.parse(raw)
        val removed = raw.keys.collectFirst { case removeItemKey(key) => key }
        if raw.contains(addItemKey) then
            val nextIndex = data.itemsFor(itemsPath)
                .flatMap((key, _) => key.stripPrefix("i").toIntOption)
                .maxOption.getOrElse(0) + 1
            respondForm(data.add(IdPath("inquiry.items.__items"), s"i$nextIndex:row"), hxRequest)
        else
            removed match
                case Some(key) =>
                    val remaining = data.itemsFor(itemsPath)
                        .filterNot(_._1 == key)
                        .map((k, t) => s"$k:$t")
                    val cleaned = FormR(
                        data.filterKeys(!_.serialize.startsWith(s"inquiry.items.$key."))
                            .data.map((k, v) => IdPath(k.serialize) -> v)
                            .updated(IdPath("inquiry.items.__items"), remaining)
                    )
                    respondForm(cleaned, hxRequest)
                case None if raw.contains("__submit") =>
                    val validation = RequiredValidation.validate(formDeclaration, data)
                    if validation.hasErrors then respondForm(data, hxRequest, validation)
                    else submitted(data)
                case None =>
                    respondForm(data, hxRequest)
        end if
    end respond

    private def respondForm(
        state: FormR,
        hxRequest: Boolean,
        validation: FormValidationState = FormValidationState.valid
    ): Response =
        val formTag = renderFormTag(formDeclaration, state, validation)
        if hxRequest then htmlResponse(formTag.render)
        else htmlResponse(shell(formTag))
    end respondForm

    private def submitted(data: FormR): Response =
        import zio.json.*
        import portaly.forms.service.impl.rest.FormPersistenceCodecs.given
        htmlResponse(shell(frag(
            h1("Inquiry received"),
            p("Submitted data:"),
            pre(code((data: FormR).toJson))
        )))
    end submitted

    override val routes: Routes[Any, Nothing] = Routes(
        Method.GET / Root / id / "page" -> handler(
            htmlResponse(shell(renderFormTag(
                formDeclaration,
                initialState,
                FormValidationState.valid
            )))
        ),
        Method.POST / Root / id / "form" -> handler { (req: Request) =>
            req.body.asURLEncodedForm.map { form =>
                val raw = form.formData
                    .collect {
                        case zio.http.FormField.Simple(name, value)     => name -> value
                        case zio.http.FormField.Text(name, value, _, _) => name -> value
                    }
                    .groupMap(_._1)(_._2).view.mapValues(_.toSeq).toMap
                respond(raw, req.headers.get("HX-Request").contains("true"))
            }.orDie
        }
    )
end SsrFormScenario
