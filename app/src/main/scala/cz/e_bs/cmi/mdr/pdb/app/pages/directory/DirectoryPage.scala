package cz.e_bs.cmi.mdr.pdb.app.pages.directory

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.Page
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.UserInfo

import components._

object DirectoryPage:

  type ViewModel = List[UserInfo]

  def render($m: Signal[ViewModel])(using router: Router[Page]): HtmlElement =
    val byLetter = for {
      d <- $m
    } yield for {
      (letter, users) <- d.groupBy(_.surname.head).to(List).sortBy(_._1)
    } yield (letter.toString, users.sortBy(_.surname))

    div(
      cls := "max-w-7xl mx-auto",
      //cls := "xl:order-first xl:flex xl:flex-col flex-shrink-0 w-96 border-r border-gray-200",
      SearchForm.render,
      Directory.render(byLetter)
    )
