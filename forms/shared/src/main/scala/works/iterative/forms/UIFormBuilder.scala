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
        yield UIForm(path.toHtmlId, path.last, children.flatten, state, context)
        end for
    end render

    def renderSegment(path: AbsolutePath, repeatIndex: Option[Int] = None)(element: SectionSegment)
        : ZPure[Nothing, Unit, Unit, FormState & FormValidationState, Nothing, Seq[UIFormElement]] =
        element match
            case Section(id, elems, _) => renderSection(path / id, repeatIndex)(elems).map(List(_))
            case Field(id, fieldType, default, optional) =>
                if fieldType.hidden then
                    renderHiddenField(path / id, default).map(List(_))
                else renderField(path / id, fieldType.id, default, optional).map(List(_))
            case File(id, multiple, optional) =>
                renderFileField(path / id, multiple, optional).map(List(_))
            case Date(id)    => renderField(path / id, "date", None, optional = true).map(List(_))
            case Display(id) => renderDisplay(path / id).map(List(_))
            case Button(id)  => renderButton(path / id).map(List(_))
            case Enum(id, values, default) =>
                renderChoiceField(path / id, values, default).map(List(_))
            case ShowIf(condition, elem) =>
                resolveCondition(path)(condition).flatMap(if _ then renderSegment(path)(elem)
                else ZPure.succeed(Nil))
            case repeated @ Repeated(id, _, _, _) =>
                for
                    instances <- ZPure.serviceWith[FormState](
                        Repeated.instances(path, repeated, _)
                    )
                    rendered <- ZPure.foreach(instances): i =>
                        renderSegment(i.path, Some(i.index))(i.segment)
                yield
                    // Hidden fields carry the item list so it round-trips through HTML forms
                    val itemsPath = path / id / "__items"
                    val itemFields = instances.map: i =>
                        UIHiddenField(
                            s"${itemsPath.toHtmlId}-${i.index}",
                            itemsPath.toHtmlName,
                            Some(s"${i.item}:${i.itemType}")
                        )
                    itemFields ++ rendered.flatten
                end for

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
        yield UIFormSection(path.toHtmlId, path.size, path.last, Seq(children), Nil, repeatIndex)
    end renderSection

    private def optionalDecoration(optional: Boolean) =
        if !optional then List(UIFieldDecoration.Required) else Nil

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
        fieldType: UIFieldType,
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
                fieldType,
                value.orElse(default),
                Nil
            ),
            optionalDecoration(optional) ++ errors
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

    private def renderButton(path: AbsolutePath) = ZPure.succeed[Unit, UIFormElement]:
        UIButton(path.toHtmlId, path.toHtmlName, "button", path.last, Nil)

    private def renderChoiceField(
        path: AbsolutePath,
        values: List[String],
        default: Option[String]
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
            errors
        )

    private def resolveCondition(path: AbsolutePath)(condition: Condition)
        : ZPure[Nothing, Unit, Unit, FormState & FormValidationState, Nothing, Boolean] =
        for
            state <- ZPure.service[Unit, FormState]
            validation <- ZPure.service[Unit, FormValidationState]
        yield Condition.eval(condition, path, state.getString, validation.isValid)
end UIFormBuilder
