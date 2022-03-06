package mdr.pdb.app.pages.directory

import com.raquo.laminar.api.L.{*, given}
import mdr.pdb.users.query.UserInfo

import components._

object DirectoryPage:

  type ViewModel = List[UserRow.ViewModel]

  def apply($m: Signal[ViewModel]): HtmlElement =
    val (actionsStream, actionObserver) =
      EventStream.withObserver[SearchForm.Action]
    val $filter = actionsStream
      .collect {
        case SearchForm.ClearFilter  => None
        case SearchForm.SetFilter(t) => Some(t)
      }
      .startWith(None)
    val byLetter = for {
      d <- $m
      f <- $filter
    } yield for {
      (letter, users) <- d
        .filter { user =>
          f match
            case None    => true
            case Some(t) => user.search.contains(t)
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
