package mdr.pdb.proof
package query.repo

import zio.*
import zio.json.*
import zio.config.*
import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.*
import org.bson.json.JsonObject
import fiftyforms.mongo.MongoJsonRepository

// TODO: extract common mongo repo config, just nest under mongo / <aggregateRoot>
case class MongoProofConfig(db: String, collection: String)

object MongoProofConfig {
  val configDesc =
    import ConfigDescriptor.*
    nested("MONGO")(
      nested("PROOF")(
        string("DB").default("mdrpdb") zip string("COLL").default("proof")
      )
    )
      .to[MongoProofConfig]
  val fromEnv: ZLayer[System, ReadError[String], MongoProofConfig] =
    ZConfig.fromSystemEnv(configDesc)
}

object MongoProofRepository:
  private val repo =
    for
      config <- ZIO.service[MongoProofConfig]
      client <- ZIO.service[MongoClient]
      coll <- Task.attempt {
        client
          .getDatabase(config.db)
          .getCollection[JsonObject](config.collection)
      }
    yield MongoProofRepository(coll)

  val layer: RLayer[MongoProofConfig & MongoClient, ProofRepository] =
    repo.toLayer

  private[query] val writeLayer
      : RLayer[MongoProofConfig & MongoClient, ProofRepositoryWrite] =
    repo.toLayer

private[query] class MongoProofRepository(
    collection: MongoCollection[JsonObject]
) extends ProofRepositoryWrite:
  import ProofRepository.*
  import mdr.pdb.proof.codecs.Codecs.given

  private val jsonRepo = MongoJsonRepository[Proof, String, Criteria](
    collection, {
      _ match
        case WithId(id)    => equal("id", id)
        case OfPerson(osc) => equal("person", osc.toString)
    },
    p => ("id", p.id)
  )

  export jsonRepo.matching

  export jsonRepo.put
