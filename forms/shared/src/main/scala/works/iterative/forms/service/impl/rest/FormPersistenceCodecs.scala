package portaly
package forms
package service
package impl.rest

import zio.json.*
import works.iterative.tapir.CustomTapir.*
import works.iterative.tapir.codecs.FileCodecs.given
import works.iterative.ui.model.forms.{RelativePath, IdPath}

trait FormPersistenceCodecs extends FormPersistenceJsonCodecs with FormPersistenceTapirCodecs

trait FormPersistenceJsonCodecs:
    given JsonCodec[SavedForm] = DeriveJsonCodec.gen[SavedForm]

    given formValueDecoder: JsonDecoder[FormValue] =
        // Decode also plain Strings, not just the FormValue.StringValue, for legacy reasons
        DeriveJsonDecoder.gen[FormValue].orElse(JsonDecoder.string.map(FormValue.StringValue(_)))

    given formValueEncoder: JsonEncoder[FormValue] =
        DeriveJsonEncoder.gen[FormValue]

    given formValueCodec: JsonCodec[FormValue] = JsonCodec(formValueEncoder, formValueDecoder)

    given formKeyFieldEncoder: JsonFieldEncoder[FormKey] =
        JsonFieldEncoder.string.contramap(_.value)
    given formKeyFieldDecoder: JsonFieldDecoder[FormKey] =
        JsonFieldDecoder.string.mapOrFail(FormKey.apply(_).toEither.left.map(_.toString))

    given formContentEncoder: JsonEncoder[FormContent] =
        JsonEncoder[Map[FormKey, Seq[FormValue]]].contramap(_.value)
    given formContentDecoder: JsonDecoder[FormContent] =
        JsonDecoder[Map[FormKey, Seq[FormValue]]].map(FormContent.unsafe)

    given formContentCodec: JsonCodec[FormContent] =
        JsonCodec(formContentEncoder, formContentDecoder)

    given JsonCodec[RelativePath] = JsonCodec.string.transform(IdPath(_), _.serialize)

    given fieldTypeDecoder: JsonDecoder[FieldType] =
        val derived = DeriveJsonDecoder.gen[FieldType]
        derived.orElse(JsonDecoder.string.map(FieldType(_)))

    given fieldTypeEncoder: JsonEncoder[FieldType] = DeriveJsonEncoder.gen[FieldType]

    given JsonCodec[FieldType] = JsonCodec(fieldTypeEncoder, fieldTypeDecoder)

    given JsonCodec[Condition] = DeriveJsonCodec.gen[Condition]
    given JsonCodec[SectionSegment] = DeriveJsonCodec.gen[SectionSegment]
    given JsonCodec[Form] = DeriveJsonCodec.gen[Form]
end FormPersistenceJsonCodecs

trait FormPersistenceTapirCodecs:
    given Schema[SavedForm] = Schema.derived[SavedForm]
    given Schema[FormValue] = Schema.derived[FormValue]
    given Schema[FormContent] =
        Schema.schemaForMap[FormKey, Seq[FormValue]](_.value).as[FormContent]
    given Schema[RelativePath] = Schema.string[RelativePath]
    given Schema[(String, String)] = Schema.schemaForArray[String].as[(String, String)]
    given Schema[FieldType] = Schema.derived[FieldType]
    implicit def conditionSchema: Schema[Condition] = Schema.derived[Condition]
    implicit def sectionSegmentSchema: Schema[SectionSegment] = Schema.derived[SectionSegment]
    given Schema[Form] = Schema.derived[Form]
end FormPersistenceTapirCodecs

object FormPersistenceCodecs extends FormPersistenceCodecs
