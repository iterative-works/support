package works.iterative.core
package service
package impl

import zio.*

final case class MongoFileConfig(
    db: String,
    collection: String
)

object MongoFileConfig:
    given config: Config[MongoFileConfig] =
        import Config.*
        (string("db") ++ string("fscoll").withDefault("files")).nested("mongo").map(
            MongoFileConfig.apply
        )
    end config
end MongoFileConfig
