package portaly.forms
package impl

import zio.*
import com.raquo.laminar.api.L.*
import works.iterative.core.*
import works.iterative.ui.laminar.*
import works.iterative.tapir.ClientEndpointFactory
import portaly.forms.service.impl.ViesConfig
import works.iterative.core.czech.ICO
import java.time.LocalDate
import works.iterative.ui.model.forms.{RelativePath, AbsolutePath, IdPath}
import portaly.forms.service.impl.rest.ViesEndpoints

class BaseValidationResolver(
    factory: ClientEndpointFactory,
    endpoints: ViesEndpoints,
    viesConfig: ViesConfig
)(using Runtime[Any])
    extends ValidationResolver:
    private val viesClient = factory.make(endpoints.vies.check)

    private def validateFutureDate(id: IdPath): Validation = ValidationRule.succeed(date =>
        if date.isBlank then ValidationState.Valid(date)
        else
            scala.util.Try {
                val d = new scala.scalajs.js.Date(date)
                val ld = java.time.LocalDate.of(
                    d.getFullYear().toInt,
                    d.getMonth().toInt + 1,
                    d.getDate().toInt
                )
                ld
            }.fold(
                _ => ValidationState.Invalid(id, UserMessage("error.invalid.date.format")),
                ld =>
                    if ld.isAfter(LocalDate.now()) then ValidationState.Valid(date)
                    else ValidationState.Invalid(id, UserMessage("error.invalid.date.past"))
            )
    )

    private def validateIco(icField: IdPath, countryField: IdPath): Validation = ValidationRule(
        ico =>
            EventStream.unit().sample(FormCtx.ctx.state.get(countryField)).map {
                case Some("CZ") => ICO(ico).map(_.value).toValidationState(icField)
                case v          => ValidationState.Valid(ico)
            }
    )

    private def absolutePath(id: IdPath)(using FormCtx): AbsolutePath = id match
        case a: AbsolutePath => a
        case r: RelativePath => FormCtx.ctx.path match
                case a: AbsolutePath => a / r
                case x: RelativePath => works.iterative.ui.model.forms.IdPath.Root / x / r

    private def validateMeridlo(id: IdPath): SectionValidation = ValidationRule.succeed:
        val fullPath = absolutePath(id)
        def nonEmpty(field: String): FormR => Boolean =
            _.getFirst(fullPath / field).exists {
                case s: String => s.nonEmpty
                case _         => false
            }
        end nonEmpty
        val hasVyrobniCislo = nonEmpty("vyrobni_cislo")
        val hasEvidencniCislo = nonEmpty("evidencni_cislo")
        def hasVyrobniOrEvidencniCislo(m: FormR) =
            hasVyrobniCislo(m) || hasEvidencniCislo(m)
        ValidationState.failUnless[FormR](id)(hasVyrobniOrEvidencniCislo)(
            UserMessage("error.meridlo.vyrobniOrEvidencniCislo.required")
        )

    private def validateSouhlas(id: IdPath): SectionValidation = ValidationRule.succeed:
        val fullPath = absolutePath(id)
        def hasSouhlas: FormR => Boolean = m =>
            m.getFirst(fullPath / "souhlas").exists {
                case s: String => s == "true"
                case _         => false
            }
        ValidationState.failUnless[FormR](id)(hasSouhlas)(
            UserMessage("error.souhlas.required")
        )

    private def validateSluzby(id: IdPath): SectionValidation = ValidationRule.succeed:
        val fullPath = absolutePath(id)
        def hasSluzby: FormR => Boolean = m =>
            m.get(fullPath / "sluzby" / "__items").exists(_.nonEmpty)

        ValidationState.failUnless[FormR](id)(hasSluzby)(
            UserMessage("error.sluzby.required")
        )

    private def validateVies(id: IdPath, countryField: IdPath): Validation =
        val tooShort: ValidationRule[EventStream, String, String] = ValidationRule.succeed: vatId =>
            ValidationState.failIf[String](id)(_.length < 2)(
                UserMessage("error.validation.vatId.tooShort")
            )(vatId)
        val tooLong: ValidationRule[EventStream, String, String] = ValidationRule.succeed: vatId =>
            ValidationState.failIf[String](id)(_.length > 12)(
                UserMessage("error.validation.vatId.tooLong")
            )(vatId)
        def euVies(vatId: String): EventStream[ValidationState[String]] =
            viesClient(vatId.take(2), vatId.drop(2)).map {
                case Some(false) =>
                    ValidationState.Invalid(id, UserMessage("error.validation.vies.failed"))
                // Either good, or VIES failed
                // We will not refuse to submit on VIES service failure
                case _ => ValidationState.Valid(vatId)
            }.toEventStream
        val vies: ValidationRule[EventStream, String, String] = ValidationRule(vatId =>
            EventStream.unit().sample(FormCtx.ctx.state.get(countryField)).flatMapSwitch {
                case Some(cc: String) if viesConfig.availableCountries.contains(cc) => euVies(vatId)
                case _ => EventStream.fromValue(ValidationState.Valid(vatId))
            }
        )

        tooShort.flatMap(tooLong).flatMap(vies)
    end validateVies

    override def resolve(fieldType: String): IdPath => Validation = id =>
        fieldType match
            case "base:vatId" =>
                // TODO: configure country field from outside
                validateVies(id, id.up / "stat")
            case "czech:ico"             => validateIco(id, id.up / "stat")
            case "base:email"            => ValidationRule.fromValidatedString(id)(Email)
            case "base:phone"            => ValidationRule.fromValidatedString(id)(Phone)
            case "cmi:preferovane_datum" => validateFutureDate(id)
            case "number" => ValidationRule.fromZValidation(id)((v: String) =>
                    zio.prelude.Validation.fromPredicateWith(UserMessage("error.number.format"))(v)(
                        _.forall(_.isDigit)
                    )
                )
            case "number:real_non_negative" => ValidationRule.fromZValidation(id)((v: String) =>
                    zio.prelude.Validation.fromPredicateWith(
                        UserMessage("error.number.real_non_negative.format")
                    )(v)(a => scala.util.Try(v.replace(',', '.').toDouble).toOption.exists(_ >= 0))
                )
            case "number:natural" => ValidationRule.fromZValidation(id)((v: String) =>
                    zio.prelude.Validation.fromPredicateWith(
                        UserMessage("error.number.natural.format")
                    )(v)(a => scala.util.Try(v.toInt).toOption.exists(_ >= 0))
                )
            case _ => ValidationRule.valid

    override def resolveSection(sectionType: String): IdPath => SectionValidation = id =>
        sectionType match
            case "cmi:meridlo" => validateMeridlo(id)
            case "cmi:souhlas" => validateSouhlas(id)
            case "cmi:sluzby"  => validateSluzby(id)
            case _             => ValidationRule.valid

end BaseValidationResolver

object BaseValidationResolver:
    def layer(endpoints: ViesEndpoints)
        : URLayer[ClientEndpointFactory & ViesConfig, ValidationResolver] =
        ZLayer {
            for
                given Runtime[Any] <- ZIO.runtime[Any]
                factory <- ZIO.service[ClientEndpointFactory]
                config <- ZIO.service[ViesConfig]
            yield BaseValidationResolver(factory, endpoints, config)
        }
end BaseValidationResolver
