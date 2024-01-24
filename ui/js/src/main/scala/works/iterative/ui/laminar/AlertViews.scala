package works.iterative.ui.laminar

import com.raquo.laminar.api.L.*

trait AlertViews:
    import AlertViews.Level
    def alert(level: Level, message: => HtmlElement, onClose: () => Unit): HtmlElement

object AlertViews:
    enum Level:
        case Success
        case Info
        case Error
        case Warning
        case Debug
    end Level
end AlertViews
