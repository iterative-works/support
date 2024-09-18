package portaly.forms
package impl

import zio.*
import com.raquo.laminar.api.L
import works.iterative.autocomplete.AutocompleteEntry
import works.iterative.autocomplete.ui.laminar.AutocompleteRegistry
import works.iterative.ui.TimeUtils
import works.iterative.ui.model.forms.{AbsolutePath, IdPath}
import com.raquo.airstream.core.Observer

class LiveFieldTypeResolver(
    autocomplete: AutocompleteRegistry,
    validation: ValidationResolver
) extends FieldTypeResolver:

    override def resolve(fieldType: FieldType): FieldFactory[String] =
        val fieldValidation: IdPath => Validation = validation.resolve(fieldType.id)
        autocomplete.getQueryFor(fieldType.id) match
            case Some(q) =>
                def valuesObserver(using FormCtx) =
                    FormCtx.ctx.updateValues.contramap[(
                        AbsolutePath,
                        AutocompleteEntry
                    )]: (id, entry) =>
                        FormR.Builder().addAll(entry.data).build(id.up)

                def meridlaObserver(using FormCtx) =
                    /*
                    val fieldId = if fieldType.id == "cmi:meridlo_evidcislo" then "evidencni_cislo"
                    else "vyrobni_cislo"
                     */
                    FormCtx.ctx.control.contramap[(AbsolutePath, AutocompleteEntry)]: (id, entry) =>
                        val paths = (entry.data.keySet).map(id.up / _)
                        if entry.data.get("typ_meridla").exists(v => !v.isBlank()) then
                            FormControl.DisableAll(paths.toSeq)
                        else
                            FormControl.EnableAll(paths.toSeq)
                end meridlaObserver

                def autocompleteObserver(using FormCtx) =
                    println(fieldType.id)
                    if fieldType.id == "cmi:meridlo_evidcislo" || fieldType.id == "cmi:meridlo_vyrcislo"
                    then
                        Observer.combine(valuesObserver, meridlaObserver)
                    else valuesObserver
                end autocompleteObserver

                FieldFactory.Autocomplete(
                    q,
                    // Add context from surrounding fields to the query
                    // This is a hack for now, the 3 levels up is a bit arbitrary
                    ctx =>
                        idPath =>
                            val contextLvl = fieldType.context match
                                case Some(ctxLvl) =>
                                    works.iterative.ui.model.forms.IdPath.parse(ctxLvl, idPath)
                                case None => idPath.up
                            ctx.state.under(contextLvl).map(_.map((k, v) =>
                                k match
                                    case ap: AbsolutePath =>
                                        ap.relativeTo(contextLvl).serialize -> v.toString
                                    case _ => k.serialize -> v.toString
                            )).map(Some(_))
                    ,
                    Some(autocompleteObserver)
                )
            case _ =>
                fieldType match
                    case FieldType("prose", _, _) => FieldFactory.TextArea("text", fieldValidation)
                    case FieldType("base:email", _, disabled) =>
                        FieldFactory.Text("email", !disabled, fieldValidation)
                    case FieldType("hidden", _, _) => FieldFactory.Hidden()
                    case FieldType("cmi:preferovane_datum", _, disabled) => FieldFactory.Text(
                            "date",
                            !disabled,
                            fieldValidation,
                            L.minAttr(TimeUtils.htmlDateFormat.format(java.time.LocalDate.now()))
                        )
                    case FieldType("number", _, disabled) =>
                        FieldFactory.Text("number", !disabled, fieldValidation)
                    case FieldType("number:natural", _, disabled) =>
                        FieldFactory.Text("number", !disabled, fieldValidation)
                    case FieldType(_, _, disabled) =>
                        FieldFactory.Text("text", !disabled, fieldValidation)
                end match
        end match
    end resolve

    override def withAutocompleteContext(context: Map[String, String]): LiveFieldTypeResolver =
        LiveFieldTypeResolver(
            autocomplete.addContext(context),
            validation
        )
end LiveFieldTypeResolver

object LiveFieldTypeResolver:
    val layer: URLayer[AutocompleteRegistry & ValidationResolver, FieldTypeResolver] =
        ZLayer {
            for
                autocomplete <- ZIO.service[AutocompleteRegistry]
                validation <- ZIO.service[ValidationResolver]
            yield LiveFieldTypeResolver(autocomplete, validation)
        }
end LiveFieldTypeResolver
