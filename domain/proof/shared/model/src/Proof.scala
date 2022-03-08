package mdr.pdb
package proof

import java.time.Instant
import java.time.LocalDate

sealed abstract class RevocationReason(msg: String)
case object Expired extends RevocationReason("Vypršela platnost důkazu")
case class Other(msg: String) extends RevocationReason(msg)

case class Authorization(
    authorized: WW,
    note: Option[String]
)

case class Revocation(
    revoked: WW,
    revokedSince: Instant,
    reason: RevocationReason,
    documents: List[DocumentRef]
)

case class Proof(
    id: Proof.Id,
    person: OsobniCislo,
    parameterId: String,
    criterionId: String,
    documents: List[DocumentRef],
    authorizations: List[Authorization],
    revocations: List[Revocation],
    created: WW
) {
  def isAuthorized = authorizations.nonEmpty
  def isRevoked = revocations.exists(_.revokedSince.isBefore(Instant.now()))
}

object Proof:
  type Id = String
