package works.iterative.autocomplete.ui.laminar

import works.iterative.ui.components.laminar.forms.*
import zio.prelude.Validation

class AutocompleteFieldBuilder(views: AutocompleteViews, registry: AutocompleteRegistry):
    private def valueOf[A](a: A)(using codec: AutocompleteCodec[A]): String =
        codec.encode(a) match
        case Left(v)  => v
        case Right(a) => a.value

    given autocompleteInput[A](using codec: AutocompleteCodec[A]): FieldBuilder[A] =
        new FieldBuilder[A]:
            override def required: Boolean = true
            override def build(
                desc: FieldDescriptor,
                initialValue: Option[A]
            ): FormComponent[A] =
                AutocompleteInput[A](
                    desc,
                    initialValue.map(valueOf),
                    registry.queryFor(desc.id),
                    Validations.requiredA(desc.label)(_).flatMap(codec.decode)
                )(using views)
        end new
    end autocompleteInput

    given optionalAutocompleteInput[A, B](using ev: B <:< Option[A])(using
        codec: AutocompleteCodec[A]
    ): FieldBuilder[Option[A]] =
        new FieldBuilder[Option[A]]:
            override def required: Boolean = false
            override def build(
                desc: FieldDescriptor,
                initialValue: Option[Option[A]]
            ): FormComponent[Option[A]] =
                AutocompleteInput[Option[A]](
                    desc,
                    initialValue.flatten.map(valueOf),
                    registry.queryFor(desc.id),
                    {
                        case Some(s) => codec.decode(s).map(Some(_))
                        case _       => Validation.succeed(None)
                    }
                )(using views)
        end new
    end optionalAutocompleteInput
end AutocompleteFieldBuilder
