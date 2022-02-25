package cz.e_bs.cmi.mdr.pdb.app.pages.directory

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.UserInfo

import components._

object DirectoryPage:

  type ViewModel = List[UserRow.ViewModel]

  def apply($m: Signal[ViewModel]): HtmlElement =
    val (actionsStream, actionObserver) =
      EventStream.withObserver[SearchForm.Action]
    val $filter = actionsStream
      .collect { case ev: SearchForm.FilterAction =>
        ev
      }
      .startWith(SearchForm.NoFilter)
    val byLetter = for {
      d <- $m
      f <- $filter
    } yield for {
      (letter, users) <- d
        .filter { user =>
          f match
            case SearchForm.NoFilter  => true
            case SearchForm.Filter(t) => user.search.contains(t)
        }
        .groupBy(_.prijmeni.head)
        .to(List)
        .sortBy(_._1)
    } yield (letter.toString, users.sortBy(_.prijmeni))

    div(
      cls := "h-full max-w-7xl mx-auto order-first flex flex-col flex-shrink-0",
      SearchForm(actionObserver),
      Directory(byLetter)
    )
