package works.iterative.ui.components

trait ErrorPageComponents[T] extends Components[T]:
    def errorPage(ex: Throwable): T
    def errorPage(message: String): T
end ErrorPageComponents
