package portaly.forms
package service
package impl

import zio.json.*
import works.iterative.tapir.CustomTapir.*

object Ares:
    case class Sidlo(
        kodStatu: String,
        nazevStatu: String,
        kodKraje: Option[Int],
        nazevKraje: Option[String],
        kodOkresu: Option[Int],
        nazevOkresu: Option[String],
        kodObce: Option[Int],
        nazevObce: Option[String],
        cisloDomovni: Option[Int],
        cisloOrientacni: Option[Int],
        kodCastiObce: Option[Int],
        nazevCastiObce: Option[String],
        kodUlice: Option[Int],
        nazevUlice: Option[String],
        kodAdresnihoMista: Option[Int],
        psc: Option[Int],
        textovaAdresa: Option[String]
    ) derives JsonCodec, Schema

    case class EkonomickySubjekt(
        ico: String,
        obchodniJmeno: String,
        sidlo: Sidlo,
        dic: Option[String]
    ) derives JsonCodec, Schema:
        val nazev = obchodniJmeno
        val ulice: String =
            val cislo = List(sidlo.cisloDomovni, sidlo.cisloOrientacni).flatten.mkString("/")
            val ulice = sidlo.nazevUlice.orElse(sidlo.nazevCastiObce).orElse(
                sidlo.nazevObce
            ).getOrElse("")
            s"${ulice} ${cislo}"
        end ulice
        val mesto = sidlo.nazevObce
        val psc = sidlo.psc.map(_.toString)
        val stat = sidlo.kodStatu

    end EkonomickySubjekt
end Ares
