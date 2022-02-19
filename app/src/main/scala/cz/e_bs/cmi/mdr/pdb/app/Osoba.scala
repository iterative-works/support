package cz.e_bs.cmi.mdr.pdb.app

import java.time.LocalDate
import java.time.Instant
import cz.e_bs.cmi.mdr.pdb.OsobniCislo

case class Potvrzeni(
    uzivatel: String,
    datum: Instant
)

case class Dukaz(
    doklady: List[String],
    potvrzeno: Option[Potvrzeni],
    platiDo: Option[Instant]
)

case class Kriterium(
    id: String,
    nazev: String,
    dukaz: Option[Dukaz]
)

case class Parametr(
    id: String,
    nazev: String,
    kriteria: List[Kriterium]
)
