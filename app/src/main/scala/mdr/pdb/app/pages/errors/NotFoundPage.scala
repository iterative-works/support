package mdr.pdb.app.pages.errors

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import mdr.pdb.app.Page
import mdr.pdb.app.Action

object NotFoundPage:
  def apply(homePage: Page, url: String, actionBus: Observer[Action])(using
      router: Router[Page]
  ): HtmlElement =
    ErrorPage(
      ErrorPage.ViewModel(
        homePage,
        "404 error",
        "Page not found.",
        s"Sorry, but page $url doesn't exist."
      ),
      actionBus
    )
