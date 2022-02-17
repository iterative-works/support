package cz.e_bs.cmi.mdr.pdb.app

import cz.e_bs.cmi.mdr.pdb.OsobniCislo

import java.time.LocalDate

object ExampleData:
  object persons:
    val jmeistrova =
      Osoba(
        OsobniCislo("60308"),
        "Ing. Jana Meistrová",
        "jmeistrova@cmi.cz",
        "+420222866180",
        None,
        Funkce(
          "manažerka jakosti ČMI",
          "generální ředitel, MJ ČMI",
          "úsek generálního ředitele"
        ),
        PracovniPomer("HPP", LocalDate.of(2005, 7, 1), None),
        List(
          Parametr(
            "1",
            "Komise pro pověřování pracovníků",
            Nil
          )
        )
      )
