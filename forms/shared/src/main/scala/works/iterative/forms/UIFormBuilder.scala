package portaly.forms

import works.iterative.ui.model.forms.*
import zio.prelude.fx.ZPure
import zio.ZEnvironment

class UIFormBuilder(layoutResolver: LayoutResolver, formHook: Option[UIForm => UIForm] = None):
    def buildForm(
        form: Form,
        state: FormState,
        validationState: FormValidationState,
        context: Option[Map[String, String]]
    ): UIForm =
        val result = render(IdPath.Root / form.id, form.elems, context).provideEnvironment(
            ZEnvironment(state, validationState)
        ).run
        formHook match
            case Some(hook) => hook(result)
            case None       => result
    end buildForm

    def render(
        path: AbsolutePath,
        elems: List[SectionSegment],
        context: Option[Map[String, String]]
    ): ZPure[Nothing, Unit, Unit, FormState & FormValidationState, Nothing, UIForm] =
        for
            children <- ZPure.foreach(elems)(renderSegment(path))
            state <- ZPure.service[Unit, FormState]
            errors <- ZPure.serviceWith[FormValidationState](_.errors(path))
        yield UIForm(path.toHtmlId, path.last, children.flatten, state, context, errors)
        end for
    end render

    def renderSegment(path: AbsolutePath, repeatIndex: Option[Int] = None)(element: SectionSegment)
        : ZPure[Nothing, Unit, Unit, FormState & FormValidationState, Nothing, Seq[UIFormElement]] =
        element match
            case Section(id, elems, _) => renderSection(path / id, repeatIndex)(elems).map(List(_))
            case Field(id, fieldType, default, optional, _) =>
                if fieldType.hidden then
                    renderHiddenField(path / id, default).map(List(_))
                else renderField(path / id, fieldType, default, optional).map(List(_))
            case File(id, multiple, optional) =>
                renderFileField(path / id, multiple, optional).map(List(_))
            case Date(id, optional) =>
                renderField(path / id, FieldType("date"), None, optional).map(List(_))
            case Display(id)        => renderDisplay(path / id).map(List(_))
            case Button(id, intent) => renderButton(path / id, intent).map(List(_))
            case Enum(id, values, default, optional) =>
                renderChoiceField(path / id, values, default, optional).map(List(_))
            case ShowIf(condition, elem) =>
                resolveCondition(path)(condition).flatMap(if _ then renderSegment(path)(elem)
                else ZPure.succeed(Nil))
            case repeated @ Repeated(id, _, optional, elems) =>
                for
                    instances <- ZPure.serviceWith[FormState](
                        Repeated.instances(path, repeated, _)
                    )
                    rows <- ZPure.foreach(instances): i =>
                        renderSegment(i.path, Some(i.index))(i.segment)
                            .map(UIRepeatedRow(i.item, i.itemType, i.index, _))
                    errors <- errorDecorations(path / id)
                yield List(UIRepeatedGroup(
                    (path / id).toHtmlId,
                    (path / id / "__items").toHtmlName,
                    elems.map(_.id.last),
                    optional,
                    rows,
                    errors
                ))

    private def getString(path: AbsolutePath)
        : ZPure[Nothing, Unit, Unit, FormState, Nothing, Option[String]] =
        ZPure.serviceWith[FormState](_.getString(path))

    private def getFileList(path: AbsolutePath)
        : ZPure[Nothing, Unit, Unit, FormState, Nothing, Option[List[UIFile]]] =
        ZPure.serviceWith[FormState](_.getFileList(path))

    private def renderSection(
        path: AbsolutePath,
        repeatIndex: Option[Int]
    )(elems: List[SectionSegment]) =
        val layout = layoutResolver.resolve(path, elems)
        val content = layout match
            case Grid(elems) =>
                ZPure.foreach(elems): row =>
                    ZPure.foreach(row): elems =>
                        renderSegment(path)(elems).map(UIGridCell(row.size, _))
                .map(UIGrid(_))
            case Flex(elems) =>
                ZPure.foreach(elems):
                    renderSegment(path)
                .map: row =>
                    UIFlexRow(row.flatten)
        for
            children <- content
            errors <- errorDecorations(path)
        yield UIFormSection(path.toHtmlId, path.size, path.last, Seq(children), errors, repeatIndex)
    end renderSection

    private def optionalDecoration(optional: Boolean) =
        if !optional then List(UIFieldDecoration.Required) else Nil

    private def fieldTypeDecorations(fieldType: FieldType): List[UIFieldDecoration] =
        (if fieldType.disabled then List(UIFieldDecoration.Disabled) else Nil)
            ++ fieldType.context.map(UIFieldDecoration.Context(_))

    private def errorDecorations(path: AbsolutePath)
        : ZPure[Nothing, Unit, Unit, FormValidationState, Nothing, List[UIFieldDecoration]] =
        ZPure.serviceWith[FormValidationState](
            _.errors(path).map(UIFieldDecoration.ErrorMessage.apply)
        )

    private def renderHiddenField(
        path: AbsolutePath,
        default: Option[String]
    ) = getString(path).map: value =>
        UIHiddenField(
            path.toHtmlId,
            path.toHtmlName,
            value.orElse(default)
        )

    private def renderField(
        path: AbsolutePath,
        fieldType: FieldType,
        default: Option[String],
        optional: Boolean
    ) =
        for
            value <- getString(path)
            errors <- errorDecorations(path)
        yield UILabeledField(
            path.toHtmlId,
            path.last,
            UITextField(
                path.toHtmlId,
                path.toHtmlName,
                fieldType.id,
                value.orElse(default),
                Nil
            ),
            optionalDecoration(optional) ++ fieldTypeDecorations(fieldType) ++ errors
        )

    private def renderFileField(
        path: AbsolutePath,
        multiple: Boolean,
        optional: Boolean
    ) =
        for
            files <- getFileList(path)
            errors <- errorDecorations(path)
        yield UILabeledField(
            path.toHtmlId,
            path.last,
            UIFileField(
                path.toHtmlId,
                path.toHtmlName,
                files,
                multiple,
                Nil
            ),
            optionalDecoration(optional) ++ errors
        )

    private def renderDisplay(path: AbsolutePath) = ZPure.succeed[Unit, UIFormElement]:
        UIBlock(path.toHtmlId, path.last)

    private def renderButton(path: AbsolutePath, intent: ButtonIntent) =
        val uiIntent = intent match
            case ButtonIntent.Submit       => UIButtonIntent.Submit
            case ButtonIntent.ServerAction => UIButtonIntent.ServerAction
            case ButtonIntent.ClientAction => UIButtonIntent.ClientAction
        ZPure.succeed[Unit, UIFormElement]:
            UIButton(path.toHtmlId, path.toHtmlName, uiIntent, path.last, Nil)
    end renderButton

    private def renderChoiceField(
        path: AbsolutePath,
        values: List[String],
        default: Option[String],
        optional: Boolean
    ) =
        for
            value <- getString(path)
            errors <- errorDecorations(path)
        yield UILabeledField(
            path.toHtmlId,
            path.last,
            UIChoiceField(
                path.toHtmlId,
                path.toHtmlName,
                value.orElse(default),
                values.map: v =>
                    val o = path / v
                    UIChoiceOption(o.toHtmlId, v, v)
                ,
                Nil
            ),
            optionalDecoration(optional) ++ errors
        )

    private def resolveCondition(path: AbsolutePath)(condition: Condition)
        : ZPure[Nothing, Unit, Unit, FormState & FormValidationState, Nothing, Boolean] =
        for
            state <- ZPure.service[Unit, FormState]
            validation <- ZPure.service[Unit, FormValidationState]
        yield Condition.eval(condition, path, state.getString, validation.isValid)
end UIFormBuilder
