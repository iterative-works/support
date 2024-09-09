package portaly.forms

import zio.prelude.fx.ZPure
import zio.ZEnvironment
import zio.json.ast.Json
import works.iterative.ui.model.forms.FormState
import works.iterative.ui.model.forms.AbsolutePath

class FormRJsonEncoder:
    def toJsonAST(
        form: Form,
        state: FormState
    ): Json.Obj = render(
        works.iterative.ui.model.forms.IdPath.Root / form.id,
        form.version,
        form.elems
    ).provideEnvironment(
        ZEnvironment(state)
    ).run

    private def render(
        path: AbsolutePath,
        version: String,
        elems: List[SectionSegment]
    ): ZPure[Nothing, Unit, Unit, FormState, Nothing, Json.Obj] =
        for
            children <-
                ZPure.foreach(elems)(elem =>
                    renderSegment(path)(elem).map(_.map(v => Json.Obj(elem.id.last -> v)))
                )
        yield Json.Obj(
            "id" -> Json.Str(path.toHtmlId),
            "version" -> Json.Str(version),
            "data" -> children.flatten.reduceRight(_.merge(_))
        )
    end render

    private def renderSegment(path: AbsolutePath)(element: SectionSegment)
        : ZPure[Nothing, Unit, Unit, FormState, Nothing, Option[Json]] =
        element match
            case Section(id, elems, _)                   => renderSection(path / id)(elems)
            case Field(id, fieldType, default, optional) => renderField(path / id)
            case File(id, multiple, optional)            => renderFileField(path / id)
            case Date(id)                                => renderField(path / id)
            case Enum(id, values, default)               => renderField(path / id)
            case ShowIf(condition, elem) =>
                resolveCondition(path)(condition).flatMap(if _ then renderSegment(path)(elem)
                else ZPure.succeed(None))
            case Repeated(id, default, _, elems) =>
                val elemMap = elems.map(e => e.id.last -> e).toMap
                for
                    items <- getItemsFor(path / id)
                    rendered <- ZPure.foreach(items): (i, t) =>
                        renderSegment(path / id / i)(elemMap(t))
                yield Some(Json.Arr(rendered.flatten*))
                end for
            case _ => ZPure.succeed(None)

    private def getString(path: AbsolutePath)
        : ZPure[Nothing, Unit, Unit, FormState, Nothing, Option[String]] =
        ZPure.serviceWith[FormState](_.getString(path))

    private def getStringList(path: AbsolutePath)
        : ZPure[Nothing, Unit, Unit, FormState, Nothing, Option[List[String]]] =
        ZPure.serviceWith[FormState](_.getStringList(path))

    private def getItemsFor(path: AbsolutePath)
        : ZPure[Nothing, Unit, Unit, FormState, Nothing, List[(String, String)]] =
        ZPure.serviceWith[FormState](_.itemsFor(path))

    private def renderSection(path: AbsolutePath)(elems: List[SectionSegment]) =
        for
            children <-
                ZPure.foreach(elems)(elem =>
                    renderSegment(path)(elem).map(_.map(v => Json.Obj(elem.id.last -> v)))
                )
        yield
            val fields = children.flatten
            if fields.nonEmpty then Some(fields.reduceLeft(_.merge(_))) else None
    end renderSection

    private def renderField(path: AbsolutePath) = getString(path).map(_.map(Json.Str(_)))

    private def renderFileField(path: AbsolutePath) =
        getStringList(path).map(_.map(v => Json.Arr(v.map(Json.Str(_))*)))

    private def resolveCondition(path: AbsolutePath)(condition: Condition)
        : ZPure[Nothing, Unit, Unit, FormState, Nothing, Boolean] =
        import Condition.*
        condition match
            case Never  => ZPure.succeed(false)
            case Always => ZPure.succeed(true)
            case AnyOf(conditions*) =>
                ZPure.foreach(conditions)(resolveCondition(path)).map(_.reduce(_ || _))
            case AllOf(conditions*) =>
                ZPure.foreach(conditions)(resolveCondition(path)).map(_.reduce(_ && _))
            case IsEqual(id, value) =>
                getString(works.iterative.ui.model.forms.IdPath.parse(id, path))
                    .map(_.contains(value))
            case IsValid(id) => ZPure.succeed(true)
            case NonEmpty(id) =>
                getString(works.iterative.ui.model.forms.IdPath.parse(id, path))
                    .map(_.nonEmpty)
        end match
    end resolveCondition
end FormRJsonEncoder
