package works.iterative.autocomplete.ui.laminar

import works.iterative.ui.components.laminar.forms.*
import zio.prelude.Validation

object AutocompleteFieldBuilder:
    given autocompleteInput[A: AutocompleteHandler](using
        AutocompleteViews
    ): FieldBuilder[A] =
        val handler = summon[AutocompleteHandler[A]]
        new FieldBuilder[A]:
            override def required: Boolean = true
            override def build(
                desc: FieldDescriptor,
                initialValue: Option[A]
            ): FormComponent[A] =
                AutocompleteInput[A](
                    desc,
                    initialValue.map(handler.encode),
                    handler,
                    Validations.requiredA(desc.label)(_).flatMap(handler.decode)
                )
        end new
    end autocompleteInput

    given optionalAutocompleteInput[A: AutocompleteHandler, B](using ev: B <:< Option[A])(using
        AutocompleteViews
    ): FieldBuilder[Option[A]] =
        val handler = summon[AutocompleteHandler[A]]
        new FieldBuilder[Option[A]]:
            override def required: Boolean = false
            override def build(
                desc: FieldDescriptor,
                initialValue: Option[Option[A]]
            ): FormComponent[Option[A]] =
                AutocompleteInput[Option[A]](
                    desc,
                    initialValue.flatten.map(handler.encode),
                    handler,
                    {
                        case Some(s) if s.trim.nonEmpty => handler.decode(s).map(Some(_))
                        case _                          => Validation.succeed(None)
                    }
                )
        end new
    end optionalAutocompleteInput
end AutocompleteFieldBuilder
