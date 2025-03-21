package works.iterative.mongo

import zio.*
import zio.json.*
import org.mongodb.scala.*
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.gridfs.GridFSBucket
import java.nio.ByteBuffer
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import java.time.Instant
import org.bson.types.ObjectId
import zio.stream.ZStream
import works.iterative.core.FileSupport
import scala.jdk.CollectionConverters.*

case class MongoFile(
    id: String,
    name: String,
    created: Instant,
    metadata: Map[String, String]
)

class MongoJsonFileRepository[Metadata: JsonCodec, Criteria](
    bucket: GridFSBucket,
    toFilter: Criteria => Bson
):
    def put(name: String, file: Array[Byte], metadata: Metadata): UIO[String] =
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
            .map(_.toString())
            .orDie

    def put(name: String, file: FileSupport.FileRepr, metadata: Metadata): UIO[String] =
        put(name, ZStream.fromFile(file), metadata)

    def put(name: String, file: ZStream[Any, Throwable, Byte], metadata: Metadata): UIO[String] =
        import zio.interop.reactivestreams.*
        file.chunks.map(v =>
            java.nio.ByteBuffer.wrap(v.toArray)
        ).toPublisher.flatMap(publisher =>
            ZIO.fromFuture(_ =>
                bucket.uploadFromObservable(
                    name,
                    BoxedPublisher(publisher),
                    GridFSUploadOptions().metadata(
                        Document(metadata.toJson)
                    ).chunkSizeBytes(1048576)
                ).toFuture
            )
        ).map(_.toString()).orDie
    end put

    def remove(id: String): UIO[Unit] =
        ZIO
            .fromFuture(_ => bucket.delete(ObjectId(id)).toFuture)
            .unit
            .orDie

    def find(id: String): UIO[Option[Array[Byte]]] =
        ZIO
            .fromFuture(_ => bucket.downloadToObservable(ObjectId(id)).toFuture)
            .map(r => if r.isEmpty then None else Some(r.map(_.array).reduce(_ ++ _)))
            .orDie

    def findStream(id: String): UIO[ZStream[Any, Throwable, Byte]] =
        import zio.interop.reactivestreams.*
        ZIO.succeed(bucket.downloadToObservable(ObjectId(id)).toZIOStream().map(v =>
            Chunk.fromArray(v.array)
        ).flattenChunks)
    end findStream

    def matching(criteria: Criteria): UIO[List[MongoFile]] =
        ZIO
            .fromFuture(_ => bucket.find(toFilter(criteria)).toFuture)
            .map(
                _.map(f =>
                    MongoFile(
                        f.getObjectId.toString,
                        f.getFilename,
                        f.getUploadDate.toInstant,
                        f.getMetadata.entrySet().asScala.map(e =>
                            e.getKey -> e.getValue.toString
                        ).toMap
                    )
                ).to(List)
            )
            .orDie

end MongoJsonFileRepository
