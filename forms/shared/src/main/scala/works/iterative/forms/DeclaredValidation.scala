// PURPOSE: Validates form data against the form declaration — required-ness and declared validations
// PURPOSE: Only visible fields validate; format checks skip blank values; Rule validations bind at the edges

package works.iterative.forms

import works.iterative.core.{MessageCatalogue, UserMessage}
import works.iterative.ui.model.forms.{AbsolutePath, FormState, IdPath}

object DeclaredValidation:

    def validate(form: Form, state: FormState)(using
        messages: MessageCatalogue
    ): MapFormValidationState =
        // Resolve labels under the form prefix exactly like the renderers do, so the
        // suffix fallback chain finds e.g. inquiry.row.qty.label for dynamic row paths
        given MessageCatalogue = messages.nested(form.id.serialize)
        MapFormValidationState(
            collect(IdPath.Root / form.id, form.elems, state).groupMap(_._1)(_._2)
        )
    end validate

    private def requiredError(path: AbsolutePath)(using MessageCatalogue) =
        path -> UserMessage("error.field.required", path.toMessage("label"))

    private def collect(path: AbsolutePath, elems: List[SectionSegment], state: FormState)(using
        MessageCatalogue
    ): List[(AbsolutePath, UserMessage)] =
        elems.flatMap(validateSegment(path, state))

    private def validateSegment(path: AbsolutePath, state: FormState)(using MessageCatalogue)(
        element: SectionSegment
    ): List[(AbsolutePath, UserMessage)] =
        element match
            case Section(id, elems, _) => collect(path / id, elems, state)
            case Field(id, fieldType, default, optional, validations) =>
                val fieldPath = path / id
                val effective = state.getString(fieldPath).orElse(default).filterNot(_.isBlank)
                val required = !optional || validations.contains(Validation.Required)
                if fieldType.hidden then Nil
                else
                    effective match
                        case None =>
                            if required then List(requiredError(fieldPath)) else Nil
                        case Some(value) =>
                            validations.flatMap(
                                Validation.check(_, value, fieldPath.toMessage("label"))
                                    .map(fieldPath -> _)
                            )
                end if
            case File(id, _, optional) =>
                if !optional && state.getFileList(path / id).forall(_.isEmpty) then
                    List(requiredError(path / id))
                else Nil
            case ShowIf(condition, elem) =>
                // Validation state cannot exist while being computed; visibility treats fields as valid
                if Condition.eval(condition, path, state.getString, Condition.alwaysValid) then
                    validateSegment(path, state)(elem)
                else Nil
            case repeated @ Repeated(id, _, optional, _) =>
                val instances = Repeated.instances(path, repeated, state)
                if instances.isEmpty then
                    if optional then Nil else List(requiredError(path / id))
                else instances.flatMap(i => validateSegment(i.path, state)(i.segment))
            case Enum(id, _, default, optional) =>
                val fieldPath = path / id
                val effective = state.getString(fieldPath).orElse(default).filterNot(_.isBlank)
                if !optional && effective.isEmpty then List(requiredError(fieldPath)) else Nil
            case Date(id, optional) =>
                val fieldPath = path / id
                if !optional && state.getString(fieldPath).forall(_.isBlank) then
                    List(requiredError(fieldPath))
                else Nil
            // Display and Button hold no data
            case _ => Nil

end DeclaredValidation
