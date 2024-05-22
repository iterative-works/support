package works.iterative.mongo

import zio.*
import zio.json.*
import zio.config.*
import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.*
import org.bson.json.JsonObject
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters

case class MongoConfig(uri: String)

object MongoConfig:
    val configDesc =
        import ConfigDescriptor.*
        nested("MONGO")(string("URI").default("mongodb://localhost:27017"))
            .to[MongoConfig]
    val fromEnv = ZConfig.fromSystemEnv(
        configDesc,
        keyDelimiter = Some('_'),
        valueDelimiter = None
    )
end MongoConfig

extension (m: MongoClient.type)
    def layer: RLayer[MongoConfig, MongoClient] =
        ZLayer {
            ZIO.serviceWithZIO[MongoConfig](c => ZIO.attempt(MongoClient(c.uri)))
        }

class MongoJsonRepository[Elem: JsonCodec, Key, Criteria](
    collection: MongoCollection[JsonObject],
    toFilter: Criteria => Bson,
    idFilter: Elem => (String, Key)
):
    def performCustomQuery[Target](
        query: FindObservable[JsonObject]
    )(using JsonDecoder[Target]): UIO[List[Target]] = {
        for
            result <- ZIO.fromFuture(_ => query.toFuture)
            decoded = result.map(r => r.getJson -> r.getJson.fromJson[Target])
            failed = decoded.collect { case (r, Left(msg)) =>
                s"Unable to decode json : $msg\nJson value:\n$r\n"
            }
            elems = decoded.collect { case (_, Right(e)) =>
                e
            }
            _ <- ZIO
                .logWarning(
                    s"Errors while reading json entries from MongoDB:\n${failed.mkString("\n")}"
                )
                .when(failed.nonEmpty)
        yield elems.to(List)
    }.orDie

    def performQuery(query: FindObservable[JsonObject]): UIO[List[Elem]] =
        performCustomQuery[Elem](query)

    def matching(criteria: Criteria): UIO[List[Elem]] =
        val filter = toFilter(criteria)
        val query = collection.find(filter)
        performQuery(query)
    end matching

    def put(elem: Elem): UIO[Unit] =
        ZIO.async(cb =>
            collection
                .replaceOne(
                    equal.tupled(idFilter(elem)),
                    JsonObject(elem.toJson),
                    ReplaceOptions().upsert(true)
                )
                .subscribe(_ => cb(ZIO.unit), t => cb(ZIO.die(t)))
        )
end MongoJsonRepository
