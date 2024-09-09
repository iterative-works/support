package portaly.forms

import zio.*
import works.iterative.ui.model.forms.*
import scala.xml.*
import works.iterative.core.MessageCatalogue
import works.iterative.autocomplete.service.AutocompleteService
import works.iterative.core.Language
import portaly.forms.service.AutocompleteResolver
import works.iterative.core.MessageArg
import works.iterative.core.UserMessage
import scala.annotation.nowarn

// Scala XML compiler bug: https://github.com/scala/bug/issues/12658
@nowarn("msg=unused value of type scala.xml.NodeBuffer")
class UIFormXMLRenderer(
    autocompleteService: AutocompleteService,
    autocompleteResolver: AutocompleteResolver,
    displayResolver: DisplayResolver[FormState, UIO[NodeSeq]],
    data: FormState
):
    def render(form: UIForm)(using messages: MessageCatalogue, lang: Language): UIO[NodeSeq] =
        given MessageCatalogue = messages.nested(form.messageKey.value)
        for
            nested <- ZIO.foreach(form.children)(renderSegment)
        yield <ui:form xmlns:ui="https://ui.iterative.works/form" title={
            renderMessage(form.messageKey, "title")
        }>{nested}</ui:form>
    end render

    private def renderMessage(key: UIMessageKey, suffix: String, args: List[MessageArg] = Nil)(
        using messages: MessageCatalogue
    ): String =
        messages.get(UserMessage(key.append(suffix), args*)).orNull

    def renderSegment(segment: UIFormElement)(using
        messages: MessageCatalogue,
        lang: Language
    ): UIO[NodeSeq] =
        segment match
            case UIFormSection(id, level, messageKey, children, decorations, repeatIndex) =>
                given MessageCatalogue = messages.nested(messageKey.value)
                for
                    nested <- ZIO.foreach(children)(renderSegment)
                yield <ui:section id={id} title={
                    renderMessage(messageKey, "section", repeatIndex.toList.map(_ + 1))
                } subtitle={
                    renderMessage(messageKey, "section.subtitle")
                }>
                            {nested}
                    </ui:section>
            case UILabeledField(id, messageKey, field, decorations) =>
                field match
                    case _: UIChoiceField =>
                        for rendered <- renderField(field)(using messages.nested(messageKey.value))
                        yield <ui:choiceField id={id} label={renderMessage(messageKey, "label")}>
                            {rendered}
                        </ui:choiceField>
                    case _ =>
                        for rendered <- renderField(field)
                        yield <ui:labeledField id={id} label={renderMessage(messageKey, "label")}>
                            {rendered}
                        </ui:labeledField>
            case UIGrid(elems) =>
                def renderCell(gridCell: UIGridCell): UIO[NodeSeq] =
                    for nested <- ZIO.foreach(gridCell.children)(renderSegment)
                    yield <ui:gridCell>{nested}</ui:gridCell>

                for
                    nested <- ZIO.foreach(elems)(e =>
                        ZIO.foreach(e)(renderCell).map(r => <ui:gridRow>{r}</ui:gridRow>)
                    )
                yield <ui:grid>{nested}</ui:grid>
            case UIFlexRow(children) =>
                for nested <- ZIO.foreach(children)(renderSegment)
                yield <ui:flexRow>{nested}</ui:flexRow>
            case UIBlock(id, messageKey) =>
                for content <- displayResolver.resolve(
                        works.iterative.ui.model.forms.IdPath.FullPath(id.split("-").toVector),
                        data
                    )
                yield <ui:block id={id} title={renderMessage(messageKey, "title")}>
                    {content}
                </ui:block>
            case _ => ZIO.succeed(NodeSeq.Empty)

    private def renderField(field: UIField)(using
        messages: MessageCatalogue,
        lang: Language
    ): UIO[NodeSeq] =
        field match
            case UITextField(id, fieldName, fieldType, rawValue, decorations) =>
                def resolved: UIO[Option[String]] =
                    ZIO.foreach(autocompleteResolver.resolveAutocomplete(fieldType)):
                        autocompleteId =>
                            ZIO.foreach(rawValue): v =>
                                autocompleteService.load(autocompleteId, v, lang.value, None).map(
                                    _.map(_.label).getOrElse(v)
                                )
                    .map(_.flatten.orElse(rawValue))

                resolved.map: resolvedValue =>
                    resolvedValue match
                        case Some(v) => <ui:inputValue>{
                                if fieldType.startsWith("number") then v.replace(',', '.') else v
                            }</ui:inputValue>
                        case _ => <ui:inputValue/>
            case UIFileField(id, fieldName, fileList, multiple, decorations) =>
                ZIO.succeed:
                    <ui:filesValue>
                        {
                        fileList.getOrElse(List.empty).map: file =>
                            <ui:fileValue>{file}</ui:fileValue>
                    }
                    </ui:filesValue>
            case UIChoiceField(id, fieldName, rawValue, values, decorations) =>
                ZIO.succeed:
                    rawValue.flatMap(v => values.find(_.value == v)).map(_.messageKey) match
                        case Some(key) =>
                            <ui:inputValue value={
                                rawValue.getOrElse("")
                            }>{renderMessage(key, "label")}</ui:inputValue>
                        case _ => <ui:inputValue value={rawValue.getOrElse("")}/>
end UIFormXMLRenderer
