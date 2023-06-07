package works.iterative.core.service.impl

import org.scalajs.dom.Storage
import works.iterative.core.service.Repository
import zio.*
import zio.json.*

// TODO: improve error reporting on generic repositories
// This is good just for prototypes
class JsStorageRepository[Value: JsonCodec](storage: Storage)
    extends Repository[String, Value]:

  override def find(id: String): UIO[Option[Value]] = {
    for
      raw <- ZIO.attemptBlocking(Option(storage.getItem(id)))
      data <- ZIO.foreach(raw) { r =>
        ZIO
          .fromEither(r.fromJson[Value])
          .mapError(_ => new RuntimeException(s"Failed to parse: ${raw}"))
      }
    yield data
  }.orDie

  override def save(key: String, value: Value): UIO[Unit] =
    ZIO.attemptBlocking(storage.setItem(key, value.toJson)).orDie
