package works.iterative.core

import zio.prelude.Validation

type Validated[A] = Validation[UserMessage, A]
