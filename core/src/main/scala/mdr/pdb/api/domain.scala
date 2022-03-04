package mdr.pdb
package api

import java.time.LocalDate
import java.time.Instant
import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

sealed trait Command

type DocumentRef = String

case class AutorizujDukaz(
    osoba: OsobniCislo,
    parametr: Parameter.Id,
    kriterium: ParameterCriteria.Id,
    dukaz: List[DocumentRef],
    platiDo: Option[LocalDate]
) extends Command

object AutorizujDukaz:
  given JsonCodec[AutorizujDukaz] = DeriveJsonCodec.gen
