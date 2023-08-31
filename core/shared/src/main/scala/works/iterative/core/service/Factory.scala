package works.iterative.core
package service

import zio.*

trait EntityFactory[Entity, Seed]:
  def make(seed: Seed): IO[UserMessage, Entity]

class GeneratingFactory[Entity, Seed, Id](
    constructor: Seed => Validated[Id => Entity],
    idGenerator: IdGenerator[Id]
) extends EntityFactory[Entity, Seed]:
  override def make(seed: Seed): IO[UserMessage, Entity] =
    for
      construct <- constructor(seed).toZIO
      id <- idGenerator.nextId
    yield construct(id)
