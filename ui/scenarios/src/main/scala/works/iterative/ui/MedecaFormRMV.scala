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
        // val adresaSchema: FormSchema[Adresa] =
        //   (
        //     Control[PlainOneLine]("ulice") *:
        //     Control[PlainOneLine]("mesto") *:
        //     Control[String]("psc") *:
        //     Control[String]("country", Some(List("ČR", "SK", "USA"))) *:
        //     FormSchema.Unit
        //   ).map(Adresa.apply)(a => (a.ulice, a.mesto, a.psc, a.country))

        val zadatelSchema: FormSchema[Applicant] = 
          (
            Control[PlainOneLine]("Název") *:
            Control[PlainOneLine]("Ulice") *:
            Control[PlainOneLine]("Město") *:
            Control[String]("PSČ") *:
            Control[String]("Stát", Some(List("Česká Republika", "Slovenská Republika"))) *:
            Control[PlainOneLine]("IČ") *:
            Control[PlainOneLine]("DIČ") *:
            Control[PlainOneLine]("Načíst přes ARES", onBtnClick = Some(()=>Console.println("bylo kliknuto"))) *:
            Control[PlainOneLine]("Zadat korespondenční adresu", checked=Some(true)) *:
            Control[PlainOneLine]("Ulice") *:
            Control[PlainOneLine]("Město") *:
            Control[String]("PSČ") *:
            Control[String]("Stát", Some(List("Česká Republika", "Slovenská Republika"))) *:
            FormSchema.Unit
          ).map(Applicant.apply)(a => (a.nazev, a.ulice, a.mesto, a.psc, a.country, a.ic, a.dic, a.nacistARES, a.zadatKorespon, a.koresponUlice, a.koresponMesto, a.koresponPsc, a.koresponCountry))

        val kontaktniOsobaSchema: FormSchema[ContactPerson] =
          (
            Control[PlainOneLine]("Jméno") *:
            Control[PlainOneLine]("Příjmení") *:
            Control[PlainOneLine]("Telefon") *:
            Control[PlainOneLine]("Email") *:
            FormSchema.Unit
          ).map(
            ContactPerson.apply
          )(k => (k.jmeno, k.prijmeni, k.telefon, k.email))
        
        val serviceSelectSchema: FormSchema[ServiceSelect] =
          (
            Control[String](
              "Služba",
              multiChoice = Some(MultiChoice(
                List(
                  "Ověření stanoveného měřidla (S2602)",
                  "Kalibrace hlavních etalonů (S2615)",
                  "Schválení typů měřidel (S2616)",
                  "Schválení typů dovezených měřidel (S2617)",
                  "Přezkoumání stanoveného měřidla (S2619)",
                  "Jiný"
                ),
                true
              ))
              )
          ).map(
            ServiceSelect.apply
          )(s => (s.sluzba))
        


      Section(
        "Realizace metrologických výkonů",
          Section("Žadatel", zadatelSchema) *:
          Section("Kontaktní osoba žadatele", kontaktniOsobaSchema) *:
          Section("Výběr požadované služby", serviceSelectSchema) *:
          FormSchema.Unit
      )
        .map(
          ZadostOVykon.apply
        )(z => (z.zadatel, z.kontaktniOsoba, z.serviceSelect))

    import TailwindUIFormBuilderModule.given
    // Form schema
    TailwindUICatalogue.layout.card(
      buildForm[ZadostOVykon](schema, Observer.empty).build(None).elements*
    )
