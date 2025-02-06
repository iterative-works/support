package works.iterative.ui.components

trait AppShell[T]:
    def wrap(pageTitle: String, content: T): T
