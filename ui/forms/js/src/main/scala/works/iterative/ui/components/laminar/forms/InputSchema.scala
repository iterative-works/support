package works.iterative.ui.components.laminar.forms

import works.iterative.core.*
import zio.prelude.Validation
import scala.util.Try

trait InputSchema[A]:
    def encode(a: A): String
    def decode(s: String): Validated[A]
    def decodeOptional(label: String)(s: Option[String]): Validated[A] =
        Validation.fail(UserMessage("error.value.required", label))
    def required: Boolean = true
    def inputType: InputSchema.InputType = InputSchema.InputType.Input("text")
end InputSchema

object InputSchema:
    enum InputType:
        case Input(tpe: String)
        case Textarea

    def apply[A](
        encodeF: A => String,
        decodeF: String => Validated[A]
    ): InputSchema[A] =
        new InputSchema[A]:
            override def encode(a: A): String = encodeF(a)
            override def decode(s: String): Validated[A] = decodeF(s)

    private def fromValidatedString[A](
        factory: ValidatedStringFactory[A]
    ): InputSchema[A] = new InputSchema[A]:
        override def encode(a: A): String = factory.getter(a)
        override def decode(s: String): Validated[A] = factory(s)

    given withValidatedStringFactory[A](using
        factory: ValidatedStringFactory[A]
    ): InputSchema[A] = fromValidatedString[A](factory)

    given validatedStringToInputCodec[A]: Conversion[ValidatedStringFactory[A], InputSchema[A]] =
        fromValidatedString(_)

    given string: InputSchema[String] with
        override def encode(a: String): String = a
        override def decode(s: String): Validated[String] = Validation.succeed(s)

    given integer: InputSchema[Int] with
        override def encode(a: Int): String = a.toString
        override def decode(s: String): Validated[Int] =
            Validation.fromTry(Try(s.toInt)).mapError(e =>
                UserMessage("error.invalid.integer.format", s)
            )
        override def inputType: InputType = InputType.Input("number")
    end integer

    given plainOneLine: InputSchema[PlainOneLine] with
        override def encode(a: PlainOneLine): String = a.asString
        override def decode(s: String): Validated[PlainOneLine] =
            PlainOneLine(s)
    end plainOneLine

    given plainMultiLine: InputSchema[PlainMultiLine] with
        override def encode(a: PlainMultiLine): String = a.asString
        override def decode(s: String): Validated[PlainMultiLine] =
            PlainMultiLine(s)
        override def inputType: InputType = InputType.Textarea
    end plainMultiLine

    given email: InputSchema[Email] with
        def encode(a: Email): String = a.value
        def decode(s: String): Validated[Email] = Email(s)
        override def inputType: InputType = InputType.Input("email")
    end email

    given optionalInputCodec[A](using
        codec: InputSchema[A]
    ): InputSchema[Option[A]] with
        def encode(a: Option[A]): String = a.map(codec.encode).getOrElse("")
        def decode(s: String): Validated[Option[A]] =
            if s.isEmpty then Validation.succeed(None)
            else codec.decode(s).map(Some(_))
        override def decodeOptional(label: String)(
            s: Option[String]
        ): Validated[Option[A]] =
            Validation.succeed(None)
        override def required: Boolean = false
    end optionalInputCodec
end InputSchema
