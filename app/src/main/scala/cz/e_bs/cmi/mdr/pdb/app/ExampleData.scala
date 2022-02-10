package cz.e_bs.cmi.mdr.pdb.app

import java.time.LocalDate

object ExampleData:
  object persons:
    val jmeistrova =
      Osoba(
        "1031",
        "Ing. Jana Meistrová",
        "jmeistrova@cmi.cz",
        "+420222866180",
        None,
        Funkce(
          "manažerka jakosti ČMI",
          "generální ředitel, MJ ČMI",
          "úsek generálního ředitele"
        ),
        PracovniPomer("HPP", LocalDate.of(2005, 7, 1), None)
      )
