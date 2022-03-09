package mdr.pdb
package proof
package query.repo

import zio.*

object ProofRepository:
  sealed trait Criteria
  case class WithId(id: Proof.Id) extends Criteria
  case class OfPerson(osc: OsobniCislo) extends Criteria

  def matching(criteria: Criteria): RIO[ProofRepository, Seq[Proof]] =
    ZIO.serviceWithZIO(_.matching(criteria))

trait ProofRepository:
  import ProofRepository.*
  def matching(criteria: Criteria): Task[Seq[Proof]]

private[query] trait ProofRepositoryWrite extends ProofRepository:
  def put(proof: Proof): Task[Unit]

private[query] object ProofRepositoryWrite:
  def matching(
      criteria: ProofRepository.Criteria
  ): RIO[ProofRepositoryWrite, Seq[Proof]] =
    ZIO.serviceWithZIO(_.matching(criteria))
  def put(proof: Proof): RIO[ProofRepositoryWrite, Unit] =
    ZIO.serviceWithZIO(_.put(proof))
