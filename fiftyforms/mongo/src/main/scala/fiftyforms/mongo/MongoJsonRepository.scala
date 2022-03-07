package fiftyforms.mongo

import zio.*
import zio.json.*
import zio.config.*
import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.*
import org.bson.json.JsonObject
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.bson.conversions.Bson

case class MongoConfig(uri: String)

object MongoConfig:
  val configDesc =
    import ConfigDescriptor.*
    nested("MONGO")(string("URI").default("mongodb://localhost:27017"))
      .to[MongoConfig]
  val fromEnv = ZConfig.fromSystemEnv(configDesc)

val client: RLayer[MongoConfig, MongoClient] =
  (for
    config <- ZIO.service[MongoConfig]
    client <- Task.attempt(MongoClient(config.uri))
  yield client).toLayer

class MongoJsonRepository[Elem, Key, Criteria](
    collection: MongoCollection[JsonObject],
    toFilter: Criteria => Bson,
    idFilter: Elem => (String, Key)
)(using JsonCodec[Elem]) {
  def matching(criteria: Criteria): Task[Seq[Elem]] =
    val filter = toFilter(criteria)
    val query = collection.find(filter)

    for
      result <- ZIO.fromFuture(_ => query.toFuture)
      proof <- ZIO.collect(result)(j =>
        ZIO.fromOption(j.getJson.fromJson[Elem].toOption)
      )
    yield proof

  def put(elem: Elem): Task[Unit] =
    Task.async(cb =>
      collection
        .replaceOne(
          equal.tupled(idFilter(elem)),
          JsonObject(elem.toJson),
          ReplaceOptions().upsert(true)
        )
        .subscribe(_ => cb(Task.unit), t => cb(Task.fail(t)))
    )
}
