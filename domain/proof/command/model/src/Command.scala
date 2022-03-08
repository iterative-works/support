package mdr.pdb
package proof
package command

import java.time.LocalDate
import java.time.Instant

sealed trait Command

sealed trait AuthorizeOption
case object Unauthorized extends AuthorizeOption
case class Authorized(note: Option[String]) extends AuthorizeOption

case class CreateProof(
    id: Proof.Id,
    person: OsobniCislo,
    parameterId: String,
    criterionId: String,
    documents: List[DocumentRef],
    authorize: AuthorizeOption
) extends Command

case class AuthorizeProof(
    id: Proof.Id,
    note: Option[String]
) extends Command

case class UpdateProof(
    id: Proof.Id,
    documents: List[DocumentRef]
) extends Command

case class RevokeProof(
    id: Proof.Id,
    reason: RevocationReason,
    since: Instant,
    documents: List[DocumentRef]
) extends Command
