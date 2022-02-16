package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}

// TODO: proper loader
def Loading =
  div(
    cls := "bg-gray-50 overflow-hidden rounded-lg",
    div(
      cls := "px-4 py-5 sm:p-6",
      "Loading..."
    )
  )
