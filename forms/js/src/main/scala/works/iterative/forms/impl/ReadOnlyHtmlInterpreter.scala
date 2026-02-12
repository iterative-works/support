package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import works.iterative.core.MessageCatalogue
import com.raquo.laminar.api.L
import scala.annotation.unused
import works.iterative.ui.model.forms.{RelativePath, AbsolutePath}

class ReadOnlyHtmlInterpreter(
    layoutResolver: LayoutResolver,
    cs: ReadOnlyComponents
)(using messages: MessageCatalogue) extends Interpreter[FormR, HtmlElement]:
    import ReadOnlyHtmlInterpreter.{*, given}

    type Data = FormR
    type Render = Ctx ?=> Data ?=> HtmlElement

    override def interpret(
        id: FormIdent,
        form: Form,
        formData: Option[FormR]
    ): HtmlElement =
        given Data = formData.getOrElse(FormR.empty)
        given Ctx = Ctx()
        render(form.id, form.elems)
    end interpret

    extension (id: AbsolutePath)
        @scala.annotation.nowarn("msg=unused implicit parameter")
        def msg(key: String)(using ctx: Ctx): String =
            id.toMessage(key)
        @scala.annotation.nowarn("msg=unused implicit parameter")
        def get(using ctx: Ctx, data: Data): String =
            data.get(id).flatMap(_.headOption.map(_.toString)).getOrElse("")
    end extension

    extension (id: RelativePath)
        def full(using ctx: Ctx): AbsolutePath =
            ctx.path / id

    private def render(id: RelativePath, sections: Seq[SectionSegment]): Render =
        cs.form(
            id.full.msg("title"),
            sections.map(renderSegment(_)(using ctx.nested(id)))
        )

    private def renderSegment(element: SectionSegment): Render =
        element match
            case Section(id, elems, _) => renderSection(id, elems)
            case Field(id, fieldType, default, optional) =>
                renderFormField(id, !optional)
            case File(id, multiple, optional) => renderFileField(id, !optional)
            case Date(id)                     => renderFormField(id, false)
            case Display(id)                  => renderDisplay(id)
            case Button(id)                   => renderButton(id)
            case Enum(id, values, default) =>
                if values.size == 2 && values.contains("true") && values.contains(
                        "false"
                    )
                then renderCheckbox(id, default)
                else renderEnum(id, values, default, required = true)
            case ShowIf(condition, elem) => renderShowIf(condition, elem)
            case Repeated(id, default, _, elems) =>
                val its = (id / "__items").full.get
                if its.isBlank() then div()
                else
                    val items = its.split(",").toList
                    val elemMap = elems.map(e => e.id.last -> e).toMap
                    div(items.map(_.split(":", 2)).collect {
                        case Array(i, t) =>
                            val elem = elemMap(t)
                            renderSegment(elem)(using ctx.nested(id / i))
                    })
                end if

    private def renderShowIf(
        condition: Condition,
        elem: SectionSegment
    ): Render =
        if resolveCondition(condition) then renderSegment(elem) else div()
    end renderShowIf

    private def resolveCondition(
        condition: Condition
    )(using Ctx, Data): Boolean =
        import Condition.*
        condition match
            case Never              => false
            case Always             => true
            case AnyOf(conditions*) => conditions.map(resolveCondition).reduce(_ || _)
            case AllOf(conditions*) => resolveConditions(conditions)
            case IsEqual(id, value) =>
                works.iterative.ui.model.forms.IdPath.parse(id, ctx.path).get == value
            case IsValid(_) => true
            case NonEmpty(id) =>
                works.iterative.ui.model.forms.IdPath.parse(id, ctx.path).get.nonEmpty
        end match
    end resolveCondition

    private def resolveConditions(conditions: Seq[Condition])(using
        Ctx,
        Data
    ): Boolean =
        conditions.map(resolveCondition).reduce(_ && _)

    private def renderSection(
        id: RelativePath,
        elems: List[SectionSegment]
    ): Render =
        def cont(i: SectionSegment) = renderSegment(i)(using ctx.nested(id))
        val layout = layoutResolver.resolve(id.full, elems)
        val content = layout match
            case Grid(elems) =>
                cs.grid(
                    elems.flatMap(segments =>
                        segments.map(s => cs.gridCell(segments.size, cont(s)))
                    )*
                )
            case Flex(elems) =>
                cs.flexRow(elems.map(cont))

        cs.section(
            id.full.toHtmlId,
            id.full.size,
            id.full.toMessageNodeOpt("section"),
            id.full.toMessageNodeOpt("section.subtitle"),
            content
        )
    end renderSection

    private def renderFormField(
        id: RelativePath,
        required: Boolean
    ): Render =
        val v = id.full.get
        cs.labeledField(
            id.full.toHtmlId,
            id.full.msg("label"),
            required,
            true,
            cs.inputValue(id.full.toHtmlId, v, v)
        )
    end renderFormField

    private def renderFileField(
        id: RelativePath,
        required: Boolean
    ): Render =
        cs.labeledField(
            id.full.toHtmlId,
            id.full.msg("label"),
            required,
            true,
            cs.fileValue(id.toHtmlId, id.full.get)
        )
    end renderFileField

    private def renderButton(@unused id: RelativePath): Render = div()

    private def renderCheckbox(id: RelativePath, @unused default: Option[String]): Render =
        renderFormField(id, required = false)

    private def renderEnum(
        id: RelativePath,
        @unused values: List[String],
        @unused default: Option[String],
        required: Boolean
    ): Render =
        renderFormField(id, required)

    private def renderDisplay(@unused id: RelativePath): Render = div()
end ReadOnlyHtmlInterpreter

object ReadOnlyHtmlInterpreter:
    given ctx(using c: Ctx): Ctx = c
    final case class Ctx(path: AbsolutePath = works.iterative.ui.model.forms.IdPath.Root):
        def nested(id: RelativePath): Ctx = copy(path / id)
end ReadOnlyHtmlInterpreter
