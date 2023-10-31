package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L.*

trait AppShellComponentsModule:
  object shell:
    object stackedLight extends StackedAppShell:
      override def navCls = cls("border-b border-gray-200 bg-white")
      override def headerCls = emptyMod
      override def pageWrapper(content: HtmlMod*): HtmlMod =
        div(cls("py-10"), content)

    object stackedBranded extends StackedAppShell:
      override def navCls = cls("bg-indigo-600")
      override def headerCls = cls("bg-white shadown-sm")
      override def pageWrapper(content: HtmlMod*): HtmlMod = content

trait StackedAppShell:
  protected def navCls: HtmlMod
  protected def headerCls: HtmlMod
  protected def pageWrapper(content: HtmlMod*): HtmlMod

  def apply(pageTitle: HtmlMod)(navbarItems: HtmlMod*)(
      content: HtmlMod*
  ): HtmlElement =
    div(
      cls("min-h-full"),
      navTag(
        navCls,
        div(
          cls("mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"),
          div(cls("flex h-16 items-center justify-between"), navbarItems)
        )
      ),
      pageWrapper(
        headerTag(
          headerCls,
          div(
            cls("mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"),
            h1(
              cls(
                "text-3xl font-bold leading-tight tracking-tight text-gray-900"
              ),
              pageTitle
            )
          )
        ),
        mainTag(
          div(cls("mx-auto max-w-7xl sm:px-6 lg:px-8"), content)
        )
      )
    )
