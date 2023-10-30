package works.iterative.ui.components.laminar.forms

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.FileList
import works.iterative.core.{FileSupport, UserMessage, Validated}
import zio.prelude.Validation

import scala.util.NotGiven

case class ChoiceOption[A](id: String, label: String, value: A)

case class Choice[A](
    options: List[ChoiceOption[A]],
    combo: Boolean = false
)

trait FieldBuilder[A]:
  def required: Boolean
  def build(
      fieldDescriptor: FieldDescriptor,
      initialValue: Option[A]
  ): FormComponent[A]

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

  given [A: InputSchema](using
      fctx: FormBuilderContext,
      ev: NotGiven[A <:< Option[_]]
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
          _ match {
            case Some(files) => Validation.succeed(files.headOption)
            case None        => Validation.succeed(None)
          }
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
          _.flatMap(_.headOption) match {
            case Some(file) => Validation.succeed(file)
            case None =>
              Validation.fail(
                UserMessage("error.file.required", fieldDescriptor.label)
              )
          }
        )

  given choiceInput[A](using Choice[A], FormBuilderContext): FieldBuilder[A] =
    new FieldBuilder[A]:
      override def required: Boolean = true
      override def build(
          fieldDescriptor: FieldDescriptor,
          initialValue: Option[A]
      ): FormComponent[A] =
        val choice = summon[Choice[A]]
        val options = choice.options
        val combo = choice.combo
        ChoiceField(
          fieldDescriptor,
          initialValue,
          Validations.requiredA(fieldDescriptor.label)(_),
          options,
          combo
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
        val options = choice.options
        val combo = choice.combo
        ChoiceField(
          fieldDescriptor,
          initialValue,
          a => Validation.succeed(a.flatten),
          ChoiceOption("", "", None) :: options.map(o =>
            o.copy(value = Some(o.value))
          ),
          combo
        )

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

  class FileField[A](
      desc: FieldDescriptor,
      validation: Option[FileList] => Validated[A]
  )(using fctx: FormBuilderContext)
      extends FormComponent[A]:
    private val rawValue: Var[Option[FileList]] = Var(None)

    override val validated: Signal[Validated[A]] =
      rawValue.signal.map(validation)

    override val elements: Seq[HtmlElement] =
      renderFileInputField(desc, rawValue.writer.contramapSome)

  class ChoiceField[A](
      desc: FieldDescriptor,
      initialValue: Option[A],
      validation: Option[A] => Validated[A],
      options: List[ChoiceOption[A]],
      combo: Boolean
  )(using fctx: FormBuilderContext)
      extends FormComponent[A]:
    private val rawValue: Var[Option[String]] = Var(
      initialValue.flatMap(i => options.find(_.value == i).map(_.id))
    )

    override val validated: Signal[Validated[A]] =
      rawValue.signal
        .map(_.flatMap(i => options.find(_.id == i).map(_.value)))
        .map(validation)

    override val elements: Seq[HtmlElement] =
      SelectField(
        desc,
        initialValue.flatMap(i =>
          options.find(_.value == i).map(o => (o.id, o.label))
        ),
        options.map(o => (o.id, o.label)),
        validated,
        rawValue.writer.contramapSome,
        combo
      ).elements

  def renderFileInputField(desc: FieldDescriptor, observer: Observer[FileList])(
      using fctx: FormBuilderContext
  ): Seq[HtmlElement] =
    Seq(
      div(
        fctx.formUIFactory
          .fileInput(desc.placeholder.getOrElse(desc.label))()(
            multiple(false),
            nameAttr(desc.name),
            idAttr(desc.idString),
            inContext(thisNode =>
              onInput.mapTo(thisNode.ref.files) --> observer
            )
          )
      )
    )
