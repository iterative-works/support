package works.iterative.ui

import com.raquo.laminar.api.L.*
import works.iterative.core.*
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.components.laminar.forms.*
import works.iterative.ui.components.laminar.tailwind.ui.{TailwindUICatalogue, TailwindUIFormBuilderModule, TailwindUIFormUIFactory}
import works.iterative.ui.scenarios.Scenario
import works.iterative.ui.scenarios.Scenario.Id

object MedecaFormRMV
    extends Scenario
    with TailwindUIFormBuilderModule
    with TailwindUIFormUIFactory:

  override val id: Id = "registrace"

  override val label: String = "Realizace metrologických výkonů"

  override def element(using ComponentContext[?]): HtmlElement =
    val schema =
      import FormSchema.*
        val adresaSchema: FormSchema[Adresa] =
          (
            Control[PlainOneLine]("ulice") *:
            Control[PlainOneLine]("mesto") *:
            Control[String]("psc", multiChoice = Some(MultiChoice(
              List(
                "Test",
                "Radio",
                "Buttonu"
                ), false))) *:
            Control[String]("country", Some(List("ČR", "SK", "USA"))) *:
            FormSchema.Unit
          ).map(Adresa.apply)(a => (a.ulice, a.mesto, a.psc, a.country))

        val zadatelSchema: FormSchema[Applicant] = 
          (
            Control[PlainOneLine]("Název") *:
            Section("Kontaktní adresa", adresaSchema) *:
            Control[PlainOneLine]("IČ") *:
            Control[PlainOneLine]("DIČ") *:
            Control[PlainOneLine]("Načíst přes ARES", onBtnClick = Some(()=>Console.println("bylo kliknuto"))) *:
            Control[PlainOneLine]("Zadat korespondenční adresu", checked=Some(true)) *:
            Section("Kontaktní adresa", adresaSchema) *:
            FormSchema.Unit
          ).map(Applicant.apply)(a => (a.nazev, a.ardesa, a.ic, a.dic, a.nacistARES, a.zadatKorespon, a.korespon))

        val kontaktniOsobaSchema: FormSchema[KontaktniOsoba] =
          (
            Control[UserName]("jmeno") *: Control[Email](
              "email"
            ) *: FormSchema.Unit
          ).map(
            KontaktniOsoba.apply
          )(k => (k.jmeno, k.email))
        
        


      Section(
        "Realizace metrologických výkonů",
          Section("zadatel", zadatelSchema) *:
            Section("prihl", kontaktniOsobaSchema) *:
            FormSchema.Unit
      )
        .map(
          ZadostOVykon.apply
        )(z => (z.zadatel, z.kontaktniOsoba))

    import TailwindUIFormBuilderModule.given
    // Form schema
    TailwindUICatalogue.layout.card(
      buildForm[ZadostOVykon](schema, Observer.empty).build(None).elements*
    )
