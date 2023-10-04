package works.iterative.ui.services

import zio.*
import works.iterative.core.*

trait ClientEntityService[Id, Entity, Command]:
  def load(id: Id): IO[UserMessage, Entity]
  def update(id: Id, command: Command): IO[UserMessage, Option[UserMessage]]
