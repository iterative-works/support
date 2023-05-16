package works.iterative.ui.components.laminar.forms

import zio.prelude.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.airstream.core.Signal
import works.iterative.core.Validated

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

  extension [A](fc: FormComponent[A])
    def wrap(wrapper: Seq[HtmlElement] => HtmlElement): FormComponent[A] =
      FormComponent(fc.validated, Seq(wrapper(fc.elements)))
    def map[B](f: A => B): FormComponent[B] =
      FormComponent(fc.validated.map(_.map(f)), fc.elements)

  given AssociativeBoth[FormComponent] with
    override def both[A, B](
        fa: => FormComponent[A],
        fb: => FormComponent[B]
    ): FormComponent[(A, B)] =
      FormComponent(
        Signal.combineWithFn(fa.validated, fb.validated)(Validation.validate),
        fa.elements ++ fb.elements
      )
