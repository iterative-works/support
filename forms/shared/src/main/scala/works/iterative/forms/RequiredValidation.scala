// PURPOSE: Computes Required-only validation of form data against the form declaration
// PURPOSE: Visible, non-optional fields with blank values are invalid; seed of the declared validation vocabulary

package portaly.forms

import works.iterative.core.{MessageCatalogue, UserMessage}
import works.iterative.ui.model.forms.{AbsolutePath, FormState, IdPath}

object RequiredValidation:

    def validate(form: Form, state: FormState)(using MessageCatalogue): FormValidationState =
        MapFormValidationState(
            collect(IdPath.Root / form.id, form.elems, state).groupMap(_._1)(_._2)
        )

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
            case Field(id, fieldType, default, optional) =>
                if fieldType.hidden || optional then Nil
                else if state.getString(path / id).orElse(default).forall(_.isBlank) then
                    List(requiredError(path / id))
                else Nil
            case File(id, _, optional) =>
                if !optional && state.getFileList(path / id).forall(_.isEmpty) then
                    List(requiredError(path / id))
                else Nil
            case ShowIf(condition, elem) =>
                if isSatisfied(path, condition, state) then validateSegment(path, state)(elem)
                else Nil
            case Repeated(id, _, optional, elems) =>
                val items = state.itemsFor(path / id)
                if items.isEmpty then
                    if optional then Nil else List(requiredError(path / id))
                else
                    val elemList = elems.map(e => e.id.last -> e)
                    val elemMap = elemList.toMap
                    val defaultSegment = elemList.head._2
                    items.flatMap((item, itemType) =>
                        validateSegment(path / id / item, state)(
                            elemMap.getOrElse(itemType, defaultSegment)
                        )
                    )
                end if
            // Date and Enum carry no optional flag, Display and Button hold no data
            case _ => Nil

    private def isSatisfied(path: AbsolutePath, condition: Condition, state: FormState): Boolean =
        import Condition.*
        condition match
            case Never              => false
            case Always             => true
            case AnyOf(conditions*) => conditions.exists(isSatisfied(path, _, state))
            case AllOf(conditions*) => conditions.forall(isSatisfied(path, _, state))
            case IsEqual(id, value) => state.getString(IdPath.parse(id, path)).contains(value)
            // Validation state cannot exist while being computed; visibility treats fields as valid
            case IsValid(_)   => true
            case NonEmpty(id) => state.getString(IdPath.parse(id, path)).nonEmpty
        end match
    end isSatisfied
end RequiredValidation
