package works.iterative.scenarios

import zio.http.template.*

object ScenarioView:
    val logo = div(
        classAttr("flex h-16 shrink-0 items-center"),
        img(
            classAttr("h-8 w-auto"),
            srcAttr := "https://tailwindui.com/img/logos/mark.svg?color=indigo&shade=600",
            altAttr := "Your Company"
        )
    )

    object icons:

        val home = Dom.raw(
            """<svg class="h-6 w-6 shrink-0 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
                    </svg>"""
        )

        val team = Dom.raw("""<svg class="h-6 w-6 shrink-0 text-gray-400 group-hover:text-indigo-600" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
        </svg>""")

        val projects = Dom.raw("""<svg class="h-6 w-6 shrink-0 text-gray-400 group-hover:text-indigo-600" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z" />
        </svg>""")

        val calendar = Dom.raw("""<svg class="h-6 w-6 shrink-0 text-gray-400 group-hover:text-indigo-600" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
        </svg>""")

        val documents = Dom.raw("""<svg class="h-6 w-6 shrink-0 text-gray-400 group-hover:text-indigo-600" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 17.25v3.375c0 .621-.504 1.125-1.125 1.125h-9.75a1.125 1.125 0 01-1.125-1.125V7.875c0-.621.504-1.125 1.125-1.125H6.75a9.06 9.06 0 011.5.124m7.5 10.376h3.375c.621 0 1.125-.504 1.125-1.125V11.25c0-4.46-3.243-8.161-7.5-8.876a9.06 9.06 0 00-1.5-.124H9.375c-.621 0-1.125.504-1.125 1.125v3.5m7.5 10.375H9.375a1.125 1.125 0 01-1.125-1.125v-9.25m12 6.625v-1.875a3.375 3.375 0 00-3.375-3.375h-1.5a1.125 1.125 0 01-1.125-1.125v-1.5a3.375 3.375 0 00-3.375-3.375H9.75" />
        </svg>""")

        val reports =
            Dom.raw("""<svg class="h-6 w-6 shrink-0 text-gray-400 group-hover:text-indigo-600" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 6a7.5 7.5 0 107.5 7.5h-7.5V6z" />
        <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 10.5H21A7.5 7.5 0 0013.5 3v7.5z" />
        </svg>""")
    end icons

    def letterIcon(letter: Char) = span(
        classAttr(
            "flex h-6 w-6 shrink-0 items-center justify-center rounded-lg border border-gray-200 bg-white text-[0.625rem] font-medium text-gray-400 group-hover:border-indigo-600 group-hover:text-indigo-600"
        ),
        letter.toString
    )

    def item(text: String, icon: Dom, current: Boolean) =
        // Current: "bg-gray-50 text-indigo-600", Default: "text-gray-700 hover:text-indigo-600 hover:bg-gray-50"
        val classes = if current then "bg-gray-50 text-indigo-600"
        else "text-gray-700 hover:text-indigo-600 hover:bg-gray-50"

        li(
            a(
                href := "#",
                classAttr(
                    List(
                        "group flex gap-x-3 rounded-md p-2 text-sm font-semibold leading-6",
                        classes
                    ).mkString(" ")
                ),
                icon,
                text
            )
        )
    end item

    def textItem(scenarioId: String, text: String, current: Boolean) =
        // Current: "bg-gray-50 text-indigo-600", Default: "text-gray-700 hover:text-indigo-600 hover:bg-gray-50"
        val classes = if current then "bg-gray-50 text-indigo-600"
        else "text-gray-700 hover:text-indigo-600 hover:bg-gray-50"
        li(
            a(
                href := s"/scenarios/$scenarioId",
                classAttr(
                    List(
                        "group flex gap-x-3 rounded-md p-2 text-sm font-semibold leading-6",
                        classes
                    ).mkString(" ")
                ),
                span(
                    classAttr(
                        "flex h-6 w-6 shrink-0 items-center justify-center rounded-lg border border-gray-200 bg-white text-[0.625rem] font-medium text-gray-400 group-hover:border-indigo-600 group-hover:text-indigo-600"
                    ),
                    text(0).toString
                ),
                span(classAttr("truncate"), text)
            )
        )
    end textItem

    def layout(scenarios: Seq[Scenario], current: Scenario) =
        html(
            classAttr("h-full bg-gray-50"),
            head(
                meta(charsetAttr := "UTF-8"),
                meta(
                    nameAttr := "viewport",
                    contentAttr := "width=device-width, initial-scale=1.0"
                ),
                title("Scenarios"),
                // Tailwind CSS
                script(srcAttr := "https://cdn.tailwindcss.com"),
                // htmx
                script(srcAttr := "https://unpkg.com/htmx.org@2.0.2")
            ),
            body(
                classAttr("h-full"),
                // https://tailwindui.com/components/application-ui/application-shells/sidebar#component-ad8537ba263d1aafea87a912adf16803
                div(
                    classAttr("h-full"),
                    // Static sidebar for desktop
                    div(
                        classAttr(
                            "fixed inset-y-0 z-50 flex w-72 flex-col"
                        ),
                        // Sidebar component, swap this element with another sidebar if you like
                        div(
                            classAttr(
                                "flex grow flex-col gap-y-5 overflow-y-auto border-r border-gray-200 bg-white px-6"
                            ),
                            /* logo
                            logo,
                             */
                            nav(
                                classAttr("flex flex-1 flex-col"),
                                ul(
                                    Dom.attr("role", "list"),
                                    classAttr("flex flex-1 flex-col gap-y-7"),
                                    /* first nav
                                    li(
                                        ul(
                                            Dom.attr("role", "list"),
                                            classAttr("-mx-2 space-y-1"),
                                            item("Dashboard", icons.home, true),
                                            item("Team", icons.team, false),
                                            item("Projects", icons.projects, false),
                                            item("Calendar", icons.calendar, false),
                                            item("Documents", icons.documents, false),
                                            item("Reports", icons.reports, false)
                                        )
                                    ),
                                     */
                                    li(
                                        div(
                                            classAttr(
                                                "text-xs font-semibold leading-6 text-gray-400"
                                            ),
                                            "Scenarios"
                                        ),
                                        ul(
                                            Dom.attr("role", "list"),
                                            classAttr("-mx-2 mt-2 space-y-1"),
                                            scenarios.map: scenario =>
                                                textItem(
                                                    scenario.id,
                                                    scenario.label,
                                                    current.id.contains(scenario.id)
                                                )
                                        )
                                    )
                                    /* Profile
                                    li(
                                        classAttr("-mx-6 mt-auto"),
                                        a(
                                            href := "#",
                                            classAttr(
                                                "flex items-center gap-x-4 px-6 py-3 text-sm font-semibold leading-6 text-gray-900 hover:bg-gray-50"
                                            ),
                                            img(
                                                classAttr("h-8 w-8 rounded-full bg-gray-50"),
                                                srcAttr := "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80",
                                                altAttr := ""
                                            ),
                                            span(classAttr("sr-only"), "Your profile"),
                                            span(Dom.attr("aria-hidden", "true"), "Tom Cook")
                                        )
                                    )
                                     */
                                )
                            )
                        )
                    ),
                    main(
                        classAttr("pl-72 h-full"),
                        div(
                            classAttr("px-8 h-full"),
                            // Your content
                            current.content
                        )
                    )
                )
            )
        )
end ScenarioView
