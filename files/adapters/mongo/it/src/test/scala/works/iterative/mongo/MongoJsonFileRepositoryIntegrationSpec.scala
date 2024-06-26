package works.iterative.mongo

import zio.*
import zio.test.*
import zio.json.*
import Assertion.*

object MongoJsonFileRepositoryIntegrationSpec extends ZIOSpecDefault:
    case class ExampleMetadata(osobniCislo: String)
    sealed trait ExampleCriteria
    case class ByOsobniCislo(osobniCislo: String) extends ExampleCriteria

    given JsonCodec[ExampleMetadata] = DeriveJsonCodec.gen

    override def spec = suite("Mongo file repository integration spec")(
        test("repo can put and read back file")(
            for
                repo <- ZIO
                    .service[MongoJsonFileRepository[ExampleMetadata, ExampleCriteria]]
                fname = "Příliš žluťoučký kůň úpěl ďábelské ódy.txt"
                _ <- repo.put(
                    fname,
                    "Example content".getBytes(),
                    ExampleMetadata("10123")
                )
                result <- repo.matching(ByOsobniCislo("10123"))
            yield assertTrue(result.head.name == fname)
        )
    ).provide(layer) @@ TestAspect.ifEnvSet("MONGO_URI")

    val layer: TaskLayer[MongoJsonFileRepository[ExampleMetadata, ExampleCriteria]] =
        import org.mongodb.scala.*
        import org.mongodb.scala.model.Filters.*
        import org.mongodb.scala.gridfs.GridFSBucket
        MongoClient.layer >>> ZLayer(
            for
                client <- ZIO.service[MongoClient]
                bucket <- ZIO.attempt(
                    GridFSBucket(client.getDatabase("test"), "testfiles")
                )
            yield new MongoJsonFileRepository[ExampleMetadata, ExampleCriteria](
                bucket, {
                    _ match
                        case ByOsobniCislo(osc) => equal("metadata.osobniCislo", osc)
                }
            )
        )
    end layer
end MongoJsonFileRepositoryIntegrationSpec
