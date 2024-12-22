package portaly.forms
package impl

import zio.prelude.*
import com.raquo.laminar.api.L.*
import works.iterative.core.UserMessage
import works.iterative.autocomplete.AutocompleteEntry
import works.iterative.autocomplete.ui.laminar.AutocompleteQuery
import works.iterative.ui.model.forms.{AbsolutePath, IdPath}

trait FieldFactory[A]:
    def render(
        id: IdPath,
        required: Boolean,
        initialValue: Option[A]
    ): Render[FieldFactory.FieldPart[A]]
end FieldFactory

object FieldFactory:
    type FieldPart[A] =
        FormPart[Any, String, Nothing, String, List[UserMessage], ValidationState[A]]
    def requiredString(id: IdPath): Render[SRule[String]] =
        ValidationRule.required[EventStream, Option, String](id)(
            UserMessage("error.field.required", id.toMessage("label"))
        ).contramap(Option(_).filterNot(_.isBlank))

    class Hidden(
        extraMods: HtmlMod*
    ) extends FieldFactory[String]:
        def render(
            id: IdPath,
            required: Boolean,
            initialValue: Option[String]
        ): Render[FieldPart[String]] =
            val field = TextFormField("hidden", initialValue, true, extraMods)
            fi => field(fi.mapError(_ => false)).map(v => ValidationState.Valid(v))
        end render
    end Hidden

    class Text(
        inputType: String,
        initialEnabled: Boolean,
        validation: IdPath => portaly.forms.impl.Validation,
        extraMods: HtmlMod*
    ) extends FieldFactory[String]:
        def render(
            id: IdPath,
            required: Boolean,
            initialValue: Option[String]
        ): Render[FieldPart[String]] =
            val field = TextFormField(inputType, initialValue, initialEnabled, extraMods)
            fi =>
                ValidatingFormField[Any, String, String, String, String](
                    if required then
                        requiredString(fi.id).flatMap(validation(fi.id))
                    else
                        v =>
                            if v.isBlank then ValidationState.Valid(v).succeed
                            else validation(fi.id)(v)
                )(
                    LabeledFormField(field, Val(required)),
                    field.touched
                )(fi)
        end render
    end Text

    class TextArea(
        inputType: String,
        validation: IdPath => portaly.forms.impl.Validation,
        extraMods: HtmlMod*
    ) extends FieldFactory[String]:
        def render(
            id: IdPath,
            required: Boolean,
            initialValue: Option[String]
        ): Render[FieldPart[String]] =
            val field = TextAreaFormField(inputType, initialValue, extraMods)
            fi =>
                ValidatingFormField[Any, String, String, String, String](
                    if required then
                        requiredString(fi.id).flatMap(validation(fi.id))
                    else
                        v =>
                            if v.isBlank then ValidationState.Valid(v).succeed
                            else validation(fi.id)(v)
                )(
                    LabeledFormField(field, Val(required)),
                    field.touched
                )(fi)
        end render
    end TextArea

    case class Autocomplete(
        query: AutocompleteQuery,
        contextSignal: FormCtx => AbsolutePath => Signal[Option[Map[String, String]]],
        selectObserver: Option[FormCtx ?=> Observer[(AbsolutePath, AutocompleteEntry)]] = None
    ) extends FieldFactory[String]:
        def render(
            id: IdPath,
            required: Boolean,
            initialValue: Option[String]
        ): Render[FieldPart[String]] =
            val observe: Observer[(AbsolutePath, Option[AutocompleteEntry])] = selectObserver match
                case Some(o) => o.contracollect[(AbsolutePath, Option[AutocompleteEntry])]:
                        case (id, Some(a)) => (id, a)
                case _ => Observer.empty
            fi =>
                val field = AutocompleteFormField(
                    initialValue,
                    query.withContextSignal(contextSignal(FormCtx.ctx)(fi.id))
                )
                val req = if required then requiredString(fi.id) else ValidationRule.valid
                ValidatingFormField(req)(
                    LabeledFormField(
                        field.tap(observe.setDisplayName(s"observe:${id.toHtmlId}")).map(
                            _.map(_.value).getOrElse("")
                        ),
                        Val(required)
                    ),
                    field.touched.setDisplayName(s"touched:${id.toHtmlId}")
                )(fi)
        end render
    end Autocomplete

    case class Select(
        validation: IdPath => portaly.forms.impl.Validation,
        getOptions: FormCtx => AbsolutePath => EventStream[List[AutocompleteEntry]],
        selectObserver: Option[FormCtx ?=> Observer[(AbsolutePath, AutocompleteEntry)]] = None,
        disabled: Boolean = false
    ) extends FieldFactory[String]:
        def render(
            id: IdPath,
            required: Boolean,
            initialValue: Option[String]
        ): Render[FieldFactory.FieldPart[String]] =
            fi =>
                val observe: Observer[(AbsolutePath, Option[AutocompleteEntry])] =
                    selectObserver match
                        case Some(o) => o.contracollect[(AbsolutePath, Option[AutocompleteEntry])]:
                                case (id, Some(a)) => (id, a)
                        case _ => Observer.empty
                val field =
                    AutocompleteSelectFormField(
                        initialValue,
                        getOptions(FormCtx.ctx)(fi.id).startWith(Nil),
                        disabled
                    )
                val req = if required then requiredString(fi.id) else ValidationRule.valid
                ValidatingFormField(req.flatMap(validation(fi.id)))(
                    LabeledFormField(
                        field.tap(observe.setDisplayName(s"observe:${id.toHtmlId}")).map(
                            _.map(_.value).getOrElse("")
                        ),
                        Val(required)
                    ),
                    field.touched
                )(fi)
    end Select
end FieldFactory
