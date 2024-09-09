package portaly.forms.repository.impl

import zio.*
import works.iterative.core.service.impl.MongoFileConfig

final case class MongoConfig(
    db: String,
    prilohy: String
):
    def toFileConfig: MongoFileConfig =
        MongoFileConfig(db, prilohy)
end MongoConfig

object MongoConfig:
    given config: Config[MongoConfig] =
        import Config.*
        (
            string("db")
                .withDefault("test_portaly")
                ++ string("prilohy").withDefault("prilohy")
        ).nested("mongo").map(MongoConfig.apply)
    end config
end MongoConfig
