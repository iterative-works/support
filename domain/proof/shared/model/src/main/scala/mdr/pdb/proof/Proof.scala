package mdr.pdb
package proof

import java.time.Instant
import java.time.LocalDate

case class Authorization(
    time: Instant,
    person: OsobniCislo
)

case class Revocation(
    time: Instant,
    person: OsobniCislo,
    explanation: String,
    documents: List[DocumentRef]
)

case class Proof(
    person: OsobniCislo,
    id: Proof.Id,
    parameterId: String,
    criterionId: String,
    documents: List[DocumentRef],
    note: String,
    authorizations: List[Authorization],
    expiration: Option[LocalDate],
    revocation: Option[Revocation]
)

object Proof:
  type Id = String
