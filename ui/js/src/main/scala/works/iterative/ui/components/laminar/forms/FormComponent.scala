package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L.*
import works.iterative.core.Validated
import zio.prelude.*

trait FormComponent[A]:
  def validated: Signal[Validated[A]]
  def elements: Seq[HtmlElement]

object FormComponent:
  def apply[A](
      v: Signal[Validated[A]],
      e: HtmlElement
  ): FormComponent[A] = apply(v, Seq(e))

  def apply[A](
      v: Signal[Validated[A]],
      e: Seq[HtmlElement]
  ): FormComponent[A] =
    new FormComponent:
      override def validated = v
      override def elements = e

  def empty: FormComponent[EmptyTuple.type] = apply(
    Val(Validation.succeed(Tuple())),
    Nil
  )

  extension [A](fc: FormComponent[A])
    def wrap(wrapper: Seq[HtmlElement] => HtmlElement): FormComponent[A] =
      FormComponent(fc.validated, Seq(wrapper(fc.elements)))
    def map[B](f: A => B): FormComponent[B] =
      FormComponent(fc.validated.map(_.map(f)), fc.elements)
    def zip[B <: Tuple](other: FormComponent[B]): FormComponent[A *: B] =
      FormComponent(
        Signal
          .combineWithFn(fc.validated, other.validated)(Validation.validate)
          .map(_.map(_ *: _)),
        fc.elements ++ other.elements
      )
