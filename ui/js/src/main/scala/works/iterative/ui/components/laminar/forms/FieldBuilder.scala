package works.iterative.ui.components.laminar.forms

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.FileList
import works.iterative.core.{FileSupport, UserMessage, Validated}
import zio.prelude.Validation

import scala.util.NotGiven

case class Choice[A](
    options: List[A],
    id: A => String,
    label: A => String,
    combo: Boolean = false,
    add: Option[String => Validated[A]] = None
):
  def optional: Choice[Option[A]] =
    Choice[Option[A]](
      options = None :: options.map(Some(_)),
      id = _.map(id).getOrElse(""),
      label = _.map(label).getOrElse(""),
      combo = combo,
      add = add.map(_.andThen(_.map(Some(_))))
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
          },
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
          _.flatMap(_.headOption) match {
            case Some(file) => Validation.succeed(file)
            case None =>
              Validation.fail(
                UserMessage("error.file.required", fieldDescriptor.label)
              )
          },
          multi = false
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
          _.map(_.toList) match {
            case Some(files) => Validation.succeed(files)
            case None =>
              Validation.fail(
                UserMessage("error.file.required", fieldDescriptor.label)
              )
          },
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
      validation: Option[FileList] => Validated[A],
      multi: Boolean = false
  )(using fctx: FormBuilderContext)
      extends FormComponent[A]:
    private val rawValue: Var[Option[FileList]] = Var(None)

    override val validated: Signal[Validated[A]] =
      rawValue.signal.map(validation)

    override val elements: Seq[HtmlElement] =
      renderFileInputField(desc, rawValue.writer.contramapSome, multi)

  class ChoiceField[A](
      desc: FieldDescriptor,
      initialValue: Option[A],
      validation: Option[A] => Validated[A]
  )(using
      choice: Choice[A]
  )(using fctx: FormBuilderContext)
      extends FormComponent[A]:
    private val Choice(options, id, label, combo, add) = choice

    private def findValue(i: String): Option[A] = options.find(id(_) == i)

    private val rawValue: Var[Option[String]] = Var(
      initialValue.map(id(_))
    )

    private def selectValue(id: Option[String]): Validated[Option[A]] =
      def constructValue(id: String): Validated[Option[A]] =
        add.fold(Validation.succeed(None))(_(id).map(Some(_)))

      def findOrConstructValue(id: String): Validated[Option[A]] =
        findValue(id)
          .map(v => Validation.succeed(Some(v)))
          .getOrElse(constructValue(id))

      id.fold(Validation.succeed(None))(findOrConstructValue)

    override val validated: Signal[Validated[A]] =
      rawValue.signal
        .map(selectValue)
        .map(_.flatMap(validation))

    override val elements: Seq[HtmlElement] =
      val addValue: Option[String => (String, String)] = add.map(_ => {
        val msg = fctx.formMessagesResolver.message(
          UserMessage(s"add.${desc.idString}")
        )
        v => (v, s"$msg: $v")
      })

      SelectField(
        desc,
        initialValue.map(i => (id(i), label(i))),
        options.map(o => (id(o), label(o))),
        validated,
        rawValue.writer.contramapSome,
        combo,
        addValue
      ).elements

  def renderFileInputField(
      desc: FieldDescriptor,
      observer: Observer[FileList],
      multi: Boolean = false
  )(using
      fctx: FormBuilderContext
  ): Seq[HtmlElement] =
    Seq(
      div(
        fctx.formUIFactory
          .fileInput(desc.placeholder.getOrElse(desc.label))()(
            multiple(multi),
            nameAttr(desc.name),
            idAttr(desc.idString),
            inContext(thisNode =>
              onInput.mapTo(thisNode.ref.files) --> observer
            )
          )
      )
    )
