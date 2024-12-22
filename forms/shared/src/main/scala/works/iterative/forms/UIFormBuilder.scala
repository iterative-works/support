package portaly.forms

import works.iterative.ui.model.forms.*
import zio.prelude.fx.ZPure
import zio.ZEnvironment
import works.iterative.ui.model.forms.FormState

class UIFormBuilder(layoutResolver: LayoutResolver, formHook: Option[UIForm => UIForm] = None):
    def buildForm(
        form: Form,
        state: FormState,
        validationState: FormValidationState
    ): UIForm =
        val result = render(IdPath.Root / form.id, form.elems).provideEnvironment(
            ZEnvironment(state, validationState)
        ).run
        formHook match
            case Some(hook) => hook(result)
            case None       => result
    end buildForm

    def render(
        path: AbsolutePath,
        elems: List[SectionSegment]
    ): ZPure[Nothing, Unit, Unit, FormState & FormValidationState, Nothing, UIForm] =
        for
            children <- ZPure.foreach(elems)(renderSegment(path))
            state <- ZPure.service[Unit, FormState]
        yield UIForm(path.toHtmlId, path.last, children.flatten, state)
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
            case Repeated(id, default, _, elems) =>
                val elemList = elems.map(e => e.id.last -> e)
                val elemMap = elemList.toMap
                val defaultSegment = elemList.head._2
                for
                    items <- getItemsFor(path / id)
                    rendered <- ZPure.foreach(items.zipWithIndex):
                        case ((i, t), idx) =>
                            renderSegment(path / id / i, Some(idx))(elemMap.getOrElse(
                                t,
                                defaultSegment
                            ))
                yield rendered.flatten
                end for

    private def getString(path: AbsolutePath)
        : ZPure[Nothing, Unit, Unit, FormState, Nothing, Option[String]] =
        ZPure.serviceWith[FormState](_.getString(path))

    private def getFileList(path: AbsolutePath)
        : ZPure[Nothing, Unit, Unit, FormState, Nothing, Option[List[UIFile]]] =
        ZPure.serviceWith[FormState](_.getFileList(path))

    private def getItemsFor(path: AbsolutePath)
        : ZPure[Nothing, Unit, Unit, FormState, Nothing, List[(String, String)]] =
        ZPure.serviceWith[FormState](_.itemsFor(path))

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
    ) = getString(path).map: value =>
        UILabeledField(
            path.toHtmlId,
            path.last,
            UITextField(
                path.toHtmlId,
                path.toHtmlName,
                fieldType,
                value.orElse(default),
                Nil
            ),
            optionalDecoration(optional)
        )

    private def renderFileField(
        path: AbsolutePath,
        multiple: Boolean,
        optional: Boolean
    ) = getFileList(path).map: files =>
        UILabeledField(
            path.toHtmlId,
            path.last,
            UIFileField(
                path.toHtmlId,
                path.toHtmlName,
                files,
                multiple,
                Nil
            ),
            optionalDecoration(optional)
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
        getString(path).map: value =>
            UILabeledField(
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
                Nil
            )

    private def resolveCondition(path: AbsolutePath)(condition: Condition)
        : ZPure[Nothing, Unit, Unit, FormState & FormValidationState, Nothing, Boolean] =
        import Condition.*
        condition match
            case Never  => ZPure.succeed(false)
            case Always => ZPure.succeed(true)
            case AnyOf(conditions*) =>
                ZPure.foreach(conditions)(resolveCondition(path)).map(_.reduce(_ || _))
            case AllOf(conditions*) =>
                ZPure.foreach(conditions)(resolveCondition(path)).map(_.reduce(_ && _))
            case IsEqual(id, value) => getString(IdPath.parse(id, path)).map(_.contains(value))
            case IsValid(id) =>
                ZPure.serviceWith[FormValidationState](_.isValid(IdPath.parse(id, path)))
            case NonEmpty(id) =>
                getString(IdPath.parse(id, path)).map(_.nonEmpty)
        end match
    end resolveCondition
end UIFormBuilder
