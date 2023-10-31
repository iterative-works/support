package works.iterative.ui

import com.raquo.laminar.api.L.*
import works.iterative.core.*
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.components.laminar.forms.*
import works.iterative.ui.components.laminar.tailwind.ui.{TailwindUICatalogue, TailwindUIFormBuilderModule, TailwindUIFormUIFactory}
import works.iterative.ui.scenarios.Scenario
import works.iterative.ui.scenarios.Scenario.Id

object FormsScenarioModule
    extends Scenario
    with TailwindUIFormBuilderModule
    with TailwindUIFormUIFactory:

  override val id: Id = "registrace"

  override val label: String = "Žádost o registraci"

  override def element(using ComponentContext[?]): HtmlElement =
    val schema =
      import FormSchema.*
      val kontaktniOsobaSchema: FormSchema[KontaktniOsoba] =
        (
          Control[UserName]("jmeno") *: Control[Email](
            "email"
          ) *: FormSchema.Unit
        ).map(
          KontaktniOsoba.apply
        )(k => (k.jmeno, k.email))

      val adresaSchema: FormSchema[Adresa] =
        (
          Control[PlainOneLine]("ulice") *: Control[PlainOneLine](
            "mesto"
          ) *: Control[PSC]("psc") *: Control[Country](
            "country"
          ) *: FormSchema.Unit
        ).map(
          Adresa.apply
        )(a => (a.ulice, a.mesto, a.psc, a.country))

      val zadatelSchema: FormSchema[Zadatel] =
        (
          Control[PlainOneLine]("nazev") *: Control[IC](
            "ic"
          ) *: adresaSchema *: FormSchema.Unit
        ).map(
          Zadatel.apply
        )(z => (z.nazev, z.ic, z.adresa))

      Section(
        "zadost-o-registraci",
        Section("zadatel", zadatelSchema) *:
          Section("administrator", kontaktniOsobaSchema) *:
          Section("pccr", kontaktniOsobaSchema) *: FormSchema.Unit
      )
        .map(
          ZadostORegistraci.apply
        )(z => (z._1, z._2, z._3))

    import TailwindUIFormBuilderModule.given
    // Form schema
    TailwindUICatalogue.layout.card(
      buildForm[ZadostORegistraci](schema, Observer.empty).build(None).elements*
    )
