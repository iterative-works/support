package works.iterative
package ui.components.tailwind

import zio.prelude.ZValidation
import works.iterative.core.UserMessage

package object form:
    type Validated[V] = ZValidation[UserMessage, InvalidValue, V]
