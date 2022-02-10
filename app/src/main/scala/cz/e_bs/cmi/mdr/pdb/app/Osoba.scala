package cz.e_bs.cmi.mdr.pdb.app

import java.time.LocalDate

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
    pracovniPomer: PracovniPomer
)
