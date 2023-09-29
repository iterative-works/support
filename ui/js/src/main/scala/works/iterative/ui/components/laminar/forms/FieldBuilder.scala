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
    options: List[ChoiceOption[A]]
)

trait FieldBuilder[A]:
  def required: Boolean
  def build(
      fieldDescriptor: FieldDescriptor,
      initialValue: Option[A]
  ): FormComponent[A]

object FieldBuilder:

  // TODO: use validation codec with A => raw string and raw string => Validted[A]
  def requiredInput[A: InputCodec](using
      fctx: FormBuilderContext
  ): FieldBuilder[A] =
    new FieldBuilder[A]:
      override def required: Boolean = true
      override def build(
          fieldDescriptor: FieldDescriptor,
          initialValue: Option[A]
      ): FormComponent[A] =
        val codec = summon[InputCodec[A]]
        Input(
          fieldDescriptor,
          initialValue.map(codec.encode),
          Validations.required(fieldDescriptor.label)(_).flatMap(codec.decode)
        )

  def optionalInput[A: InputCodec](using
      fctx: FormBuilderContext
  ): FieldBuilder[Option[A]] =
    new FieldBuilder[Option[A]]:
      override def required: Boolean = false
      override def build(
          fieldDescriptor: FieldDescriptor,
          initialValue: Option[Option[A]]
      ): FormComponent[Option[A]] =
        val codec = summon[InputCodec[A]]
        Input[Option[A]](
          fieldDescriptor,
          initialValue.flatten.map(codec.encode),
          (v: Option[String]) =>
            v match
              case Some(s) if s.trim.nonEmpty => codec.decode(s).map(Some(_))
              case _                          => Validation.succeed(None)
        )

  given [A: InputCodec](using
      fctx: FormBuilderContext,
      ev: NotGiven[A <:< Option[_]]
  ): FieldBuilder[A] = requiredInput[A]

  given [A, B: InputCodec](using
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
        val options = summon[Choice[A]].options
        ChoiceField(
          fieldDescriptor,
          Some(initialValue.getOrElse(options.head.value)),
          Validations.requiredA(fieldDescriptor.label)(_),
          options
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
        ChoiceField(
          fieldDescriptor,
          initialValue,
          a => Validation.succeed(a.flatten),
          ChoiceOption("", "", None) :: summon[Choice[A]].options.map(o =>
            o.copy(value = Some(o.value))
          )
        )

  class Input[A](
      desc: FieldDescriptor,
      initialValue: Option[String] = None,
      validation: Option[String] => Validated[A]
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
        rawValue.writer
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
      options: List[ChoiceOption[A]]
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
      renderSelect(
        desc,
        initialValue.flatMap(i => options.find(_.value == i).map(_.id)),
        options.map(o => (o.id, o.label)),
        rawValue.writer.contramapSome
      )

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

  def renderSelect(
      desc: FieldDescriptor,
      initialValue: Option[String],
      options: List[(String, String)],
      observer: Observer[String]
  )(using
      fctx: FormBuilderContext
  ): Seq[HtmlElement] =
    Seq(
      div(
        select(
          idAttr(desc.idString),
          nameAttr(desc.name),
          cls(
            "mt-2 block w-full sm:max-w-xs rounded-md border-0 py-1.5 pl-3 pr-10 text-gray-900 ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-indigo-600 sm:text-sm sm:leading-6"
          ),
          initialValue.map(L.value(_)),
          options.map(o =>
            option(selected(initialValue.contains(o._1)), value(o._1), o._2)
          ),
          onChange.mapToValue.setAsValue --> observer
        )
      )
    )
