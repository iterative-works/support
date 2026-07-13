// PURPOSE: Renders a UIForm as a server-side HTML form using scalatags
// PURPOSE: Plain POST form enriched by a FormTransport for change-triggered re-render (FC-D4)

package works.iterative.forms

import scalatags.Text.all.*
import scalatags.Text.tags2
import works.iterative.core.{Language, MessageArg, MessageCatalogue, UserMessage}
import works.iterative.ui.model.forms.*

class UIFormHtmlRenderer(
    displayResolver: DisplayResolver[FormState, Frag],
    transport: FormTransport
):

    def render(form: UIForm, postAction: String)(using messages: MessageCatalogue): Tag =
        given formMessages: MessageCatalogue = messages.nested(form.messageKey.value)
        given UIForm = form
        tag("form")(
            id := form.id.toHtmlId,
            method := "post",
            action := postAction,
            transport.formAttributes(postAction),
            // novalidate: transports validate forms before requests unless noValidate is set,
            // which would block change re-renders while required fields are still blank.
            // Validation is the server's job; required attributes stay for a11y/styling.
            attr("novalidate").empty
        )(
            renderMessage(form.messageKey, "title").map(h1(_)),
            Option.when(form.errors.nonEmpty)(
                div(cls := "form-errors")(
                    form.errors.map(msg => span(cls := "form-error")(formMessages(msg)))
                )
            ),
            form.children.map(renderElement),
            // Chrome submit only when the form declares no submit button of its own
            Option.unless(hasDeclaredSubmit(form.children))(
                div(cls := "form-actions")(
                    // Named so the server can tell real submissions from change-triggered re-renders
                    button(name := "__submit", value := "submit", `type` := "submit")(
                        renderMessage(form.messageKey, "submit").getOrElse("Submit"): String
                    )
                )
            )
        )
    end render

    private def hasDeclaredSubmit(elements: Seq[UIFormElement]): Boolean =
        elements.exists:
            case UIButton(_, _, UIButtonIntent.Submit, _, _) => true
            case UIFormSection(_, _, _, children, _, _)      => hasDeclaredSubmit(children)
            case UIGrid(rows)        => hasDeclaredSubmit(rows.flatten.flatMap(_.children))
            case UIFlexRow(children) => hasDeclaredSubmit(children)
            case _                   => false

    private def renderMessage(key: UIMessageKey, suffix: String, args: List[MessageArg] = Nil)(
        using messages: MessageCatalogue
    ): Option[String] =
        messages.opt(UserMessage(key.append(suffix), args*))

    private def renderElement(element: UIFormElement)(using
        form: UIForm,
        messages: MessageCatalogue
    ): Frag =
        element match
            case UIFormSection(sid, level, messageKey, children, decorations, repeatIndex) =>
                given MessageCatalogue = messages.nested(messageKey.value)
                tags2.section(id := sid.toHtmlId)(
                    renderMessage(messageKey, "section", repeatIndex.toList.map(_ + 1))
                        .map(heading(level)(_)),
                    renderMessage(messageKey, "section.subtitle").map(p(cls := "subtitle")(_)),
                    renderErrors(decorations),
                    children.map(renderElement)
                )
            case UILabeledField(fid, messageKey, field, decorations) =>
                given MessageCatalogue = messages.nested(messageKey.value)
                val required = decorations.contains(UIFieldDecoration.Required)
                val fieldDisabled = decorations.contains(UIFieldDecoration.Disabled)
                div(cls := "field")(
                    renderMessage(messageKey, "label").map(l =>
                        label(`for` := fid.toHtmlId)(
                            l,
                            Option.when(required)(span(cls := "required")("*"))
                        )
                    ),
                    renderField(field, required, fieldDisabled),
                    renderErrors(decorations)
                )
            case UIHiddenField(fid, fieldName, fieldValue) =>
                input(
                    `type` := "hidden",
                    id := fid.toHtmlId,
                    name := fieldName,
                    value := fieldValue.getOrElse("")
                )
            case UIGrid(rows) =>
                div(cls := "grid")(
                    rows.map(row =>
                        div(cls := "grid-row")(
                            row.map(gridCell =>
                                div(cls := "grid-cell")(gridCell.children.map(renderElement))
                            )
                        )
                    )
                )
            case UIFlexRow(children) =>
                div(cls := "flex-row")(children.map(renderElement))
            case UIRepeatedGroup(gid, fieldName, _, _, rows, decorations) =>
                div(id := gid.toHtmlId, cls := "repeated-group")(
                    rows.map(row =>
                        div(cls := "repeated-row")(
                            // The item entry rides a hidden input so the list round-trips
                            input(
                                `type` := "hidden",
                                name := fieldName,
                                value := s"${row.item}:${row.itemType}"
                            ),
                            row.children.map(renderElement)
                        )
                    ),
                    renderErrors(decorations)
                )
            case UIButton(bid, buttonName, intent, messageKey, _) =>
                val buttonLabel: String =
                    renderMessage(messageKey, "label").getOrElse(messageKey.value)
                intent match
                    case UIButtonIntent.Submit =>
                        button(
                            id := bid.toHtmlId,
                            name := "__submit",
                            value := "submit",
                            `type` := "submit"
                        )(buttonLabel)
                    case UIButtonIntent.ServerAction =>
                        // Named submit button so the server sees which button fired
                        button(
                            id := bid.toHtmlId,
                            name := buttonName,
                            value := buttonName,
                            `type` := "submit"
                        )(buttonLabel)
                    case UIButtonIntent.ClientAction =>
                        // Inert without client-side code; SSR degradation is a no-op button
                        button(id := bid.toHtmlId, `type` := "button")(buttonLabel)
                end match
            case UIBlock(bid, messageKey) =>
                given Language = messages.language
                div(id := bid.toHtmlId, cls := "block")(
                    renderMessage(messageKey, "title").map(heading(3)(_)),
                    displayResolver.resolve(bid, form.data)
                )

    private def renderField(field: UIField, required: Boolean, fieldDisabled: Boolean)(using
        messages: MessageCatalogue
    ): Frag =
        val requiredAttr = Option.when(required)(attr("required") := "required")
        val disabledAttr = Option.when(fieldDisabled)(disabled)
        // Disabled controls never submit; a hidden mirror keeps their value in the POST loop
        def disabledMirror(fieldName: UIFieldName, rawValue: Option[String]) =
            Option.when(fieldDisabled)(
                input(`type` := "hidden", name := fieldName, value := rawValue.getOrElse(""))
            )
        field match
            case UITextField(fid, fieldName, fieldType, rawValue, _) =>
                FieldKind.of(fieldType) match
                    case FieldKind.Prose =>
                        frag(
                            textarea(
                                id := fid.toHtmlId,
                                name := fieldName,
                                requiredAttr,
                                disabledAttr
                            )(
                                rawValue.getOrElse(""): String
                            ),
                            disabledMirror(fieldName, rawValue)
                        )
                    case kind =>
                        frag(
                            input(
                                `type` := htmlInputType(kind),
                                id := fid.toHtmlId,
                                name := fieldName,
                                rawValue.map(value := _),
                                requiredAttr,
                                disabledAttr
                            ),
                            disabledMirror(fieldName, rawValue)
                        )
            case UIFileField(fid, fieldName, fileList, multipleFiles, _) =>
                frag(
                    input(
                        `type` := "file",
                        id := fid.toHtmlId,
                        name := fieldName,
                        Option.when(multipleFiles)(multiple),
                        requiredAttr
                    ),
                    fileList.filter(_.nonEmpty).map(files =>
                        ul(cls := "files")(files.map(file => li(fileLabel(file))))
                    )
                )
            case UIChoiceField(fid, fieldName, rawValue, values, _) =>
                select(id := fid.toHtmlId, name := fieldName, requiredAttr)(
                    Option.when(rawValue.isEmpty)(option(value := "")("")),
                    values.map(choice =>
                        option(
                            value := choice.value,
                            Option.when(rawValue.contains(choice.value))(
                                attr("selected") := "selected"
                            )
                        )(
                            renderMessage(choice.messageKey, "label").getOrElse(
                                choice.value
                            ): String
                        )
                    )
                )
        end match
    end renderField

    private def renderErrors(decorations: List[UIFieldDecoration])(using
        messages: MessageCatalogue
    ): Frag =
        val errors = decorations.collect { case UIFieldDecoration.ErrorMessage(msg) => msg }
        Option.when(errors.nonEmpty)(
            div(cls := "field-errors")(
                errors.map(msg => span(cls := "field-error")(messages(msg)))
            )
        )
    end renderErrors

    private def heading(level: Int): ConcreteHtmlTag[String] = level match
        case 1 => h1
        case 2 => h2
        case 3 => h3
        case 4 => h4
        case 5 => h5
        case _ => h6

    private def htmlInputType(kind: FieldKind): String = kind match
        case FieldKind.Date                              => "date"
        case FieldKind.Email                             => "email"
        case FieldKind.Phone                             => "tel"
        case FieldKind.Checkbox                          => "checkbox"
        case FieldKind.Number(_)                         => "number"
        case FieldKind.Custom(id @ ("tel" | "password")) => id
        case FieldKind.Text | FieldKind.Hidden | FieldKind.Prose | FieldKind.Select |
            FieldKind.Zip | FieldKind.Country | FieldKind.Ruian | FieldKind.Custom(_) => "text"

    private def fileLabel(file: UIFile): String = file match
        case name: String                      => name
        case ref: works.iterative.core.FileRef => ref.name
end UIFormHtmlRenderer
