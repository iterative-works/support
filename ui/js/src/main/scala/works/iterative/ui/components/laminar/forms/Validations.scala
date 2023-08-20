package works.iterative.ui.components.laminar.forms

import zio.prelude.*
import works.iterative.core.UserMessage
import works.iterative.core.Validated

object Validations:

  def required(label: String): Option[String] => Validated[String] =
    case Some(value) if value.trim.nonEmpty => Validation.succeed(value)
    case _ =>
      Validation.fail(UserMessage("error.value.required", label))

  def requiredA[A](label: String): Option[A] => Validated[A] =
    case Some(value) => Validation.succeed(value)
    case _ =>
      Validation.fail(UserMessage("error.value.required", label))
