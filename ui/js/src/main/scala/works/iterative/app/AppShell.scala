package works.iterative.app

import com.raquo.laminar.api.L.*

trait AppShell:
    val element: HtmlElement

trait AppShellFactory:
    def make(appPage: Signal[HtmlElement]): AppShell
