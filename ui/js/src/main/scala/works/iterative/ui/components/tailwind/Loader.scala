package works.iterative.ui.components.tailwind

import com.raquo.laminar.api.L.*

// TODO: proper loader
def Loading =
  div(
    cls := "bg-gray-50 overflow-hidden rounded-lg",
    div(
      cls := "px-4 py-5 sm:p-6",
      "Loading..."
    )
  )
