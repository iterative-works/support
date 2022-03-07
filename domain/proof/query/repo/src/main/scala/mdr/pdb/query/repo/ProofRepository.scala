package mdr.pdb
package proof
package query.repo

import zio.*

object ProofRepository:
  sealed trait Criteria
  case class WithId(id: Proof.Id) extends Criteria
  case class OfPerson(osc: OsobniCislo) extends Criteria

trait ProofRepository:
  import ProofRepository.*
  def matching(criteria: Criteria): Task[Seq[Proof]]

private[query] trait ProofRepositoryWrite extends ProofRepository:
  def put(proof: Proof): Task[Unit]
