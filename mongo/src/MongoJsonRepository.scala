package works.iterative.mongo

import zio.*
import zio.json.*
import zio.config.*
import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.*
import org.bson.json.JsonObject
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.gridfs.GridFSBucket
import java.io.File
import java.nio.ByteBuffer
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import java.time.Instant
import org.bson.types.ObjectId

case class MongoConfig(uri: String)

object MongoConfig:
  val configDesc =
    import ConfigDescriptor.*
    nested("MONGO")(string("URI").default("mongodb://localhost:27017"))
      .to[MongoConfig]
  val fromEnv = ZConfig.fromSystemEnv(
    configDesc,
    keyDelimiter = Some('_'),
    valueDelimiter = Some(',')
  )

extension (m: MongoClient.type)
  def layer: RLayer[MongoConfig, MongoClient] =
    ZIO
      .serviceWithZIO[MongoConfig](c => Task.attempt(MongoClient(c.uri)))
      .toLayer

class MongoJsonRepository[Elem: JsonCodec, Key, Criteria](
    collection: MongoCollection[JsonObject],
    toFilter: Criteria => Bson,
    idFilter: Elem => (String, Key)
):
  def matching(criteria: Criteria): Task[List[Elem]] =
    val filter = toFilter(criteria)
    val query = collection.find(filter)

    for
      result <- ZIO.fromFuture(_ => query.toFuture)
      elems <- ZIO.collect(result)(j =>
        ZIO.fromOption(j.getJson.fromJson[Elem].toOption)
      )
    yield elems.to(List)

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

case class MongoFile(
  id: String,
  name: String,
  created: Instant
)

class MongoJsonFileRepository[Metadata: JsonCodec, Criteria](
    bucket: GridFSBucket,
    toFilter: Criteria => Bson
):

  def put(name: String, file: Array[Byte], metadata: Metadata): Task[Unit] =
    ZIO
      .fromFuture(_ =>
        bucket
          .uploadFromObservable(
            name,
            Observable(Seq(ByteBuffer.wrap(file))),
            GridFSUploadOptions().metadata(Document(metadata.toJson))
          )
          .toFuture
      )
      .unit

  def find(id: String): Task[Option[Array[Byte]]] =
    ZIO
      .fromFuture(_ => bucket.downloadToObservable(ObjectId(id)).toFuture)
      .map(_.headOption.map(_.array))

  def matching(criteria: Criteria): Task[List[MongoFile]] =
    ZIO
      .fromFuture(_ => bucket.find(toFilter(criteria)).toFuture)
      .map(_.map(f => MongoFile(f.getObjectId.toString, f.getFilename, f.getUploadDate.toInstant)).to(List))
