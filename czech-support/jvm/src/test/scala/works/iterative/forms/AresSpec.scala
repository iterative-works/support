// PURPOSE: Characterization tests for the ARES economic-subject model — the derived
// PURPOSE: address accessors the complete_ares button fills form fields from

package works.iterative.forms.czech

import zio.test.*
import works.iterative.forms.service.impl.Ares.*

object AresSpec extends ZIOSpecDefault:

    def sidlo(
        nazevObce: Option[String] = None,
        cisloDomovni: Option[Int] = None,
        cisloOrientacni: Option[Int] = None,
        nazevCastiObce: Option[String] = None,
        nazevUlice: Option[String] = None,
        psc: Option[Int] = None
    ): Sidlo = Sidlo(
        kodStatu = "CZ",
        nazevStatu = "Česko",
        kodKraje = None,
        nazevKraje = None,
        kodOkresu = None,
        nazevOkresu = None,
        kodObce = None,
        nazevObce = nazevObce,
        cisloDomovni = cisloDomovni,
        cisloOrientacni = cisloOrientacni,
        kodCastiObce = None,
        nazevCastiObce = nazevCastiObce,
        kodUlice = None,
        nazevUlice = nazevUlice,
        kodAdresnihoMista = None,
        psc = psc,
        textovaAdresa = None
    )

    def subjekt(s: Sidlo): EkonomickySubjekt =
        EkonomickySubjekt("06510446", "Iterative Works s.r.o.", s, Some("CZ06510446"))

    def spec = suite("Ares.EkonomickySubjekt")(
        test("street joins the street name with house/orientation numbers") {
            val v = subjekt(sidlo(
                nazevObce = Some("Praha"),
                nazevUlice = Some("Hlavní"),
                cisloDomovni = Some(12),
                cisloOrientacni = Some(3)
            ))
            assertTrue(v.ulice == "Hlavní 12/3")
        },
        test("street name falls back to part of town, then town") {
            val fallback = subjekt(sidlo(
                nazevObce = Some("Praha"),
                nazevCastiObce = Some("Malá Strana"),
                cisloDomovni = Some(7)
            ))
            val townOnly = subjekt(sidlo(nazevObce = Some("Praha"), cisloDomovni = Some(7)))
            assertTrue(
                fallback.ulice == "Malá Strana 7",
                townOnly.ulice == "Praha 7"
            )
        },
        test("form-field accessors derive from the registered address") {
            val v = subjekt(sidlo(nazevObce = Some("Praha"), psc = Some(11000)))
            assertTrue(
                v.nazev == "Iterative Works s.r.o.",
                v.mesto == Some("Praha"),
                v.psc == Some("11000"),
                v.stat == "CZ",
                v.dic == Some("CZ06510446")
            )
        }
    )
end AresSpec
