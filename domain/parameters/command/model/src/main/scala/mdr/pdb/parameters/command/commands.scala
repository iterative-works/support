package mdr.pdb
package parameters
package command

import java.time.LocalDate
import java.time.Instant

sealed trait Command

case class AuthorizeProof(
    osoba: OsobniCislo,
    parametr: Parameter.Id,
    kriterium: ParameterCriterion.Id,
    dukaz: List[DocumentRef],
    platiDo: Option[LocalDate]
) extends Command
