package mdr.pdb
package proof

import java.time.Instant

case class ProofEvent(event: Event, meta: WW)

object ProofEvent:
  val Tag = "proof"

sealed trait Event:
  def id: Proof.Id

case class ProofCreated(
    id: Proof.Id,
    person: OsobniCislo,
    parameterId: String,
    criterionId: String,
    documents: List[DocumentRef]
) extends Event

case class ProofUpdated(
    id: Proof.Id,
    documents: List[DocumentRef]
) extends Event

case class ProofAuthorized(
    id: Proof.Id,
    note: Option[String]
) extends Event

case class ProofRevoked(
    id: Proof.Id,
    reason: RevocationReason,
    revokedSince: Instant,
    documents: List[DocumentRef]
) extends Event
