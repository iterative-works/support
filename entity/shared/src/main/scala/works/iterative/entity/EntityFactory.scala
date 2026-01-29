package works.iterative.entity

import works.iterative.core.Validated
import works.iterative.core.UserMessage
import works.iterative.core.service.IdGenerator
import zio.*

/** Generic class for entity factories.
  *
  * Creates a new entity from a "seed".
  */
trait EntityFactory[Entity, Seed]:
    def make(seed: Seed): IO[UserMessage, Entity]
end EntityFactory

/** A factory using idGenerator to create the entity */
class GeneratingFactory[Entity, Seed, Id](
    constructor: Seed => Validated[Id => Entity],
    idGenerator: IdGenerator[Id]
) extends EntityFactory[Entity, Seed]:
    override def make(seed: Seed): IO[UserMessage, Entity] =
        for
            construct <- constructor(seed).toZIO
            id <- idGenerator.nextId
        yield construct(id)
end GeneratingFactory
