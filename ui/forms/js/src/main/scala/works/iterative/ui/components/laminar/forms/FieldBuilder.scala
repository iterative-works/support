package works.iterative.ui.components.laminar.forms

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import org.scalajs.dom.FileList
import works.iterative.core.{FileSupport, UserMessage, Validated}
import zio.prelude.Validation
import scala.util.NotGiven

case class Choice[A](
    options: Signal[List[A]],
    id: A => String,
    label: A => String,
    combo: Boolean = false,
    add: Option[String => Validated[A]] = None,
    query: Option[String => EventStream[List[A]]] = None
):
    def optional: Choice[Option[A]] =
        Choice[Option[A]](
            options = options.map(_.map(Some(_))),
            id = _.map(id).getOrElse(""),
            label = _.map(label).getOrElse(""),
            combo = combo,
            add = add.map(_.andThen(_.map(Some(_))))
        )
end Choice

object Choice:
    def apply[A](options: List[A], id: A => String, label: A => String): Choice[A] =
        Choice(
            options = Val(options),
            id = id,
            label = label
        )
end Choice

trait FieldBuilder[A]:
    def required: Boolean
    def build(
        fieldDescriptor: FieldDescriptor,
        initialValue: Option[A]
    ): FormComponent[A]
end FieldBuilder

object FieldBuilder:

    // TODO: use validation codec with A => raw string and raw string => Validted[A]
    def requiredInput[A: InputSchema](using
        fctx: FormBuilderContext
    ): FieldBuilder[A] =
        new FieldBuilder[A]:
            override def required: Boolean = true
            override def build(
                fieldDescriptor: FieldDescriptor,
                initialValue: Option[A]
            ): FormComponent[A] =
                val codec = summon[InputSchema[A]]
                Input(
                    fieldDescriptor,
                    initialValue.map(codec.encode),
                    Validations.required(fieldDescriptor.label)(_).flatMap(codec.decode),
                    codec.inputType
                )
            end build

    def optionalInput[A: InputSchema](using
        fctx: FormBuilderContext
    ): FieldBuilder[Option[A]] =
        new FieldBuilder[Option[A]]:
            override def required: Boolean = false
            override def build(
                fieldDescriptor: FieldDescriptor,
                initialValue: Option[Option[A]]
            ): FormComponent[Option[A]] =
                val codec = summon[InputSchema[A]]
                Input[Option[A]](
                    fieldDescriptor,
                    initialValue.flatten.map(codec.encode),
                    (v: Option[String]) =>
                        v match
                            case Some(s) if s.trim.nonEmpty => codec.decode(s).map(Some(_))
                            case _                          => Validation.succeed(None)
                    ,
                    codec.inputType
                )
            end build

    given [A: InputSchema](using
        fctx: FormBuilderContext,
        ev: NotGiven[A <:< Option[?]]
    ): FieldBuilder[A] = requiredInput[A]

    given [A, B: InputSchema](using
        fctx: FormBuilderContext,
        ev: A <:< Option[B]
    ): FieldBuilder[Option[B]] = optionalInput[B]

    given optionalFileInput(using
        FormBuilderContext
    ): FieldBuilder[Option[FileSupport.FileRepr]] =
        new FieldBuilder[Option[FileSupport.FileRepr]]:
            override def required: Boolean = false
            override def build(
                fieldDescriptor: FieldDescriptor,
                initialValue: Option[Option[FileSupport.FileRepr]]
            ): FormComponent[Option[FileSupport.FileRepr]] =
                FileField(
                    fieldDescriptor,
                    _ match
                        case Some(files) => Validation.succeed(files.headOption)
                        case None        => Validation.succeed(None)
                    ,
                    multi = false
                )

    given fileInput(using
        FormBuilderContext
    ): FieldBuilder[FileSupport.FileRepr] =
        new FieldBuilder[FileSupport.FileRepr]:
            override def required: Boolean = true
            override def build(
                fieldDescriptor: FieldDescriptor,
                initialValue: Option[FileSupport.FileRepr]
            ): FormComponent[FileSupport.FileRepr] =
                FileField(
                    fieldDescriptor,
                    _.flatMap(_.headOption) match
                        case Some(file) => Validation.succeed(file)
                        case None =>
                            Validation.fail(
                                UserMessage("error.file.required", fieldDescriptor.label)
                            )
                    ,
                    multi = false
                )

    // TODO: Embed file type into the FileRepr somehow, so that it can be determined automatically
    def restrictedFileInput(accepts: String)(using
        FormBuilderContext
    ): FieldBuilder[FileSupport.FileRepr] =
        new FieldBuilder[FileSupport.FileRepr]:
            override def required: Boolean = true
            override def build(
                fieldDescriptor: FieldDescriptor,
                initialValue: Option[FileSupport.FileRepr]
            ): FormComponent[FileSupport.FileRepr] =
                FileField(
                    fieldDescriptor,
                    _.flatMap(_.headOption) match
                        case Some(file) => Validation.succeed(file)
                        case None =>
                            Validation.fail(
                                UserMessage("error.file.required", fieldDescriptor.label)
                            )
                    ,
                    multi = false,
                    accepts = Some(accepts)
                )

    given filesInput(using
        FormBuilderContext
    ): FieldBuilder[List[FileSupport.FileRepr]] =
        new FieldBuilder[List[FileSupport.FileRepr]]:
            override def required: Boolean = true
            override def build(
                fieldDescriptor: FieldDescriptor,
                initialValue: Option[List[FileSupport.FileRepr]]
            ): FormComponent[List[FileSupport.FileRepr]] =
                FileField(
                    fieldDescriptor,
                    _.map(_.toList) match
                        case Some(files) => Validation.succeed(files)
                        case None =>
                            Validation.fail(
                                UserMessage("error.file.required", fieldDescriptor.label)
                            )
                    ,
                    multi = true
                )

    given optionalFilesInput(using
        FormBuilderContext
    ): FieldBuilder[Option[List[FileSupport.FileRepr]]] =
        new FieldBuilder[Option[List[FileSupport.FileRepr]]]:
            override def required: Boolean = false
            override def build(
                fieldDescriptor: FieldDescriptor,
                initialValue: Option[Option[List[FileSupport.FileRepr]]]
            ): FormComponent[Option[List[FileSupport.FileRepr]]] =
                FileField(
                    fieldDescriptor,
                    _.map(_.toList) match
                        case Some(files) => Validation.succeed(Some(files))
                        case None        => Validation.succeed(None)
                    ,
                    multi = true
                )

    given choiceInput[A](using Choice[A], FormBuilderContext): FieldBuilder[A] =
        new FieldBuilder[A]:
            override def required: Boolean = true
            override def build(
                fieldDescriptor: FieldDescriptor,
                initialValue: Option[A]
            ): FormComponent[A] =
                ChoiceField(
                    fieldDescriptor,
                    initialValue,
                    Validations.requiredA(fieldDescriptor.label)(_)
                )

    given optionalChoiceInput[A, B](using Choice[A], FormBuilderContext)(using
        ev: B <:< Option[A]
    ): FieldBuilder[Option[A]] =
        new FieldBuilder[Option[A]]:
            override def required: Boolean = false
            override def build(
                fieldDescriptor: FieldDescriptor,
                initialValue: Option[Option[A]]
            ): FormComponent[Option[A]] =
                val choice = summon[Choice[A]]
                ChoiceField(
                    fieldDescriptor,
                    initialValue,
                    a => Validation.succeed(a.flatten)
                )(using choice.optional)
            end build

    class Input[A](
        desc: FieldDescriptor,
        initialValue: Option[String] = None,
        validation: Option[String] => Validated[A],
        inputType: InputSchema.InputType
    )(using fctx: FormBuilderContext)
        extends FormComponent[A]:
        private val rawValue: Var[Option[String]] = Var(initialValue)

        override val validated: Signal[Validated[A]] =
            rawValue.signal.map(validation)

        override val elements: Seq[HtmlElement] =
            InputField(
                desc,
                initialValue,
                validated,
                rawValue.writer,
                inputType
            ).elements
    end Input

    class FileField[A](
        desc: FieldDescriptor,
        validation: Option[FileList] => Validated[A],
        multi: Boolean = false,
        accepts: Option[String] = None
    )(using fctx: FormBuilderContext)
        extends FormComponent[A]:
        private val rawValue: Var[Option[FileList]] = Var(None)

        override val validated: Signal[Validated[A]] =
            rawValue.signal.map(validation)

        override val elements: Seq[HtmlElement] =
            renderFileInputField(desc, rawValue.writer.contramapSome, multi, accepts)
    end FileField

    class ChoiceField[A](
        desc: FieldDescriptor,
        initialValue: Option[A],
        validation: Option[A] => Validated[A]
    )(using
        choice: Choice[A]
    )(using fctx: FormBuilderContext)
        extends FormComponent[A]:
        private val Choice(options, id, label, combo, add, query) = choice

        private val rawValue: Var[Option[A]] = Var(initialValue)

        override val validated: Signal[Validated[A]] =
            rawValue.signal.map(validation)

        override val elements: Seq[HtmlElement] =
            SelectField(
                desc,
                id,
                label,
                initialValue,
                options,
                validated,
                rawValue.writer,
                combo,
                add,
                query
            ).elements
    end ChoiceField

    def renderFileInputField(
        desc: FieldDescriptor,
        observer: Observer[FileList],
        multi: Boolean = false,
        accepts: Option[String] = None
    )(using
        fctx: FormBuilderContext
    ): Seq[HtmlElement] =
        Seq(
            div(
                fctx.formUIFactory
                    .fileInput(desc.placeholder.getOrElse(desc.label))()(
                        multiple(multi),
                        accepts.map(accept(_)),
                        nameAttr(desc.name),
                        idAttr(desc.idString),
                        inContext(thisNode =>
                            onInput.mapTo(thisNode.ref.files) --> observer
                        )
                    )
            )
        )
end FieldBuilder
