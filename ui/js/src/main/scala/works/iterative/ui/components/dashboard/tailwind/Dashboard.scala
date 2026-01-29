package works.iterative.ui.components.dashboard.tailwind

import com.raquo.laminar.api.L.*

object Dashboard:
    def number(n: Int, color: String, t: String) =
        span(cls(color), title(t), s"$n")
