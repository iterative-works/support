package mdr.pdb
package parameters
package command

import java.time.LocalDate
import java.time.Instant

sealed trait Command

type DocumentRef = String

case class AutorizujDukaz(
    osoba: OsobniCislo,
    parametr: Parameter.Id,
    kriterium: ParameterCriterion.Id,
    dukaz: List[DocumentRef],
    platiDo: Option[LocalDate]
) extends Command
