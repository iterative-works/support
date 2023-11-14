package works.iterative.core
package service
package impl

import zio.*
import zio.config.*

final case class MongoFileConfig(
    db: String,
    collection: String
)

object MongoConfig:

  val configDescriptor: ConfigDescriptor[MongoFileConfig] =
    import ConfigDescriptor.*
    nested("MONGO")(
      string("DB")
        .zip(string("FSCOLL").default("files"))
    ).to[MongoFileConfig]

  val fromEnv: ZLayer[Any, ReadError[String], MongoFileConfig] =
    ZConfig.fromSystemEnv(configDescriptor, Some('_'), Some(','))
