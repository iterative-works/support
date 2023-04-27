package works.iterative.ui.model

import works.iterative.core.UserMessage

/** A class representing the states of a model that needs computation
  */
// TODO: move to core when stable
enum Computable[Model]:
  case Uninitialized extends Computable[Nothing]
  case Computing extends Computable[Nothing]
  case Ready(model: Model) extends Computable[Model]
  case Failed(error: UserMessage) extends Computable[Nothing]
