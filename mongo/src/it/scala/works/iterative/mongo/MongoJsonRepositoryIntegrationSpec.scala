package works.iterative.mongo

import zio.*
import zio.test.*
import zio.json.*
import Assertion.*

object MongoJsonRepositoryIntegrationSpec extends ZIOSpecDefault:
  case class Example(id: String, value: String)
  sealed trait ExampleCriteria
  case object All extends ExampleCriteria
  case class ById(id: String) extends ExampleCriteria

  given JsonCodec[Example] = DeriveJsonCodec.gen

  override def spec = suite("Mongo repository integration spec")(
    test("repo can put and read back")(
      for
        repo <- ZIO
          .service[MongoJsonRepository[Example, String, ExampleCriteria]]
        _ <- repo.put(Example("1", "test"))
        result <- repo.matching(ById("1"))
      yield assertTrue(result.head.value == "test")
    )
  ).provide(layer) @@ TestAspect.ifEnvSet("MONGO_URI")

  val layer: TaskLayer[MongoJsonRepository[Example, String, ExampleCriteria]] =
    import org.mongodb.scala.*
    import org.mongodb.scala.model.Filters.*
    import org.bson.json.JsonObject
    import org.mongodb.scala.bson.Document
    MongoConfig.fromEnv >>> MongoClient.layer >>> ZLayer {
      (for
        client <- ZIO.service[MongoClient]
        coll <- ZIO.attempt(
          client.getDatabase("test").getCollection[JsonObject]("example")
        )
      yield new MongoJsonRepository[Example, String, ExampleCriteria](
        coll, {
          _ match
            case ById(id) => equal("id", id)
            case All      => Document()
        },
        e => ("id", e.id)
      ))
    }
