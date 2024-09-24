package portaly.forms

import com.raquo.laminar.api.L.*
import works.iterative.ui.model.forms.*
import works.iterative.ui.laminar.*
import works.iterative.autocomplete.ui.AutocompleteComponents
import portaly.forms.impl.ReadOnlyHtmlDisplayResolver
import works.iterative.core.FileRef
import works.iterative.ui.components.FileComponents
import works.iterative.core.UserMessage
import works.iterative.core.MessageCatalogue
import works.iterative.core.Language
import scala.annotation.unused

class UIFormReadOnlyRenderer(
    displayResolver: ReadOnlyHtmlDisplayResolver,
    val cs: ReadOnlyComponents,
    acs: AutocompleteComponents,
    fcs: FileComponents,
    hooks: UIFormReadOnlyHooks,
    // TODO: replace with hooks
    isInline: UIFormId => Boolean = _ => false
)(using MessageCatalogue, Language):
    def withComponents(components: ReadOnlyComponents): UIFormReadOnlyRenderer =
        UIFormReadOnlyRenderer(displayResolver, components, acs, fcs, hooks, isInline)

    def addHooks(hooks: UIFormReadOnlyHooks): UIFormReadOnlyRenderer =
        UIFormReadOnlyRenderer(
            displayResolver,
            cs,
            acs,
            fcs,
            hooks.composeWith(this.hooks),
            isInline
        )

    def render(form: UIForm): HtmlElement =
        inMessageContext(form.messageKey)(
            cs.form(form.id, renderMessage("title"), form.children.map(renderSegment(form.data)))
        )
    end render

    def renderSection(form: UIForm, sectionId: UIFormId): HtmlElement =
        def findSections(sections: Seq[UIFormElement]): Seq[UIFormSection] =
            sections.collect {
                case section: UIFormSection => section
            }.find(_.id == sectionId).toList
        inMessageContext(form.messageKey)(findSections(form.children).map(renderSegment(form.data)))
    end renderSection

    private def renderMessage(key: UIMessageKey): Node =
        UserMessage(key).node

    private def renderMessage(key: UIMessageKey, repeatIndex: Option[Int]): Node =
        UserMessage(key, repeatIndex.map(_ + 1).toList*).node

    private def renderMessage(key: UIMessageKey, suffix: String): Node =
        UserMessage(key.append(suffix)).node

    def renderSegment(data: FormState)(segment: UIFormElement): HtmlElement =
        segment match
            case i @ UIFormSection(id, level, messageKey, children, decorations, repeatIndex) =>
                val advisedRender = hooks.adviceAroundSection(i, data, renderSection)
                div(inMessageContext(messageKey)(advisedRender(i, data)))
            case UILabeledField(_, _, UITextField(_, _, _, None, _), _)       => div()
            case UILabeledField(_, _, UITextField(_, _, _, Some(""), _), _)   => div()
            case UILabeledField(_, _, UIChoiceField(_, _, None, _, _), _)     => div()
            case UILabeledField(_, _, UIChoiceField(_, _, Some(""), _, _), _) => div()
            case i @ UILabeledField(id, messageKey, field, decorations) => field match
                    case _: UIChoiceField =>
                        val advisedRender =
                            hooks.adviceAroundLabeledField(i, data, renderChoiceField)
                        advisedRender(i, data)
                    case _ =>
                        val advisedRender =
                            hooks.adviceAroundLabeledField(i, data, renderLabeledField)
                        advisedRender(i, data)
            case UIGrid(elems) =>
                cs.grid:
                    elems.flatMap: segments =>
                        segments.map: gridCell =>
                            cs.gridCell(gridCell.size, gridCell.children.map(renderSegment(data)))
            case UIFlexRow(children) =>
                cs.flexRow(children.map(renderSegment(data)))
            case UIBlock(id, messageKey) =>
                displayResolver.resolve(
                    works.iterative.ui.model.forms.IdPath.FullPath(id.split("-").toVector),
                    data
                )
            case _ => div()

    private def renderSection(section: UIFormSection, data: FormState): HtmlElement =
        val UIFormSection(id, level, messageKey, children, decorations, repeatIndex) = section
        cs.section(
            id,
            level,
            Some(renderMessage(s"${id.split("-").last}.section", repeatIndex)),
            Some(renderMessage(
                s"${id.split("-").last}.section.subtitle",
                repeatIndex
            )),
            children.map(renderSegment(data)),
            idAttr(id)
        )
    end renderSection

    private def renderChoiceField(field: UILabeledField, @unused data: FormState): HtmlElement =
        val UILabeledField(id, messageKey, theField, decorations) = field
        cs.labeledField(
            id,
            renderMessage(messageKey, "label"),
            false,
            isInline(id),
            inMessageContext(messageKey)(renderField(theField))
        )
    end renderChoiceField

    private def renderLabeledField(field: UILabeledField, @unused data: FormState): HtmlElement =
        val UILabeledField(id, messageKey, theField, decorations) = field
        cs.labeledField(
            id,
            renderMessage(messageKey, "label"),
            false,
            isInline(id),
            inMessageContext(messageKey)(renderField(theField))
        )
    end renderLabeledField

    private def renderField(field: UIField): Node =
        field match
            case UITextField(id, fieldName, fieldType, rawValue, decorations) =>
                rawValue match
                    case Some(v) => cs.inputValue(id, v, acs.labelFor(fieldType, v))
                    case _       => cs.inputValue(id, "", "")
            case UIFileField(id, fieldName, fileList, multiple, decorations) =>
                def uiFileToNode(file: UIFile): Node = file match
                    case s: String               => s
                    case f: org.scalajs.dom.File => f.name
                    case r: FileRef              => fcs.renderFileLink(r)

                fileList.getOrElse(List.empty).map(uiFileToNode) match
                    case Nil   => cs.inputValue(id, "", "")
                    case files => ul(files.map(f => li(cs.fileValue(id, f))))
            case UIChoiceField(id, fieldName, rawValue, values, decorations) =>
                rawValue.flatMap(v => values.find(_.value == v)).map(_.messageKey) match
                    case Some(key) =>
                        cs.inputValue(id, rawValue.getOrElse(""), renderMessage(key, "label"))
                    case _ => cs.inputValue(id, rawValue.getOrElse(""), "")
end UIFormReadOnlyRenderer
