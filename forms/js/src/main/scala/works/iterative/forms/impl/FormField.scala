package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import works.iterative.core.UserMessage
import works.iterative.ui.model.forms.IdPath

// TODO: generalize over the Monad types
type FieldValue = Any
type FieldErrors = Seq[UserMessage]

type NamedValue = (IdPath, Seq[FieldValue])
type NamedValues = Seq[NamedValue]

type ErrorValue = (IdPath, FieldErrors)
type ErrorValues = Seq[ErrorValue]

type OutputValue = HtmlElement
type OutputValues = Seq[HtmlElement]

type ValidatedValue = (IdPath, ValidationState[Any])
type ValidatedValues = Seq[(IdPath, ValidationState[Any])]
