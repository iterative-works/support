package mdr.pdb
package proof

import java.time.Instant

sealed trait Event

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
