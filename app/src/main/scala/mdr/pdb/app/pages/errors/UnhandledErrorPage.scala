package mdr.pdb.app.pages.errors

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import mdr.pdb.app.Page
import mdr.pdb.app.Action

object UnhandledErrorPage:

  case class ViewModel(
      homePage: Page,
      errorName: Option[String],
      errorMessage: Option[String]
  )

  def apply(m: ViewModel, actionBus: Observer[Action])(using
      router: Router[Page]
  ): HtmlElement =
    ErrorPage(
      ErrorPage.ViewModel(
        m.homePage,
        "Unexpected error occurred",
        m.errorName.getOrElse(
          "Uh oh!"
        ), // TODO: translations, better text than uh oh
        m.errorMessage.getOrElse(
          "This wasn't supposed to happen! Please try again."
        )
      ),
      actionBus
    )
