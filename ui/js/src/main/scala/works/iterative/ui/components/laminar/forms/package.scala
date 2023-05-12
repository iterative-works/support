package works.iterative.ui.components.laminar

import zio.prelude.Validation
import works.iterative.core.UserMessage

package object forms:
  type FieldId = String
  type FieldLabel = String

  type Validated[A] = Validation[UserMessage, A]
