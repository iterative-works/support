package mdr.pdb
package frontend

import java.time.LocalDate
import java.time.Instant

sealed trait Command

type DocumentRef = String

case class AutorizujDukaz(
    osoba: OsobniCislo,
    parametr: Parameter.Id,
    kriterium: ParameterCriteria.Id,
    dukaz: List[DocumentRef],
    platiDo: Option[LocalDate]
) extends Command
