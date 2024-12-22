package portaly.forms

import works.iterative.core.FileRef
import works.iterative.core.ValidatedStringFactory
import works.iterative.core.Validated
import scala.reflect.ClassTag
import works.iterative.ui.model.forms.AbsolutePath
import works.iterative.core.UserMessage
import zio.prelude.Validation

opaque type FormKey = String

object FormKey extends ValidatedStringFactory[FormKey](identity):
    def apply(key: String): Validated[FormKey] = Validated.nonEmptyString("form_key")(key)
    def apply(key: AbsolutePath): FormKey = key.serialize

    extension (key: FormKey)
        def asPath: AbsolutePath = works.iterative.ui.model.forms.IdPath.parse(key)
        def +(suffix: String | FormKey): FormKey = key + "." + suffix
    end extension

    given Conversion[AbsolutePath, FormKey] = FormKey(_)
    given Conversion[String, FormKey] = FormKey.unsafe(_)
end FormKey

// This type represents the contents of the form for encoding / saving.
sealed trait FormValue
object FormValue:
    final case class StringValue(value: String) extends FormValue
    final case class FileValue(value: FileRef) extends FormValue

final case class TypedItem(key: FormKey, id: String, tpe: String, content: FormContent):
    val prefix: FormKey = key + s"$id.$tpe"
    private def nested(k: FormKey) = prefix + k
    def first[T <: FormValue: ClassTag](key: FormKey): Option[T] = content.first[T](nested(key))
    def getAs[T <: FormValue: ClassTag](key: FormKey): Seq[T] = content.getAs[T](nested(key))
    def firstString(key: FormKey): Option[String] = first[FormValue.StringValue](key).map(_.value)
    def firstStringOrElse(key: FormKey, default: => String): String =
        first[FormValue.StringValue](key).map(_.value).getOrElse(default)
    def firstFile(key: FormKey): Option[FileRef] =
        first[FormValue.FileValue](key).map(_.value)
    def get(key: FormKey): Seq[FormValue] = content.get(nested(key))
    def get(key: AbsolutePath): Seq[FormValue] = content.get(nested(FormKey(key)))
    def items(key: FormKey): Seq[TypedItem] = content.items(nested(key))
    def stringItems(key: FormKey, tpe: String): Seq[String] =
        items(key).filter(_.tpe == tpe).flatMap(it => it.content.firstString(it.prefix))
end TypedItem

object TypedItem:
    def parse(content: FormContent, key: FormKey)(typedItem: String): Validated[TypedItem] =
        val parts = typedItem.split(":", 2)
        parts match
            case Array(id, tpe) => Validation.succeed(TypedItem(key, id, tpe, content))
            case _              => Validation.fail(UserMessage("error.invalid.format"))
    end parse
end TypedItem

opaque type FormContent = Map[FormKey, Seq[FormValue]]

object FormContent:
    def apply(
        formId: String,
        formVersion: String,
        content: Map[FormKey, Seq[FormValue]]
    ): FormContent =
        content
            .updated(formIdKey, Seq(FormValue.StringValue(formId)))
            .updated(formVersionKey, Seq(FormValue.StringValue(formVersion)))

    def unsafe(content: Map[FormKey, Seq[FormValue]]): FormContent = content

    extension (content: FormContent)
        def first[T <: FormValue: ClassTag](key: FormKey): Option[T] =
            content.get(key)
                .flatMap:
                    _.headOption
                .collect:
                    case v: T => v

        def getAs[T <: FormValue: ClassTag](key: FormKey): Seq[T] =
            content.get(key)
                .map:
                    _.collect:
                        case v: T => v
                .getOrElse(Nil)

        def firstString(key: FormKey): Option[String] =
            first[FormValue.StringValue](key).map(_.value)

        def firstStringOrElse(key: FormKey, default: => String): String =
            first[FormValue.StringValue](key).map(_.value).getOrElse(default)

        def firstFile(key: FormKey): Option[FileRef] =
            first[FormValue.FileValue](key).map(_.value)

        def get(key: FormKey): Seq[FormValue] = content.getOrElse(key, Seq.empty)
        def get(key: AbsolutePath): Seq[FormValue] = content.getOrElse(FormKey(key), Seq.empty)

        def items(key: FormKey): Seq[TypedItem] =
            content.get(s"${key}.__items").map: items =>
                items.collect:
                    case FormValue.StringValue(v) => TypedItem.parse(content, key)(v).toOption
                .flatten
            .getOrElse(Nil)

        def stringItems(key: FormKey, tpe: String): Seq[String] =
            items(key).filter(tpe == _.tpe).flatMap(it => it.content.firstString(it.prefix))

        def value: Map[FormKey, Seq[FormValue]] = content

        def formId: Option[String] = content.first[FormValue.StringValue](formIdKey).map(_.value)
        def formVersion: Option[String] =
            content.first[FormValue.StringValue](formVersionKey).map(_.value)
        def toStringMap: Map[String, Seq[String]] =
            content.map: (k, v) =>
                k -> v.map:
                    case FormValue.StringValue(v) => v
                    case FormValue.FileValue(v)   => v.name
    end extension

    val formIdKey: FormKey = "___form_id"
    val formVersionKey: FormKey = "___form_version"

    val empty = FormContent.unsafe(Map.empty)
end FormContent
