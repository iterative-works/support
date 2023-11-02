package works.iterative.ui

import works.iterative.core.UserName
import works.iterative.core.Email
import works.iterative.core.PlainOneLine

type PSC = String
type Country = String
type IC = String

final case class KontaktniOsoba(
    jmeno: UserName,
    email: Email
)

final case class Prihlaseni(
    jmeno: UserName,
    email: Email
)

final case class Adresa(
  ulice: PlainOneLine,
  mesto: PlainOneLine,
  psc: String,
  country: String
)

final case class Zadatel(nazev: PlainOneLine, ic: String, adresa: Adresa)

final case class ZadostORegistraci(zadatel: Zadatel, administrator: KontaktniOsoba, pccr: KontaktniOsoba)

final case class ZadostOVykon(zadatel: Applicant, kontaktniOsoba: KontaktniOsoba)

final case class ContactPerson(jmeno: PlainOneLine, prijmeni: PlainOneLine, telefon: PlainOneLine, email: PlainOneLine)

final case class Applicant(
  nazev: PlainOneLine,
  ardesa: Adresa,
  ic: PlainOneLine,
  dic: PlainOneLine,
  nacistARES: PlainOneLine,
  zadatKorespon: PlainOneLine,
  korespon: Adresa
)