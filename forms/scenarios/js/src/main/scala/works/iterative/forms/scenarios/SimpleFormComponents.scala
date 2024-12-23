package works.iterative.forms.scenarios

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L
import io.laminext.syntax.core.*
import portaly.forms.Components
import org.scalajs.dom.FileList

class SimpleFormComponents extends Components:
    val sectionMargin = "mt-6"
    val sectionSpace = "space-y-8"

    val headerText = "text-gray-900"

    override def formLayout(theForm: HtmlMod, menu: HtmlMod): HtmlElement = div(theForm)

    override def formPart(mods: HtmlMod*): HtmlElement = div(mods)

    override def formTitle(titleMod: HtmlMod, mods: HtmlMod*): HtmlElement =
        h1(cls(s"text-xl font-semibold leading-7 $headerText"), titleMod, mods)

    override def form(
        id: String,
        titleMod: HtmlMod,
        formMods: Option[HtmlMod],
        mods: HtmlMod*
    ): HtmlElement =
        L.form(
            div(
                formMods.getOrElse(cls(s"$sectionMargin $sectionSpace")),
                titleMod,
                mods
            )
        )

    override def segmentRemoveIcon(mods: SvgMod*): SvgElement =
        import svg.*
        svg(
            mods,
            fill("none"),
            viewBox("0 0 24 24"),
            strokeWidth("1.5"),
            stroke("currentColor"),
            path(
                strokeLineCap := "round",
                strokeLineJoin := "round",
                d := "M6 18L18 6M6 6l12 12"
            )
        )
    end segmentRemoveIcon

    override def attachmentRemoveIcon(mods: SvgMod*): SvgElement =
        import svg.*
        svg(
            fill := "none",
            viewBox := "0 0 24 24",
            strokeWidth := "1.5",
            stroke := "currentColor",
            mods,
            path(
                strokeLineCap := "round",
                strokeLineJoin := "round",
                d := "M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"
            )
        )
    end attachmentRemoveIcon

    override def attachmentIcon(mods: SvgMod*): SvgElement =
        import svg.*
        svg(
            mods,
            cls := "flex-shrink-0 text-gray-400",
            viewBox := "0 0 20 20",
            fill := "currentColor",
            aria.hidden := true,
            path(
                fillRule := "evenodd",
                d := "M15.621 4.379a3 3 0 00-4.242 0l-7 7a3 3 0 004.241 4.243h.001l.497-.5a.75.75 0 011.064 1.057l-.498.501-.002.002a4.5 4.5 0 01-6.364-6.364l7-7a4.5 4.5 0 016.368 6.36l-3.455 3.553A2.625 2.625 0 119.52 9.52l3.45-3.451a.75.75 0 111.061 1.06l-3.45 3.451a1.125 1.125 0 001.587 1.595l3.454-3.553a3 3 0 000-4.242z",
                clipRule := "evenodd"
            )
        )
    end attachmentIcon

    override def uploadIcon(mods: SvgMod*): SvgElement =
        import svg.*
        svg(
            mods,
            fill("currentColor"),
            viewBox("0 0 24 24"),
            path(d := "M0 0h24v24H0z", fill("none")),
            path(d := "M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z")
        )
    end uploadIcon

    def errorIcon(mods: SvgMod*): SvgElement =
        import svg.*
        svg(
            mods,
            viewBox("0 0 20 20"),
            fill("currentColor"),
            aria.hidden(true),
            path(
                fillRule("evenodd"),
                d(
                    "M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-5a.75.75 0 01.75.75v4.5a.75.75 0 01-1.5 0v-4.5A.75.75 0 0110 5zm0 10a1 1 0 100-2 1 1 0 000 2z"
                ),
                clipRule("evenodd")
            )
        )
    end errorIcon

    def comboboxIcon(mods: SvgMod*): SvgElement =
        import svg.*
        svg(
            mods,
            viewBox := "0 0 20 20",
            fill := "currentColor",
            aria.hidden := true,
            path(
                fillRule := "evenodd",
                d := "M10 3a.75.75 0 01.55.24l3.25 3.5a.75.75 0 11-1.1 1.02L10 4.852 7.3 7.76a.75.75 0 01-1.1-1.02l3.25-3.5A.75.75 0 0110 3zm-3.76 9.2a.75.75 0 011.06.04l2.7 2.908 2.7-2.908a.75.75 0 111.1 1.02l-3.25 3.5a.75.75 0 01-1.1 0l-3.25-3.5a.75.75 0 01.04-1.06z",
                clipRule := "evenodd"
            )
        )
    end comboboxIcon

    def checkIcon(mods: SvgMod*): SvgElement =
        import svg.*
        svg(
            mods,
            viewBox := "0 0 20 20",
            fill := "currentColor",
            aria.hidden := true,
            path(
                fillRule := "evenodd",
                d := "M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z",
                clipRule := "evenodd"
            )
        )
    end checkIcon

    override def flexRow(mods: HtmlMod*): HtmlElement =
        div(cls(s"flex flex-col md:flex-row md:justify-between"), mods)

    override def button(
        id: String,
        name: String,
        buttonMod: HtmlMod,
        buttonType: String,
        mods: HtmlMod*
    ): HtmlElement =
        L.button(
            tpe(buttonType),
            idAttr(id),
            nameAttr(name),
            buttonLike,
            if buttonType == "submit" then submitButtonMod else defaultButtonMod,
            buttonMod,
            mods
        )

    private val defaultButtonMod = cls(
        "bg-white text-blue-800 hover:bg-gray-50 disabled:hover:bg-white disabled:text-gray-500"
    )

    private val submitButtonMod = cls(
        "bg-blue-800 text-white hover:bg-blue-700 disabled:hover:bg-blue-300 disabled:text-gray-500"
    )

    override def buttonLike: HtmlMod =
        cls(
            "rounded-md px-2.5 py-1.5 text-sm font-semibold shadow-sm ring-1 ring-inset ring-gray-300 cursor-pointer disabled:cursor-default"
        )

    override def section(
        id: String,
        level: Int,
        titleMod: Option[HtmlMod],
        subtitleMod: Option[HtmlMod],
        errors: Signal[List[Node]],
        mods: HtmlMod*
    ): HtmlElement =
        div(
            if level == 2 then cls(s"rounded-lg bg-white shadow") else emptyMod,
            div(
                if level == 2 then cls("px-4 py-5 sm:p-6")
                else if level == 3 then cls("mt-2 border-t border-gray-900/10 pt-2")
                else emptyMod,
                titleMod.map(t => h2(cls("text-base font-semibold leading-7 text-blue-800"), t)),
                subtitleMod.map(t => p(cls("mt-1 text-sm leading-6 text-gray-600"), t)),
                children <-- errors.map:
                    _.map[HtmlElement]:
                        p(cls("mt-2 text-sm text-red-800"), _)
                ,
                mods
            )
        )

    private def requiredDecoration(required: Signal[Boolean]): HtmlMod =
        required.childWhenTrue(span(cls("text-red-800"), "*"))

    override def labeledField(
        id: String,
        labelMod: HtmlMod,
        helpMod: Option[HtmlMod],
        required: Signal[Boolean],
        errors: Signal[List[Node]],
        mods: HtmlMod*
    ): HtmlElement =
        div(
            label(
                forId(id),
                cls("block text-sm font-medium leading-6 text-blue-800"),
                labelMod,
                requiredDecoration(required)
            ),
            div(
                cls("mt-1 pb-4 text-sm"),
                mods,
                children <-- errors.map(
                    _.map[HtmlElement](
                        p(cls("mt-2 text-sm text-red-800"), _)
                    )
                ),
                helpMod.map(h => p(cls("mt-2 text-sm text-gray-500"), h))
            )
        )

    override def fileInput(
        id: String,
        fieldName: String,
        multiple: Boolean,
        labelMod: HtmlMod,
        inputMod: Mod[Input]
    ) =
        div(
            cls := "mt-4 sm:mt-0 sm:flex-none",
            L.label(
                cls("block w-full"),
                div(
                    cls("cursor-pointer"),
                    buttonLike,
                    defaultButtonMod,
                    uploadIcon(svg.cls("w-6 h-6 mr-2")),
                    labelMod
                ),
                L.input(
                    idAttr(id),
                    nameAttr(fieldName),
                    cls("hidden"),
                    tpe("file"),
                    L.multiple(multiple),
                    inputMod
                )
            )
        )
    end fileInput

    // TODO: split to fileInputField and fileInputFieldContainer
    override def fileInputField(
        id: String,
        fieldName: String,
        multiple: Boolean,
        inError: Signal[Boolean],
        buttonMod: HtmlMod
    ): HtmlElement =
        val selectedFiles: Var[Option[FileList]] = Var(None)
        div(
            cls("relative"),
            // File list
            ul(
                cls("pb-2"),
                children <-- selectedFiles.signal.map(_.map(_.toList).getOrElse(Nil).map { file =>
                    li(
                        cls("flex items-center text-sm text-gray-900 py-2"),
                        span(attachmentIcon(svg.cls("flex-shrink-0 h-5 w-5"))),
                        span(cls("ml-2 truncate"), file.name)
                    )
                })
            ),
            // File upload button
            div(
                cls := "mt-4 sm:mt-0 sm:flex-none",
                L.label(
                    cls("block w-full"),
                    div(
                        cls("cursor-pointer"),
                        defaultButtonMod,
                        buttonLike,
                        uploadIcon(svg.cls("w-6 h-6 mr-2")),
                        buttonMod
                    ),
                    L.input(
                        idAttr(id),
                        nameAttr(fieldName),
                        cls("hidden"),
                        tpe("file"),
                        L.multiple(multiple),
                        inContext[Input](thisNode =>
                            onInput.mapTo(thisNode.ref.files) --> selectedFiles.writer.contramapSome
                        )
                    )
                )
            ),
            inError.childWhenTrue(
                div(
                    cls(
                        "pointer-events-none absolute inset-y-0 right-0 pr-3 flex items-center"
                    ),
                    errorIcon(
                        svg.cls("h-5 w-5 text-red-800")
                    )
                )
            )
        )
    end fileInputField

    override def checkbox(
        id: String,
        name: String,
        value: Signal[String],
        labelMod: HtmlMod,
        descriptionMod: Option[HtmlMod],
        inputMods: HtmlMod*
    ): HtmlElement =
        div(
            cls("pb-2 pt-2 relative flex items-start"),
            div(
                cls("flex h-6 items-center"),
                input(
                    idAttr(id),
                    nameAttr(name),
                    aria.describedBy(s"${id}-description"),
                    tpe("checkbox"),
                    cls(
                        "h-4 w-4 rounded border-gray-300 text-blue-800 focus:ring-blue-800"
                    ),
                    checked <-- value.signal.map(_ == "true"),
                    inputMods
                )
            ),
            div(
                cls("ml-3 text-sm leading-6"),
                label(
                    forId(id),
                    cls("font-medium text-blue-800"),
                    labelMod
                ),
                descriptionMod.map(d =>
                    span(
                        idAttr(s"${id}-description"),
                        cls("text-gray-500"),
                        d
                    )
                )
            )
        )

    override def radio(
        id: String,
        name: String,
        labelMod: HtmlMod,
        descriptionMod: Option[HtmlMod],
        required: Signal[Boolean],
        value: Signal[String],
        values: List[Components.RadioOption],
        inError: Signal[Boolean],
        mods: HtmlMod*
    ): HtmlElement =
        fieldSet(
            idAttr(id),
            legend(
                cls("text-sm font-semibold leading-6 text-blue-800"),
                labelMod,
                requiredDecoration(required)
            ),
            descriptionMod.map(t =>
                p(cls("mt-1 text-sm leading-6 text-gray-600"), t)
            ),
            div(
                cls("mt-1 pb-4 space-y-1"),
                values.map: r =>
                    div(
                        cls("flex items-center gap-x-2"),
                        input(
                            idAttr(r.id),
                            nameAttr(name),
                            tpe("radio"),
                            cls(
                                "h-4 w-4 border-gray-300 text-blue-800 focus:ring-blue-800"
                            ),
                            L.value <-- value,
                            checked <-- value.map(_ == r.value),
                            r.mods
                        ),
                        label(
                            forId(r.id),
                            cls("block text-sm font-medium leading-6 text-blue-800"),
                            r.text
                        )
                    )
            ),
            mods
        )

    def select(
        id: String,
        name: String,
        labelMod: HtmlMod,
        descriptionMod: Option[HtmlMod],
        required: Signal[Boolean],
        value: Signal[String],
        values: Signal[List[Components.RadioOption]],
        mods: HtmlMod*
    ): HtmlElement = div(child <-- values.map(vs =>
        select(
            id,
            name,
            labelMod,
            descriptionMod,
            required,
            value,
            vs,
            mods
        )
    ))

    def select(
        id: String,
        name: String,
        labelMod: HtmlMod,
        descriptionMod: Option[HtmlMod],
        required: Signal[Boolean],
        value: Signal[String],
        values: List[Components.RadioOption],
        mods: HtmlMod*
    ): HtmlElement = radio(
        id,
        name,
        labelMod,
        descriptionMod,
        required,
        value,
        values,
        Val(false),
        mods
    )

    override def grid(mods: HtmlMod*): HtmlElement =
        div(
            cls(s"grid grid-cols-1 gap-x-4 sm:grid-cols-6"),
            mods
        )

    override def gridCell(spanNo: Int, mods: HtmlMod*): HtmlElement =
        val colSpan = spanNo match
            case 2 => "sm:col-span-3"
            case 3 => "sm:col-span-2"
            case _ => "sm:col-span-full"

        span(cls(colSpan), mods)
    end gridCell

    override def comboContainer(mods: HtmlMod*): HtmlElement =
        div(cls("relative mt-2"), mods)

    override def comboOptionsContainer(mods: HtmlMod*): HtmlElement =
        ul(
            cls(
                "absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm"
            ),
            role := "listbox",
            mods
        )

    override def comboOption(
        active: Signal[Boolean],
        selected: Signal[Boolean],
        text: Node,
        description: Option[Node],
        mods: HtmlMod*
    ): HtmlElement =
        li(
            cls("relative cursor-default select-none py-2 pl-3 pr-9 text-blue-800"),
            cls("text-white bg-blue-800") <-- active,
            cls("text-blue-800") <-- active.not,
            cls("font-semibold") <-- selected,
            span(cls("block truncate"), text),
            description.map: d =>
                span(
                    active.classSwitch("text-white", "text-blue-800"),
                    cls("block text-sm"),
                    d
                ),
            selected.childWhenTrue(
                span(
                    cls("absolute inset-y-0 right-0 flex items-center pr-4"),
                    cls("text-blue-800") <-- active.not,
                    cls("text-white") <-- active,
                    checkIcon(svg.cls("h-5 w-5"))
                )
            ),
            mods
        )

    override def comboButton(mods: HtmlMod*): HtmlElement =
        L.button(
            cls(
                "absolute inset-y-0 right-0 flex items-center rounded-r-md px-2 focus:outline-none"
            ),
            tpe("button"),
            comboboxIcon(svg.cls("h-5 w-5 text-gray-400")),
            mods
        )

    override def inputFieldContainer(
        inError: Signal[Boolean],
        input: HtmlElement,
        mods: HtmlMod*
    ): HtmlElement =
        div(
            cls("relative"),
            input,
            inError.childWhenTrue(
                div(
                    cls("pointer-events-none absolute inset-y-0 right-0 pr-3 flex items-center"),
                    errorIcon(svg.cls("h-5 w-5 text-red-800"))
                )
            ),
            mods
        )

    override def inputField(
        id: String,
        fieldName: String,
        inError: Signal[Boolean],
        inputMods: HtmlMod*
    ): L.Input =
        input(
            cls(
                "text-red-900 ring-red-300 placeholder:text-red-300 focus:ring-red-500"
            ) <-- inError,
            cls(
                "text-blue-400 ring-gray-300 placeholder:text-gray-400 focus:ring-blue-400"
            ) <-- inError.not,
            cls(
                "block w-full rounded-md border-0 py-1.5 shadow-sm ring-1 ring-inset focus:ring-2 focus:ring-inset sm:text-sm sm:leading-6"
            ),
            idAttr(id),
            nameAttr(fieldName),
            inputMods
        )

    override def textAreaField(
        id: String,
        fieldName: String,
        inError: Signal[Boolean],
        inputMods: HtmlMod*
    ): L.TextArea =
        textArea(
            cls(
                "text-red-900 ring-red-300 placeholder:text-red-300 focus:ring-red-500"
            ) <-- inError,
            cls(
                "text-blue-400 ring-gray-300 placeholder:text-gray-400 focus:ring-blue-400"
            ) <-- inError.not,
            cls(
                "block w-full rounded-md border-0 py-1.5 shadow-sm ring-1 ring-inset focus:ring-2 focus:ring-inset sm:text-sm sm:leading-6"
            ),
            idAttr(id),
            nameAttr(fieldName),
            inputMods
        )
end SimpleFormComponents
