package works.iterative.ui.components.laminar.forms

import works.iterative.core.*

sealed trait FormSchema[A]:
  def zip[B <: Tuple](that: FormSchema[B]): FormSchema[A *: B] =
    FormSchema.Zip(this, that)
  def map[B](f: A => B)(g: B => A): FormSchema[B] =
    FormSchema.BiMap(this, f, g)

object FormSchema:

  extension [A](f: FormSchema[A])
    def *:[B <: Tuple](that: FormSchema[B]): FormSchema[A *: B] = f.zip(that)

  case object Unit extends FormSchema[EmptyTuple]

  case class Control[A](
      name: String,
      required: Boolean,
      decode: A => String,
      validation: Option[String] => Validated[A],
      inputType: InputSchema.InputType
  ) extends FormSchema[A]

  object Control:
    def apply[A](name: String)(using ic: InputSchema[A]): Control[A] =
      Control(
        name,
        ic.required,
        ic.encode,
        ic.decodeOptional(name),
        ic.inputType
      )

  case class Section[A](name: String, content: FormSchema[A])
      extends FormSchema[A]

  case class Zip[A, B <: Tuple](left: FormSchema[A], right: FormSchema[B])
      extends FormSchema[A *: B]:
    def toLeft(value: A *: B): A = value.head
    def toRight(value: A *: B): B = value.tail

  case class BiMap[A, B](form: FormSchema[A], f: A => B, g: B => A)
      extends FormSchema[B]
