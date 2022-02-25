package cz.e_bs.cmi.mdr.pdb.app.pages.directory.components

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.UserInfo

object Directory:

  object Header:
    type ViewModel = String
    def apply($m: Signal[ViewModel]): HtmlElement =
      div(
        cls := "z-10 sticky top-0 border-t border-b border-gray-200 bg-gray-50 px-6 py-1 text-sm font-medium text-gray-500",
        h3(child.text <-- $m)
      )

  object UserList:
    type ViewModel = List[UserRow.ViewModel]
    def apply($m: Signal[ViewModel]): HtmlElement =
      ul(
        role := "list",
        cls := "relative z-0 divide-y divide-gray-200",
        children <-- $m.split(_.osobniCislo)((_, _, s) => UserRow.render(s))
      )

  type ViewModel = List[(String, List[UserRow.ViewModel])]
  def apply($m: Signal[ViewModel]): HtmlElement =
    nav(
      cls := "flex-1 min-h-0 overflow-y-auto",
      aria.label := "Directory",
      children <-- $m.split(_._1)((_, _, s) =>
        div(
          cls := "relative",
          Header(s.map(_._1)),
          UserList(s.map(_._2))
        )
      )
    )
