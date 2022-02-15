package cz.e_bs.cmi.mdr.pdb.app.pages.errors

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.Page

def UnhandledErrorPage(
    homePage: Page,
    errorName: Option[String],
    errorMessage: Option[String]
)(using router: Router[Page]): HtmlElement =
  ErrorPage(
    homePage,
    "Unexpected error occurred",
    errorName.getOrElse("Uh oh!"), // TODO: translations, better text than uh oh
    errorMessage.getOrElse("This wasn't supposed to happen! Please try again.")
  ).render
