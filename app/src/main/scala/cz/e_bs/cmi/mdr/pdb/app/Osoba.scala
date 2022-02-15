package cz.e_bs.cmi.mdr.pdb.app

import java.time.LocalDate
import java.time.Instant

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

case class PracovniPomer(
    druh: String,
    pocatek: LocalDate,
    konec: Option[LocalDate]
)

case class Funkce(
    nazev: String,
    stredisko: String,
    voj: String
)

case class Osoba(
    osobniCislo: String,
    jmeno: String,
    email: String,
    telefon: String,
    img: Option[String],
    hlavniFunkce: Funkce,
    pracovniPomer: PracovniPomer,
    parametry: List[Parametr]
)
