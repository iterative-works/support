// PURPOSE: Renders a UIForm as a server-side HTML form using scalatags
// PURPOSE: Plain POST form enriched with HTMX attributes for change-triggered re-render (FC-D4)

package portaly.forms

import scalatags.Text.all.*
import scalatags.Text.tags2
import works.iterative.core.{Language, MessageArg, MessageCatalogue, UserMessage}
import works.iterative.ui.model.forms.*

class UIFormHtmlRenderer(displayResolver: DisplayResolver[FormState, Frag]):

    def render(form: UIForm, postAction: String)(using messages: MessageCatalogue): Tag =
        given MessageCatalogue = messages.nested(form.messageKey.value)
        given UIForm = form
        tag("form")(
            id := form.id,
            method := "post",
            action := postAction,
            attr("hx-post") := postAction,
            attr("hx-trigger") := "change",
            attr("hx-target") := "this",
            attr("hx-swap") := "outerHTML"
        )(
            renderMessage(form.messageKey, "title").map(h1(_)),
            form.children.map(renderElement),
            div(cls := "form-actions")(
                button(`type` := "submit")(
                    renderMessage(form.messageKey, "submit").getOrElse("Submit"): String
                )
            )
        )
    end render

    private def renderMessage(key: UIMessageKey, suffix: String, args: List[MessageArg] = Nil)(
        using messages: MessageCatalogue
    ): Option[String] =
        messages.opt(UserMessage(key.append(suffix), args*))

    private def renderElement(element: UIFormElement)(using
        form: UIForm,
        messages: MessageCatalogue
    ): Frag =
        element match
            case UIFormSection(sid, level, messageKey, children, _, repeatIndex) =>
                given MessageCatalogue = messages.nested(messageKey.value)
                tags2.section(id := sid)(
                    renderMessage(messageKey, "section", repeatIndex.toList.map(_ + 1))
                        .map(heading(level)(_)),
                    renderMessage(messageKey, "section.subtitle").map(p(cls := "subtitle")(_)),
                    children.map(renderElement)
                )
            case UILabeledField(fid, messageKey, field, decorations) =>
                given MessageCatalogue = messages.nested(messageKey.value)
                val required = decorations.contains(UIFieldDecoration.Required)
                div(cls := "field")(
                    renderMessage(messageKey, "label").map(l =>
                        label(`for` := fid)(l, Option.when(required)(span(cls := "required")("*")))
                    ),
                    renderField(field, required),
                    renderErrors(decorations)
                )
            case UIHiddenField(fid, fieldName, fieldValue) =>
                input(
                    `type` := "hidden",
                    id := fid,
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
            case UIButton(bid, buttonName, _, messageKey, _) =>
                // SSR degradation: named submit button so the server sees which button fired
                button(id := bid, name := buttonName, value := buttonName, `type` := "submit")(
                    renderMessage(messageKey, "label").getOrElse(messageKey.value): String
                )
            case UIBlock(bid, messageKey) =>
                given Language = messages.language
                div(id := bid, cls := "block")(
                    renderMessage(messageKey, "title").map(heading(3)(_)),
                    displayResolver.resolve(
                        IdPath.FullPath(bid.split("-").toVector),
                        form.data
                    )
                )

    private def renderField(field: UIField, required: Boolean)(using
        messages: MessageCatalogue
    ): Frag =
        val requiredAttr = Option.when(required)(attr("required") := "required")
        field match
            case UITextField(fid, fieldName, fieldType, rawValue, _) =>
                fieldType match
                    case "prose" =>
                        textarea(id := fid, name := fieldName, requiredAttr)(
                            rawValue.getOrElse(""): String
                        )
                    case t =>
                        input(
                            `type` := htmlInputType(t),
                            id := fid,
                            name := fieldName,
                            rawValue.map(value := _),
                            requiredAttr
                        )
            case UIFileField(fid, fieldName, fileList, multipleFiles, _) =>
                frag(
                    input(
                        `type` := "file",
                        id := fid,
                        name := fieldName,
                        Option.when(multipleFiles)(multiple),
                        requiredAttr
                    ),
                    fileList.filter(_.nonEmpty).map(files =>
                        ul(cls := "files")(files.map(file => li(fileLabel(file))))
                    )
                )
            case UIChoiceField(fid, fieldName, rawValue, values, _) =>
                select(id := fid, name := fieldName, requiredAttr)(
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

    private def htmlInputType(fieldType: UIFieldType): String = fieldType match
        case "date" | "email" | "tel" | "password" | "checkbox" => fieldType
        case t if t.startsWith("number")                        => "number"
        case _                                                  => "text"

    private def fileLabel(file: UIFile): String = file match
        case name: String                      => name
        case ref: works.iterative.core.FileRef => ref.name
end UIFormHtmlRenderer
