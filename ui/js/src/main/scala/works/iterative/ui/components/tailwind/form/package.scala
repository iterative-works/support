package works.iterative
package ui.components.tailwind

import zio.prelude.Validation

package object form:
  type Validated[V] = Validation[InvalidValue, V]
